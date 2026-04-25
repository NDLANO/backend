/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NodeRepository extends TaxonomyRepository<Node> {
    @Query("SELECT DISTINCT n FROM Node n WHERE n.context = :isContext")
    List<Node> findAllByContextIncludingCachedUrlsAndTranslations(boolean isContext);

    Optional<Node> findFirstByPublicId(URI publicId);

    @Query(value = "SELECT n.id FROM Node n ORDER BY n.id", countQuery = "SELECT count(*) from Node")
    Page<Integer> findIdsPaginated(Pageable pageable);

    @Query("""
            SELECT DISTINCT n FROM Node n
            LEFT JOIN FETCH n.resourceResourceTypes rrt
            LEFT JOIN FETCH rrt.resourceType rt
            LEFT JOIN FETCH n.parentConnections pc
            WHERE n.id in :ids
            """)
    List<Node> findByIds(Collection<Integer> ids);

    @Query("""
            SELECT n
            FROM Node n
            LEFT JOIN FETCH n.parentConnections pc
            WHERE n.qualityEvaluation IS NOT NULL
            """)
    Stream<Node> findNodesWithQualityEvaluation();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Node n
            SET n.childQualityEvaluationSum = 0,
                n.childQualityEvaluationCount = 0
            """)
    void wipeQualityEvaluationAverages();

    String APPLY_QUALITY_EVALUATION_GRADE_DELTA_TO_ANCESTORS_QUERY = """
            WITH RECURSIVE ancestors(id, path) AS (
                SELECT n0.id, ARRAY[n0.id] FROM node n0 WHERE n0.id IN (:startIds)
                UNION ALL
                SELECT nc.parent_id, a.path || nc.parent_id FROM node_connection nc
                JOIN ancestors a ON nc.child_id = a.id
                WHERE nc.connection_type <> 'LINK'
                  AND nc.parent_id IS NOT NULL
                  AND NOT nc.parent_id = ANY(a.path)
            ), ancestor_deltas AS (
                SELECT id,
                       CAST(COUNT(*) AS integer) AS occurrences,
                       CAST(COUNT(*) AS integer) * :countDelta AS count_delta,
                       CAST(COUNT(*) AS integer) * :sumDelta AS sum_delta
                FROM ancestors
                GROUP BY id
            )
            UPDATE node n SET
                child_quality_evaluation_count = CASE
                    WHEN n.child_quality_evaluation_count = 0
                         OR n.child_quality_evaluation_sum = 0
                        THEN CASE WHEN CAST(:newGrade AS integer) IS NOT NULL THEN delta.occurrences
                                  ELSE n.child_quality_evaluation_count END
                    WHEN CAST(:oldGrade AS integer) IS NOT NULL AND CAST(:newGrade AS integer) IS NULL
                         AND (n.child_quality_evaluation_count + delta.count_delta <= 0
                              OR n.child_quality_evaluation_sum + delta.sum_delta <= 0)
                        THEN 0
                    ELSE n.child_quality_evaluation_count + delta.count_delta
                END,
                child_quality_evaluation_sum = CASE
                    WHEN n.child_quality_evaluation_count = 0
                         OR n.child_quality_evaluation_sum = 0
                        THEN COALESCE(CAST(:newGrade AS integer) * delta.occurrences, n.child_quality_evaluation_sum)
                    WHEN CAST(:oldGrade AS integer) IS NOT NULL AND CAST(:newGrade AS integer) IS NULL
                         AND (n.child_quality_evaluation_count + delta.count_delta <= 0
                              OR n.child_quality_evaluation_sum + delta.sum_delta <= 0)
                        THEN 0
                    ELSE n.child_quality_evaluation_sum + delta.sum_delta
                END
            FROM ancestor_deltas delta
            WHERE n.id = delta.id
            """;

    String APPLY_QUALITY_EVALUATION_AVERAGE_DELTA_TO_ANCESTORS_QUERY = """
            WITH RECURSIVE ancestors(id, path) AS (
                SELECT n0.id, ARRAY[n0.id] FROM node n0 WHERE n0.id IN (:startIds)
                UNION ALL
                SELECT nc.parent_id, a.path || nc.parent_id FROM node_connection nc
                JOIN ancestors a ON nc.child_id = a.id
                WHERE nc.connection_type <> 'LINK'
                  AND nc.parent_id IS NOT NULL
                  AND NOT nc.parent_id = ANY(a.path)
            ), ancestor_deltas AS (
                SELECT id,
                       CAST(COUNT(*) AS integer) * :countDelta AS count_delta,
                       CAST(COUNT(*) AS integer) * :sumDelta AS sum_delta
                FROM ancestors
                GROUP BY id
            )
            UPDATE node n SET
                child_quality_evaluation_count = CASE
                    WHEN n.child_quality_evaluation_count = 0
                         OR n.child_quality_evaluation_sum = 0
                        THEN CASE WHEN :countDelta > 0 THEN delta.count_delta
                                  ELSE n.child_quality_evaluation_count END
                    WHEN n.child_quality_evaluation_count + delta.count_delta <= 0
                         OR n.child_quality_evaluation_sum + delta.sum_delta <= 0
                        THEN 0
                    ELSE n.child_quality_evaluation_count + delta.count_delta
                END,
                child_quality_evaluation_sum = CASE
                    WHEN n.child_quality_evaluation_count = 0
                         OR n.child_quality_evaluation_sum = 0
                        THEN CASE WHEN :countDelta > 0 THEN delta.sum_delta
                                  ELSE n.child_quality_evaluation_sum END
                    WHEN n.child_quality_evaluation_count + delta.count_delta <= 0
                         OR n.child_quality_evaluation_sum + delta.sum_delta <= 0
                        THEN 0
                    ELSE n.child_quality_evaluation_sum + delta.sum_delta
                END
            FROM ancestor_deltas delta
            WHERE n.id = delta.id
            """;

    /**
     * Applies a (sumDelta, countDelta) to child_quality_evaluation_sum/count for every non-LINK
     * ancestor (inclusive of start ids) in one atomic UPDATE. Shared ancestors are aggregated by
     * the recursive CTE; deltas are occurrence-weighted (one occurrence per branch path).
     *
     * Branches mirror Node.updateChildQualityEvaluationAverage:
     *   - self-heal: count = 0 or sum = 0 + newGrade present → (occurrences, newGrade * occurrences).
     *     Caveat: with corrupt (count > 0, sum = 0) + both grades, this overwrites instead of
     *     applying the diff (SQL recovers once; the old recursion did so per visit);
     *   - remove-to-zero: oldGrade present, newGrade null, delta would push either ≤ 0 → zero both;
     *   - else: apply deltas.
     *
     * Concurrent calls accumulate via EPQ: SET expressions reference only n.* columns, so on
     * lock-wait unblock under READ COMMITTED, Postgres re-evaluates them against the freshly
     * committed row instead of overwriting. Lock order within the statement is plan-determined;
     * overlapping concurrent calls rely on Postgres' deadlock detector — on deadlock one
     * transaction aborts with SQLSTATE 40P01. The {@code @Retryable} annotation on the affected
     * REST controllers (configured by {@link no.ndla.taxonomy.config.RetryConfig}) replays the
     * outer transaction up to a fixed number of attempts so the rollback is recoverable.
     *
     * The recursive CTE is evaluated once at statement start; concurrent commits to node_connection
     * between CTE evaluation and row-lock acquisition are not reflected in the ancestor set. Use
     * updateEntireAverageTreeForNode to repair if a connect/disconnect raced with this update.
     *
     * :oldGrade and :newGrade keep their CASTs — a NULL-bound Integer used only in a NULL check
     * otherwise breaks Postgres type inference.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = APPLY_QUALITY_EVALUATION_GRADE_DELTA_TO_ANCESTORS_QUERY, nativeQuery = true)
    void applyQualityEvaluationDeltaToAncestors(
            Collection<Integer> startIds, Integer oldGrade, Integer newGrade, int sumDelta, int countDelta);

    /**
     * Variant of {@link #applyQualityEvaluationDeltaToAncestors} that leaves the persistence
     * context intact. Use when the caller still needs entities loaded earlier in this transaction
     * (e.g. the freshly refreshed child or parent in a connect/disconnect flow).
     */
    @Modifying(flushAutomatically = true)
    @Query(value = APPLY_QUALITY_EVALUATION_GRADE_DELTA_TO_ANCESTORS_QUERY, nativeQuery = true)
    void applyQualityEvaluationDeltaToAncestorsWithoutClearing(
            Collection<Integer> startIds, Integer oldGrade, Integer newGrade, int sumDelta, int countDelta);

    /**
     * Adds (or removes, via negative deltas) a child's already-calculated subtree average to every
     * non-LINK ancestor. Used on connect/disconnect of topic-style children.
     *
     * Differs from the grade-delta query in how the add/remove signal is encoded: branches gate on
     * the sign of :countDelta rather than nullable :newGrade, since the source here is an
     * already-aggregated subtree average rather than a single grade transition.
     *
     * Does NOT clear the persistence context — current callers keep child/parent in memory.
     * EPQ accumulation, CTE-snapshot, and deadlock characteristics match the grade-delta query.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = APPLY_QUALITY_EVALUATION_AVERAGE_DELTA_TO_ANCESTORS_QUERY, nativeQuery = true)
    void applyQualityEvaluationAverageDeltaToAncestors(Collection<Integer> startIds, int sumDelta, int countDelta);

    @Query("""
            SELECT n.id FROM Node n
            LEFT JOIN n.parentConnections pc
            WHERE ((:#{#nodeTypes == null} = true) OR n.nodeType in (:nodeTypes))
            AND ((:#{#publicIds == null} = true) OR n.publicId in (:publicIds))
            AND (:metadataFilterKey IS NULL OR jsonb_extract_path_text(n.customfields, cast(:metadataFilterKey as text)) IS NOT NULL)
            AND (:metadataFilterValue IS NULL OR cast(jsonb_path_query_array(n.customfields, '$.*') as text) like :metadataFilterValue)
            AND (:contentUri IS NULL OR n.contentUri = :contentUri)
            AND (:isContext IS NULL OR n.context = :isContext)
            AND (:isRoot IS NULL OR (pc IS NULL AND n.context = true))
            """)
    List<Integer> findIdsFiltered(
            Optional<List<NodeType>> nodeTypes,
            Optional<List<URI>> publicIds,
            Optional<String> metadataFilterKey,
            Optional<String> metadataFilterValue,
            Optional<URI> contentUri,
            Optional<Boolean> isRoot,
            Optional<Boolean> isContext);

    @Query(value = """
            SELECT n.id FROM Node n
            WHERE (:contextId IS NULL OR n.contextids @> jsonb_build_array(:contextId))
            """, nativeQuery = true)
    List<Integer> findIdsByContextId(Optional<String> contextId);

    @Query(
            value = "SELECT n.id FROM Node n where n.nodeType = :nodeType ORDER BY n.id",
            countQuery = "SELECT count(*) from Node n where n.nodeType = :nodeType")
    Page<Integer> findIdsByTypePaginated(Pageable pageable, NodeType nodeType);

    @Query("""
            SELECT DISTINCT n FROM Node n
            LEFT JOIN FETCH n.resourceResourceTypes rrt
            LEFT JOIN FETCH rrt.resourceType
            LEFT JOIN FETCH n.parentConnections pc
            LEFT JOIN FETCH n.childConnections cc
            WHERE n.nodeType = "PROGRAMME"
            AND n.context = true
            """)
    List<Node> findProgrammes();

    @Query("""
            SELECT DISTINCT n FROM Node n
            LEFT JOIN FETCH n.resourceResourceTypes rrt
            LEFT JOIN FETCH rrt.resourceType
            LEFT JOIN FETCH n.parentConnections pc
            LEFT JOIN FETCH n.childConnections cc
            WHERE n.nodeType = "SUBJECT"
            AND n.context = true
            """)
    List<Node> findRootSubjects();

    @Query("""
            SELECT DISTINCT n FROM Node n
            LEFT JOIN FETCH n.resourceResourceTypes rrt
            LEFT JOIN FETCH rrt.resourceType
            LEFT JOIN FETCH n.parentConnections pc
            WHERE n.contentUri = :contentUri
            """)
    List<Node> findByContentUri(Optional<URI> contentUri);
}
