/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.repositories

import java.net.URI
import no.ndla.taxonomy.domain.Version
import no.ndla.taxonomy.domain.VersionType
import org.springframework.data.jpa.repository.Query

interface VersionRepository : TaxonomyRepository<Version> {
  @Query(
      value = "SELECT * from Version v where v.public_id = :#{#publicId.toString()}",
      nativeQuery = true)
  fun findFirstByPublicId(publicId: URI): Version?

  @Query(value = "SELECT * from Version v where v.hash = :hash", nativeQuery = true)
  fun findFirstByHash(hash: String): Version?

  @Query(
      value = "SELECT * from Version v where v.version_type = :#{#versionType.name()}",
      nativeQuery = true)
  fun findByVersionType(versionType: VersionType): List<Version>

  @Query(
      value = "SELECT * from Version v where v.version_type = :#{#versionType.name()}",
      nativeQuery = true)
  fun findFirstByVersionType(versionType: VersionType): Version?
}
