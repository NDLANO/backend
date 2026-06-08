/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories

import java.net.URI
import no.ndla.taxonomy.domain.ResourceType
import org.springframework.data.jpa.repository.Query

interface ResourceTypeRepository : TaxonomyRepository<ResourceType> {

  @Query("SELECT DISTINCT rt FROM ResourceType rt WHERE rt.publicId = :publicId")
  fun findFirstByPublicIdIncludingTranslations(publicId: URI): ResourceType?

  fun findAllByOrderByOrderAsc(): List<ResourceType>

  fun findByParentPublicId(parentPublicId: URI): List<ResourceType>

  @Query("SELECT COALESCE(MAX(rt.order), -1) + 1 FROM ResourceType rt") fun nextOrderValue(): Int
}
