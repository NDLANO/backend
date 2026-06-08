/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories

import java.net.URI
import no.ndla.taxonomy.domain.exceptions.NotFoundException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface TaxonomyRepository<T> : JpaRepository<T, Int>, JpaSpecificationExecutor<T> {
  fun findByPublicId(id: URI): T?

  fun getByPublicId(id: URI): T = findByPublicId(id) ?: throw NotFoundException("entity", id)

  fun deleteAllAndFlush() {
    deleteAll()
    flush()
  }
}
