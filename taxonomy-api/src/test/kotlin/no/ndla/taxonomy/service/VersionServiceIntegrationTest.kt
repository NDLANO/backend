/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import jakarta.transaction.Transactional
import no.ndla.taxonomy.domain.VersionType
import no.ndla.taxonomy.repositories.VersionRepository
import no.ndla.taxonomy.rest.v1.commands.VersionPost
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@Transactional
class VersionServiceIntegrationTest : AbstractIntegrationTest() {
  @Autowired private lateinit var versionRepository: VersionRepository
  @Autowired private lateinit var versionService: VersionService

  @BeforeEach
  fun clearAllRepos() {
    versionRepository.deleteAllAndFlush()
  }

  @Test
  fun can_create_new_version_with_own_schema() {
    val command = VersionPost(name = "Beta")
    val version = versionService.createNewVersion(null, command)
    assertEquals(VersionType.BETA, version.versionType)

    // Check that specified schema exists
    assertTrue(checkSchemaExists(versionService.schemaFromHash(version.hash)))
  }

  @Test
  fun can_publish_version() {
    val command = VersionPost(name = "Beta")
    val version = versionService.createNewVersion(null, command)
    versionService.publishBetaAndArchiveCurrent(version.publicId)

    val published = versionRepository.getByPublicId(version.publicId)
    assertEquals(VersionType.PUBLISHED, published.versionType)
    assertNotNull(published.published)
  }
}
