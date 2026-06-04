/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories

import no.ndla.taxonomy.domain.UrlMapping
import org.springframework.data.repository.CrudRepository

interface UrlMappingRepository : CrudRepository<UrlMapping, String> {
  fun findAllByOldUrlLike(oldUrl: String): List<UrlMapping>

  fun findAllByOldUrl(oldUrl: String): List<UrlMapping>
}
