/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories

import java.net.URI
import no.ndla.taxonomy.domain.ResourceResourceType
import org.springframework.data.jpa.repository.Query

interface ResourceResourceTypeRepository : TaxonomyRepository<ResourceResourceType> {

  @Query(
      "SELECT rrt FROM ResourceResourceType rrt JOIN FETCH rrt.node r JOIN FETCH rrt.resourceType")
  fun findAllIncludingResourceAndResourceType(): List<ResourceResourceType>

  @Query(
      """
            SELECT rrt FROM ResourceResourceType rrt
            JOIN FETCH rrt.node
            JOIN FETCH rrt.resourceType rt
            LEFT JOIN FETCH rt.parent
            WHERE rrt.node.publicId = :parentNodeId""")
  fun resourceResourceTypeByParentId(parentNodeId: URI): List<ResourceResourceType>
}
