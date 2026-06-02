/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain

import java.net.URI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UrlMappingTest {
  private lateinit var urlMapping: UrlMapping

  @BeforeEach
  fun setUp() {
    urlMapping =
        UrlMapping(
            "urn:test:0",
            URI.create("urn:test-public-0"),
            URI.create("urn:test-subject-id-0"),
        )
  }

  @Test
  fun setAndGetPublicId() {
    urlMapping.publicId = URI.create("urn:test-public-1")
    assertEquals("urn:test-public-1", urlMapping.publicId.toString())
  }

  @Test
  fun getAndSetSubjectId() {
    urlMapping.subjectId = URI.create("urn:test-subject-id-1")
    assertEquals("urn:test-subject-id-1", urlMapping.subjectId.toString())
  }
}
