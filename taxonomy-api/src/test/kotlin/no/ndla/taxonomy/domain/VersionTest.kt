/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VersionTest {
  private lateinit var version: Version

  @BeforeEach
  fun setUp() {
    version = Version()
  }

  @Test
  fun has_public_id() {
    assertNotNull(version.publicId)
    assertTrue(version.publicId.toString().startsWith("urn:version:"))
  }

  @Test
  fun version_type_can_be_changed() {
    assertEquals(version.versionType, VersionType.BETA)
    version.versionType = VersionType.PUBLISHED
    assertEquals(version.versionType, VersionType.PUBLISHED)
  }
}
