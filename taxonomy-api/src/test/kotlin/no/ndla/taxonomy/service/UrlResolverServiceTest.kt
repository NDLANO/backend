/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import jakarta.persistence.EntityManager
import java.net.URI
import no.ndla.taxonomy.domain.Builder
import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.repositories.NodeRepository
import no.ndla.taxonomy.repositories.UrlMappingRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
class UrlResolverServiceTest : AbstractIntegrationTest() {

  @Autowired private lateinit var builder: Builder
  @Autowired private lateinit var entityManager: EntityManager
  @Autowired private lateinit var nodeRepository: NodeRepository
  @Autowired private lateinit var urlMappingRepository: UrlMappingRepository
  @Autowired private lateinit var oldUrlCanonifier: OldUrlCanonifier

  private lateinit var urlResolverService: UrlResolverService

  @BeforeEach
  fun clearAllRepos() {
    nodeRepository.deleteAllAndFlush()
  }

  @BeforeEach
  fun restTestSetUp() {
    urlResolverService = UrlResolverService(urlMappingRepository, nodeRepository, oldUrlCanonifier)
  }

  @Test
  @Transactional
  fun resolveOldUrl() {
    val subjectId = "urn:subject:11"
    val nodeId = "urn:topic:1:183926"
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId(subjectId).child(NodeType.TOPIC) { t -> t.publicId(nodeId) }
    }
    val oldUrl = "ndla.no/node/183926?fag=127013"
    val urlMapping =
        builder.urlMapping { it.oldUrl(oldUrl).public_id(nodeId).subject_id(subjectId) }
    entityManager.persist(urlMapping)
    entityManager.flush()

    assertEquals(
        "/subject:11/topic:1:183926",
        requireNotNull(urlResolverService.resolveOldUrl(oldUrl)),
    )
  }

  @Test
  @Transactional
  fun queryForNonExistingNodeShouldNotMatchSimilarNodeId() {
    val oldUrl = "ndla.no/node/54"
    val otherSubjectId = "urn:subject:1"
    val otherTopicId = "urn:topic:1:54321"
    val otherTopicUrl = "ndla.no/node/54321"

    // create another topic and mapping that should NOT match the query for the url above
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId(otherSubjectId).child(NodeType.TOPIC) { t ->
        t.publicId("urn:topic:1:54321")
      }
    }
    entityManager.persist(
        builder.urlMapping {
          it.oldUrl(otherTopicUrl).public_id(otherTopicId).subject_id(otherSubjectId)
        })
    entityManager.flush()
    assertNull(urlResolverService.resolveOldUrl(oldUrl))
  }

  @Test
  @Transactional
  fun resolveOldUrlWithLanguage() {
    val subjectId = "urn:subject:11"
    val nodeId = "urn:topic:1:183926"
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId(subjectId).child(NodeType.TOPIC) { t -> t.publicId(nodeId) }
    }
    val oldUrl = "ndla.no/node/183926?fag=127013"
    entityManager.persist(
        builder.urlMapping { it.oldUrl(oldUrl).public_id(nodeId).subject_id(subjectId) })
    entityManager.flush()
    assertEquals(
        "/subject:11/topic:1:183926",
        requireNotNull(urlResolverService.resolveOldUrl("ndla.no/nb/node/183926?fag=127013")),
    )
  }

  @Test
  @Transactional
  fun resolveOldUrlWhenNoSubjectImportedToPrimaryPath() {
    val nodeId = "urn:topic:1:183926"
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:2").child(NodeType.TOPIC) { t -> t.publicId(nodeId) }
    }

    val oldUrl = "ndla.no/node/183926?fag=127013"
    entityManager.persist(builder.urlMapping { it.oldUrl(oldUrl).public_id(nodeId) })
    entityManager.flush()
    assertEquals(
        "/subject:2/topic:1:183926",
        requireNotNull(urlResolverService.resolveOldUrl(oldUrl)),
    )
  }

  @Test
  @Transactional
  fun resolveOldUrlWhenNoSubjectImportedOrQueriedToPrimaryPath() {
    val nodeId = "urn:topic:1:183926"
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:2").child(NodeType.TOPIC) { t -> t.publicId(nodeId) }
    }

    val oldUrl = "ndla.no/node/183926"
    entityManager.persist(builder.urlMapping { it.oldUrl(oldUrl).public_id(nodeId) })
    entityManager.flush()
    assertEquals(
        "/subject:2/topic:1:183926",
        requireNotNull(urlResolverService.resolveOldUrl(oldUrl)),
    )
  }

  @Test
  @Transactional
  fun resolveOldUrlWhenSubjectImportedButNotQueriedToPrimaryPath() {
    val subjectId = "urn:subject:11"
    val nodeId = "urn:topic:1:183926"
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId(subjectId).child(NodeType.TOPIC) { t -> t.publicId(nodeId) }
    }
    val oldUrl = "ndla.no/node/183926?fag=127013"
    entityManager.persist(
        builder.urlMapping { it.oldUrl(oldUrl).public_id(nodeId).subject_id(subjectId) })
    entityManager.flush()
    assertEquals(
        "/subject:11/topic:1:183926",
        requireNotNull(urlResolverService.resolveOldUrl("ndla.no/node/183926")),
    )
  }

  @Test
  @Transactional
  fun resolveOldUrlBadSubjectPrimaryPath() {
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:2").child(NodeType.TOPIC) { t ->
        t.publicId("urn:topic:1:183926")
      }
    }
    val oldUrl = "ndla.no/node/183926?fag=127013"
    entityManager.persist(
        builder.urlMapping {
          it.oldUrl(oldUrl).public_id("urn:topic:1:183926").subject_id("urn:subject:11")
        })
    entityManager.flush()
    assertEquals(
        "/subject:2/topic:1:183926",
        requireNotNull(urlResolverService.resolveOldUrl(oldUrl)),
    )
  }

  @Test
  @Transactional
  fun putOldUrlForNonexistentResource() {
    assertThrows<Exception> {
      urlResolverService.putUrlMapping(
          "abc",
          URI.create("urn:topic:1:12"),
          URI.create("urn:subject:12"),
      )
    }
  }

  @Test
  @Transactional
  fun putOldUrl() {
    val subjectId = "urn:subject:12"
    val nodeId = "urn:topic:1:183926"
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId(subjectId).child(NodeType.TOPIC) { t -> t.publicId(nodeId) }
    }
    entityManager.flush()

    val oldUrl = "ndla.no/nb/node/183926?fag=127013"
    urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId))
    entityManager.flush()

    assertEquals(
        "/subject:12/topic:1:183926",
        requireNotNull(urlResolverService.resolveOldUrl(oldUrl)),
    )
  }

  @Test
  @Transactional
  fun putOldUrlTwice() {
    val subjectId = "urn:subject:12"
    val nodeId = "urn:topic:1:183926"
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId(subjectId).child(NodeType.TOPIC) { t -> t.publicId(nodeId) }
    }
    entityManager.flush()

    val oldUrl = "ndla.no/nb/node/183926?fag=127013"
    urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId))
    urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId))
    entityManager.flush()

    assertEquals(
        "/subject:12/topic:1:183926",
        requireNotNull(urlResolverService.resolveOldUrl(oldUrl)),
    )
  }

  @Test
  @Transactional
  fun putOldUrlWithNoPaths() {
    val subjectId = "urn:subject:12"
    val nodeId = "urn:topic:1:183926"
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId(subjectId).child(NodeType.TOPIC) { t -> t.publicId(nodeId) }
    }
    entityManager.flush()

    val oldUrl = "ndla.no/nb/node/183926?fag=127013"
    assertThrows<UrlResolverService.NodeIdNotFoundException> {
      urlResolverService.putUrlMapping(
          oldUrl,
          URI.create("urn:topic:1:283926"),
          URI.create(subjectId),
      )
    }
  }

  @Test
  @Transactional
  fun putOldUrlWithSubjectQueryWithoutSubject() {
    val subjectId = "urn:subject:12"
    val nodeId = "urn:topic:1:183926"
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId(subjectId).child(NodeType.TOPIC) { t -> t.publicId(nodeId) }
    }
    entityManager.flush()

    val oldUrl = "ndla.no/nb/node/183926?fag=127013"
    urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId))
    entityManager.flush()

    assertEquals(
        "/subject:12/topic:1:183926",
        requireNotNull(urlResolverService.resolveOldUrl("ndla.no/nb/node/183926")),
    )
  }

  @Test
  @Transactional
  fun putOldUrlInsertsNewRowWhenRelatedRowWithDifferentQueryParamExists() {
    val subjectId = "urn:subject:12"
    val nodeId = "urn:topic:1:183926"
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId(subjectId).child(NodeType.TOPIC) { t -> t.publicId(nodeId) }
    }
    entityManager.flush()

    urlResolverService.putUrlMapping(
        "ndla.no/nb/node/183926?fag=127013",
        URI.create(nodeId),
        URI.create(subjectId),
    )

    urlResolverService.putUrlMapping("ndla.no/nb/node/183926", URI.create(nodeId))
    entityManager.flush()

    assertFalse(urlMappingRepository.findAllByOldUrl("ndla.no/node/183926").isEmpty())
    assertFalse(urlMappingRepository.findAllByOldUrl("ndla.no/node/183926?fag=127013").isEmpty())
  }

  @Test
  @Transactional
  fun resolveEntitiesFromPath() {
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:1").name("Maths").child(NodeType.TOPIC) { t ->
        t.publicId("urn:topic:1").name("Trigonometry").resource("resource") { resourceBuilder ->
          resourceBuilder
              .publicId("urn:resource:1")
              .name("Resource Name")
              .contentUri(URI.create("urn:test:1"))
        }
      }
    }

    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:2").name("Biology").child(NodeType.TOPIC) { t ->
        t.publicId("urn:topic:2").name("Mammals").resource("resource")
      }
    }

    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:3").name("Chemistry").child(NodeType.TOPIC) { t ->
        t.publicId("urn:topic:3").isContext(true).name("Acids").resource("resource")
      }
    }

    builder.node(NodeType.RESOURCE) { it.publicId("urn:resource:2").name("Resource Name") }

    // Four paths exists to the same resource:
    // /subject:1/topic:1/resource:1
    // /subject:2/topic:2/resource:1
    // /subject:3/topic:3/resource:1
    // /topic:3/resource:1

    run {
      val resolvedUrl =
          requireNotNull(urlResolverService.resolveUrl("/subject:1/topic:1/resource:1", "nb"))
      assertEquals(2, resolvedUrl.parents.size)

      val parentIdList = resolvedUrl.parents.map { it.schemeSpecificPart }
      assertEquals("resource:1", resolvedUrl.id.schemeSpecificPart)
      assertEquals("Resource Name", resolvedUrl.name)
      assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri)
      assertEquals("/subject:1/topic:1/resource:1", resolvedUrl.path)
      assertTrue(resolvedUrl.url.startsWith("/r/maths/resource-name/"))

      assertEquals("topic:1", parentIdList[0])
      assertEquals("subject:1", parentIdList[1])
    }

    run {
      val resolvedUrl =
          requireNotNull(urlResolverService.resolveUrl("/subject:2/topic:2/resource:1", "nb"))
      assertEquals(2, resolvedUrl.parents.size)

      val parentIdList = resolvedUrl.parents.map { it.schemeSpecificPart }

      assertEquals("resource:1", resolvedUrl.id.schemeSpecificPart)
      assertEquals("Resource Name", resolvedUrl.name)
      assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri)
      assertEquals("/subject:2/topic:2/resource:1", resolvedUrl.path)
      assertTrue(resolvedUrl.url.startsWith("/r/biology/resource-name/"))

      assertEquals("topic:2", parentIdList[0])
      assertEquals("subject:2", parentIdList[1])
    }

    run {
      val resolvedUrl = requireNotNull(urlResolverService.resolveUrl("/subject:2/topic:2", "nb"))
      assertEquals(1, resolvedUrl.parents.size)

      val parentIdList = resolvedUrl.parents.map { it.schemeSpecificPart }

      assertEquals("topic:2", resolvedUrl.id.schemeSpecificPart)
      assertEquals("/subject:2/topic:2", resolvedUrl.path)
      assertTrue(resolvedUrl.url.startsWith("/e/biology/mammals/"))
      assertEquals("subject:2", parentIdList.first())
    }

    run {
      val resolvedUrl = requireNotNull(urlResolverService.resolveUrl("/subject:2", "nb"))
      assertEquals(0, resolvedUrl.parents.size)
      assertEquals("subject:2", resolvedUrl.id.schemeSpecificPart)
      assertEquals("/subject:2", resolvedUrl.path)
      assertTrue(resolvedUrl.url.startsWith("/f/biology/"))
    }

    run {
      val resolvedUrl = requireNotNull(urlResolverService.resolveUrl("/subject:2/", "nb"))
      assertEquals(0, resolvedUrl.parents.size)
      assertEquals("subject:2", resolvedUrl.id.schemeSpecificPart)
      assertEquals("/subject:2", resolvedUrl.path)
      assertTrue(resolvedUrl.url.startsWith("/f/biology/"))
    }

    run {
      val resolvedUrl = requireNotNull(urlResolverService.resolveUrl("subject:2", "nb"))
      assertEquals(0, resolvedUrl.parents.size)
      assertEquals("subject:2", resolvedUrl.id.schemeSpecificPart)
      assertEquals("/subject:2", resolvedUrl.path)
      assertTrue(resolvedUrl.url.startsWith("/f/biology/"))
    }

    // Test with a non-valid path to valid resource
    run {
      val resolvedUrl = requireNotNull(urlResolverService.resolveUrl("/subject:2/resource:1", "nb"))
      assertFalse(resolvedUrl.exactMatch)
    }

    // Test with a non-valid path to valid resource
    run {
      val resolvedUrl =
          requireNotNull(urlResolverService.resolveUrl("/subject:2/topic:4/resource:1", "nb"))
      assertFalse(resolvedUrl.exactMatch)
    }

    // Test with non-context node
    run {
      assertNull(urlResolverService.resolveUrl("/topic:1/resource:2", "nb"))
      assertNull(urlResolverService.resolveUrl("/topic:2/resource:2", "nb"))
    }

    // Since topic3 is a context in itself, it would be valid to use it as root
    run {
      val resolvedUrl = requireNotNull(urlResolverService.resolveUrl("/topic:3/resource:1", "nb"))
      assertEquals(1, resolvedUrl.parents.size)

      val parentIdList = resolvedUrl.parents.map { it.schemeSpecificPart }

      assertEquals("resource:1", resolvedUrl.id.schemeSpecificPart)
      assertEquals("Resource Name", resolvedUrl.name)
      assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri)
      assertEquals("/topic:3/resource:1", resolvedUrl.path)
      assertTrue(resolvedUrl.url.startsWith("/r/acids/resource-name/"))
      assertEquals("topic:3", parentIdList.first())
    }

    // Going via subject:3 is also valid
    run {
      val resolvedUrl =
          requireNotNull(urlResolverService.resolveUrl("/subject:3/topic:3/resource:1", "nb"))
      assertEquals(2, resolvedUrl.parents.size)
      val parentIdList = resolvedUrl.parents.map { it.schemeSpecificPart }

      assertEquals("resource:1", resolvedUrl.id.schemeSpecificPart)
      assertEquals("Resource Name", resolvedUrl.name)
      assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri)
      assertEquals("/subject:3/topic:3/resource:1", resolvedUrl.path)
      assertTrue(resolvedUrl.url.startsWith("/r/chemistry/resource-name/"))

      assertEquals("topic:3", parentIdList[0])
      assertEquals("subject:3", parentIdList[1])
    }

    // Additional slashes should make no difference
    run {
      val resolvedUrl =
          requireNotNull(
              urlResolverService.resolveUrl("////subject:3///topic:3//////resource:1///", "nb"))
      assertEquals(2, resolvedUrl.parents.size)

      val parentIdList = resolvedUrl.parents.map { it.schemeSpecificPart }

      assertEquals("resource:1", resolvedUrl.id.schemeSpecificPart)
      assertEquals("Resource Name", resolvedUrl.name)
      assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri)
      assertEquals("/subject:3/topic:3/resource:1", resolvedUrl.path)
      assertTrue(resolvedUrl.url.startsWith("/r/chemistry/resource-name/"))

      assertEquals("topic:3", parentIdList[0])
      assertEquals("subject:3", parentIdList[1])
    }

    // No leading slash should make no difference
    run {
      val resolvedUrl =
          requireNotNull(
              urlResolverService.resolveUrl("subject:3///topic:3//////resource:1///", "nb"))

      assertEquals(2, resolvedUrl.parents.size)

      val parentIdList = resolvedUrl.parents.map { it.schemeSpecificPart }

      assertEquals("resource:1", resolvedUrl.id.schemeSpecificPart)
      assertEquals("Resource Name", resolvedUrl.name)
      assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri)
      assertEquals("/subject:3/topic:3/resource:1", resolvedUrl.path)
      assertTrue(resolvedUrl.url.startsWith("/r/chemistry/resource-name/"))

      assertEquals("topic:3", parentIdList[0])
      assertEquals("subject:3", parentIdList[1])
    }
  }
}
