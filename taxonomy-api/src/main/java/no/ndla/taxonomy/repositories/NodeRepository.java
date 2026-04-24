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
