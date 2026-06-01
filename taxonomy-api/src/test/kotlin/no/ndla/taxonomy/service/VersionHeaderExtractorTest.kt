/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import no.ndla.taxonomy.domain.Version
import no.ndla.taxonomy.domain.VersionType
import no.ndla.taxonomy.repositories.VersionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@Transactional
class VersionHeaderExtractorTest : AbstractIntegrationTest() {

  @Autowired private lateinit var versionRepository: VersionRepository
  @Autowired private lateinit var versionHeaderExtractor: VersionHeaderExtractor

  @Value("\${spring.datasource.hikari.schema:taxonomy_api}")
  private lateinit var defaultSchema: String

  @BeforeEach
  fun setUp() {
    versionRepository.deleteAllAndFlush()
  }

  private fun mockRequest(
      uri: String,
      header: String? = null,
      method: String = "GET",
  ) =
      mock(HttpServletRequest::class.java).also {
        `when`(it.requestURI).thenReturn(uri)
        `when`(it.getHeader(anyString())).thenReturn(header)
        `when`(it.method).thenReturn(method)
      }

  @Test
  fun no_headers_without_versions_returns_default_schema() {
    mockRequest("/v1/versions").let {
      assertEquals(defaultSchema, versionHeaderExtractor.getVersionSchemaFromHeader(it))
    }
  }

  @Test
  fun header_with_no_matching_version_returns_default_schema() {
    mockRequest("/v1/versions", "abcd", "POST").let {
      assertEquals(defaultSchema, versionHeaderExtractor.getVersionSchemaFromHeader(it))
    }
    mockRequest("/v1/versions", "abcd").let {
      assertEquals(defaultSchema, versionHeaderExtractor.getVersionSchemaFromHeader(it))
    }
  }

  @Test
  fun header_with_matching_version_returns_correct_schema() {
    val saved = versionRepository.save(Version().apply { versionType = VersionType.BETA })
    mockRequest("/v1/subjects", saved.hash, "POST").let {
      val actual = versionHeaderExtractor.getVersionSchemaFromHeader(it)
      assertEquals("${defaultSchema}_${saved.hash}", actual)
    }
    mockRequest("/v1/subjects", saved.hash).let {
      val actual = versionHeaderExtractor.getVersionSchemaFromHeader(it)
      assertEquals("${defaultSchema}_${saved.hash}", actual)
    }
  }

  @Test
  fun no_header_returns_published_schema_for_GET_and_default_for_POST() {
    val saved = versionRepository.save(Version().apply { versionType = VersionType.PUBLISHED })
    mockRequest("/v1/subjects").let {
      val actual = versionHeaderExtractor.getVersionSchemaFromHeader(it)
      assertEquals("${defaultSchema}_${saved.hash}", actual)
    }
    mockRequest("/v1/subjects", method = "POST").let {
      assertEquals(defaultSchema, versionHeaderExtractor.getVersionSchemaFromHeader(it))
    }
  }
}
