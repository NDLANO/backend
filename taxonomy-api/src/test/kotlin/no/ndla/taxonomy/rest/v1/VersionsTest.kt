/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import java.net.URI
import no.ndla.taxonomy.TestUtils.assertAllTrue
import no.ndla.taxonomy.TestUtils.getId
import no.ndla.taxonomy.domain.VersionType
import no.ndla.taxonomy.rest.v1.commands.VersionPost
import no.ndla.taxonomy.rest.v1.commands.VersionPut
import no.ndla.taxonomy.service.dtos.VersionDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class VersionsTest : RestTest() {

  @BeforeEach
  fun cleanDatabase() {
    versionRepository.deleteAllAndFlush()
  }

  @Test
  @Throws(Exception::class)
  fun can_get_all_versions() {
    builder.version()
    builder.version()

    val response = testUtils.getResource("/v1/versions")
    val versions = testUtils.getObject(Array<VersionDTO>::class.java, response)
    assertEquals(2, versions.size)
  }

  @Test
  fun can_get_specified_version() {
    val versionId = URI.create("urn:version:1")
    builder.version { v -> v.publicId(versionId) }
    testUtils.getResource("/v1/versions/${versionId}").let { response ->
      val version = testUtils.getObject(VersionDTO::class.java, response)
      assertEquals(versionId, version.id)
      assertNotNull(version.hash)
      assertNull(version.published)
      assertNull(version.archived)
    }
    testUtils.getResource("/v1/versions/urn:version:2", status().is4xxClientError()).let { response
      ->
      assertEquals(404, response.status)
      assertEquals("{\"error\":\"Version not found\"}", response.contentAsString)
    }
  }

  @Test
  fun can_get_versions_from_hash() {
    val version = builder.version()
    testUtils.getResource("/v1/versions?hash=${version.hash}").let { response ->
      val versions = testUtils.getObject(Array<VersionDTO>::class.java, response)
      assertEquals(1, versions.size)
      assertEquals(version.hash, versions.first().hash)
    }
    testUtils.getResource("/v1/versions?hash=random", status().is4xxClientError()).let { response ->
      assertEquals(404, response.status)
      assertEquals("{\"error\":\"Version not found\"}", response.contentAsString)
    }
  }

  @Test
  fun can_get_versions_of_type() {
    val version = builder.version() // BETA
    val response = testUtils.getResource("/v1/versions?type=BETA")
    val versions = testUtils.getObject(Array<VersionDTO>::class.java, response)
    assertEquals(1, versions.size)
    assertAllTrue(versions) { v -> v.versionType == VersionType.BETA }
    assertAllTrue(versions) { v -> v.id == version.publicId }

    val version2 = builder.version { v -> v.type(VersionType.PUBLISHED) }
    val response2 = testUtils.getResource("/v1/versions?type=PUBLISHED")
    val versions2 = testUtils.getObject(Array<VersionDTO>::class.java, response2)
    assertEquals(1, versions2.size)
    assertAllTrue(versions2) { v -> v.versionType == VersionType.PUBLISHED }
    assertAllTrue(versions2) { v -> v.id == version2.publicId }
  }

  @Test
  fun can_create_version() {
    val createVersionCommand = VersionPost(id = URI.create("urn:version:1"), name = "Beta")

    val response = testUtils.createResource("/v1/versions", createVersionCommand)
    val id = getId(response)

    val version = versionRepository.getByPublicId(id)
    assertEquals(createVersionCommand.id, version.publicId)
    assertEquals(VersionType.BETA, version.versionType)
    assertEquals("Beta", version.name)
  }

  @Test
  fun can_create_version_based_on_existing() {
    val published = builder.version { v -> v.type(VersionType.PUBLISHED) }
    val createVersionCommand = VersionPost(id = URI.create("urn:version:1"), name = "Beta")
    val response =
        testUtils.createResource(
            "/v1/versions?sourceId=${published.publicId}",
            createVersionCommand,
        )
    val id = getId(response)
    val version = versionRepository.getByPublicId(id)
    assertEquals(createVersionCommand.id, version.publicId)
    assertEquals(VersionType.BETA, version.versionType)
    assertEquals("Beta", version.name)
  }

  @Test
  fun can_delete_version() {
    val version = builder.version()
    testUtils.deleteResource("/v1/versions/${version.publicId}")
    try {
      versionRepository.getByPublicId(version.publicId)
      fail("Failed to delete version")
    } catch (e: Exception) {
      // All OK
    }
  }

  @Test
  fun cannot_delete_locked_version() {
    val locked = builder.version { v -> v.locked(true) }
    val response =
        testUtils.deleteResource("/v1/versions/${locked.publicId}", status().is4xxClientError())
    assertEquals(400, response.status)
    assertEquals("{\"error\":\"Cannot delete locked version\"}", response.contentAsString)
  }

  @Test
  fun can_update_version() {
    val version = builder.version()
    val newUri = URI.create("urn:version:1")
    val updateVersionCommand = VersionPut(id = newUri, name = "New name", locked = true)

    testUtils.updateResource("/v1/versions/${version.publicId}", updateVersionCommand)

    val updated = versionRepository.getByPublicId(newUri)

    assertEquals(updateVersionCommand.id, updated.publicId)
    assertEquals(VersionType.BETA, updated.versionType)
    assertEquals("New name", updated.name)
    assertTrue(updated.isLocked)
    assertEquals(version.hash, updated.hash) // Not changed during update
  }

  @Test
  fun can_publish_version() {
    val versionUri = URI.create("urn:version:beta")
    val createVersionCommand = VersionPost(id = versionUri, name = "Beta")
    testUtils.createResource("/v1/versions", createVersionCommand)
    testUtils.updateResource("/v1/versions/${versionUri}/publish", null)

    val updated = versionRepository.getByPublicId(versionUri)
    assertEquals(VersionType.PUBLISHED, updated.versionType)
    assertTrue(updated.isLocked)
    assertNotNull(updated.published)
  }

  @Test
  fun cannot_publish_published_or_archived_version() {
    val version = builder.version { v -> v.type(VersionType.PUBLISHED) }
    val response =
        testUtils.updateResource(
            "/v1/versions/${version.publicId}/publish",
            null,
            status().is4xxClientError(),
        )
    assertEquals(400, response.status)
    assertEquals("{\"error\":\"Version has wrong type\"}", response.contentAsString)

    val version2 = builder.version { v -> v.type(VersionType.ARCHIVED) }
    val response2 =
        testUtils.updateResource(
            "/v1/versions/${version2.publicId}/publish",
            null,
            status().is4xxClientError(),
        )
    assertEquals(400, response2.status)
    assertEquals("{\"error\":\"Version has wrong type\"}", response2.contentAsString)
  }

  @Test
  fun publishing_version_unpublishes_current() {
    val published = builder.version { v -> v.type(VersionType.PUBLISHED) }
    val beta = builder.version()
    testUtils.updateResource("/v1/versions/${beta.publicId}/publish", null)

    val updated = versionRepository.getByPublicId(beta.publicId)
    assertEquals(VersionType.PUBLISHED, updated.versionType)
    assertNotNull(updated.published)

    val unpublished = versionRepository.getByPublicId(published.publicId)
    assertEquals(VersionType.ARCHIVED, unpublished.versionType)
    assertNotNull(unpublished.archived)
  }
}
