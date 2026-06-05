/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import no.ndla.taxonomy.domain.Version
import no.ndla.taxonomy.domain.VersionType
import no.ndla.taxonomy.repositories.VersionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class VersionResolverTest {
  private val defaultSchema = "taxonomy_api"

  private fun resolver(repository: VersionRepository, ttlSeconds: Long = 60) =
      VersionResolver(repository, ttlSeconds, defaultSchema)

  @Test
  fun published_schema_is_cached_within_ttl() {
    val version = Version().apply { versionType = VersionType.PUBLISHED }
    val repository = mock(VersionRepository::class.java)
    `when`(repository.findFirstByVersionType(VersionType.PUBLISHED)).thenReturn(version)

    val resolver = resolver(repository)
    assertEquals("${defaultSchema}_${version.hash}", resolver.publishedVersionSchema())
    assertEquals("${defaultSchema}_${version.hash}", resolver.publishedVersionSchema())
    verify(repository, times(1)).findFirstByVersionType(VersionType.PUBLISHED)
  }

  @Test
  fun missing_published_version_is_cached_as_null() {
    val repository = mock(VersionRepository::class.java)
    `when`(repository.findFirstByVersionType(VersionType.PUBLISHED)).thenReturn(null)

    val resolver = resolver(repository)
    assertNull(resolver.publishedVersionSchema())
    assertNull(resolver.publishedVersionSchema())
    verify(repository, times(1)).findFirstByVersionType(VersionType.PUBLISHED)
  }

  @Test
  fun hash_schema_is_cached_within_ttl() {
    val version = Version()
    val repository = mock(VersionRepository::class.java)
    `when`(repository.findFirstByHash(version.hash)).thenReturn(version)

    val resolver = resolver(repository)
    assertEquals("${defaultSchema}_${version.hash}", resolver.versionSchemaForHash(version.hash))
    assertEquals("${defaultSchema}_${version.hash}", resolver.versionSchemaForHash(version.hash))
    verify(repository, times(1)).findFirstByHash(version.hash)
  }

  @Test
  fun invalidate_clears_cached_values() {
    val version = Version().apply { versionType = VersionType.PUBLISHED }
    val repository = mock(VersionRepository::class.java)
    `when`(repository.findFirstByVersionType(VersionType.PUBLISHED)).thenReturn(version)
    `when`(repository.findFirstByHash(version.hash)).thenReturn(version)

    val resolver = resolver(repository)
    resolver.publishedVersionSchema()
    resolver.versionSchemaForHash(version.hash)
    resolver.invalidate()
    resolver.publishedVersionSchema()
    resolver.versionSchemaForHash(version.hash)

    verify(repository, times(2)).findFirstByVersionType(VersionType.PUBLISHED)
    verify(repository, times(2)).findFirstByHash(version.hash)
  }

  @Test
  fun zero_ttl_disables_caching() {
    val version = Version().apply { versionType = VersionType.PUBLISHED }
    val repository = mock(VersionRepository::class.java)
    `when`(repository.findFirstByVersionType(VersionType.PUBLISHED)).thenReturn(version)

    val resolver = resolver(repository, ttlSeconds = 0)
    resolver.publishedVersionSchema()
    resolver.publishedVersionSchema()
    verify(repository, times(2)).findFirstByVersionType(VersionType.PUBLISHED)
  }
}
