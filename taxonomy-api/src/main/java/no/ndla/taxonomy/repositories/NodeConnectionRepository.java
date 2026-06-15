/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import java.net.URI;
import java.util.*;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface NodeConnectionRepository extends TaxonomyRepository<NodeConnection> {
    @Query("""
            SELECT DISTINCT nc
            FROM NodeConnection nc
            JOIN FETCH nc.parent p
            JOIN FETCH nc.child c
            WHERE nc.parent.publicId IN :nodeId
            AND ((:#{#nodeTypes == null} = true) OR c.nodeType in :nodeTypes)
            """)
    List<NodeConnection> findAllByNodeIdInIncludingTopicAndSubtopic(Set<URI> nodeId, List<NodeType> nodeTypes);

    @Query(value = """
            SELECT nc.* FROM node_connection nc
            JOIN node c ON nc.child_id = c.id
            JOIN node p ON nc.parent_id = p.id
            WHERE p.public_id IN :nodeIds
            AND (CAST(:resourceTypeIds AS text[]) IS NULL OR c.resource_type_ids && CAST(:resourceTypeIds AS text[]))
            AND (:relevance IS NULL OR nc.relevance = :relevance)
            AND c.node_type = 'RESOURCE'
            """, nativeQuery = true)
    List<NodeConnection> getResourceBy(Set<String> nodeIds, String[] resourceTypeIds, String relevance);

    @Query(
            "SELECT nc FROM NodeConnection nc JOIN FETCH nc.parent JOIN FETCH nc.child c WHERE c.nodeType = :childNodeType")
    List<NodeConnection> findAllByChildNodeType(NodeType childNodeType);

    @Query(
            value = "SELECT nc.id FROM NodeConnection nc ORDER BY nc.id",
            countQuery = "SELECT count(*) from NodeConnection")
    Page<Integer> findIdsPaginated(Pageable pageable);

    @Query(value = """
            SELECT nc
            FROM NodeConnection nc
            JOIN nc.child c
            WHERE c.nodeType = :nodeType
            ORDER BY nc.id
            """, countQuery = """
            SELECT count(nc)
            FROM NodeConnection nc
            JOIN nc.child c
            WHERE c.nodeType = :nodeType
            """)
    Page<NodeConnection> findIdsPaginatedByChildNodeType(Pageable pageable, NodeType nodeType);

    @Query("SELECT nc.id FROM NodeConnection nc")
    List<Integer> findAllIds();

    @Query("SELECT DISTINCT nc FROM NodeConnection nc JOIN FETCH nc.parent n JOIN FETCH nc.child c WHERE nc.id in :ids")
    List<NodeConnection> findByIds(Collection<Integer> ids);

    @Query(
            "SELECT DISTINCT nc FROM NodeConnection nc JOIN FETCH nc.child c JOIN FETCH nc.parent n WHERE n.publicId = :publicId")
    List<NodeConnection> findAllByParentPublicIdIncludingChildAndChildTranslations(URI publicId);

    @Query("""
            SELECT DISTINCT nc
            FROM NodeConnection nc
            JOIN FETCH nc.child c
            WHERE nc.child.publicId IN :childIds
            AND nc.parent.publicId IN :parentIds
            """)
    List<NodeConnection> doFindChildConnections(Collection<URI> childIds, Collection<URI> parentIds);

    default List<NodeConnection> findChildConnections(Collection<URI> childIds, Collection<URI> parentIds) {
        if (childIds.isEmpty()) {
            return List.of();
        }

        return doFindChildConnections(childIds, parentIds);
    }

    Optional<NodeConnection> findFirstByPublicId(URI publicId);

    @Query("""
            SELECT DISTINCT nc
            FROM NodeConnection nc
            JOIN FETCH nc.parent n
            JOIN FETCH nc.child c
            WHERE nc.parent.id = :parentId
            AND nc.child.id = :childId
            """)
    NodeConnection findByParentIdAndChildId(Integer parentId, Integer childId);
}
