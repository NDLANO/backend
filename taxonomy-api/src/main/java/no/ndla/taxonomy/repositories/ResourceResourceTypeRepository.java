/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories;

import java.net.URI;
import java.util.List;
import no.ndla.taxonomy.domain.ResourceResourceType;
import org.springframework.data.jpa.repository.Query;

public interface ResourceResourceTypeRepository extends TaxonomyRepository<ResourceResourceType> {
    @Query("SELECT rrt FROM ResourceResourceType rrt JOIN FETCH rrt.node r JOIN FETCH rrt.resourceType")
    List<ResourceResourceType> findAllIncludingResourceAndResourceType();

    @Query("""
            SELECT rrt FROM ResourceResourceType rrt
            JOIN FETCH rrt.node
            JOIN FETCH rrt.resourceType rt
            LEFT JOIN FETCH rt.parent
            WHERE rrt.node.publicId = :parentNodeId""")
    List<ResourceResourceType> resourceResourceTypeByParentId(URI parentNodeId);
}
