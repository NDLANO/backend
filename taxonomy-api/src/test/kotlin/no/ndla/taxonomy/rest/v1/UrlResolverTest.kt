/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1

import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.service.dtos.ResolvedUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UrlResolverTest : RestTest() {

  @Test
  fun can_resolve_url_for_subject() {
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:1").contentUri("urn:article:1").name("the subject")
    }
    val url = resolveUrl("/subject:1")

    assertEquals("urn:subject:1", url.id.toString())
    assertEquals("the subject", url.name)
    assertEquals("urn:article:1", url.contentUri.toString())
    assertEquals(0, url.parents.size)
  }

  @Test
  fun can_resolve_url_for_topic() {
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC) { t ->
        t.publicId("urn:topic:1").name("the topic").contentUri("urn:article:1")
      }
    }

    val url = resolveUrl("/subject:1/topic:1")

    assertEquals("urn:topic:1", url.id.toString())
    assertEquals("the topic", url.name)
    assertEquals("urn:article:1", url.contentUri.toString())
    assertParents(url, "urn:subject:1")
  }

  @Test
  fun can_resolve_url_for_subtopic() {
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC) { t ->
        t.publicId("urn:topic:1").child(NodeType.TOPIC) { st ->
          st.publicId("urn:topic:2").contentUri("urn:article:1")
        }
      }
    }

    val url = resolveUrl("/subject:1/topic:1/topic:2")
    assertEquals("urn:article:1", url.contentUri.toString())
    assertParents(url, "urn:topic:1", "urn:subject:1")
  }

  @Test
  fun can_resolve_url_for_resource() {
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC) { t ->
        t.publicId("urn:topic:1").resource { r ->
          r.publicId("urn:resource:1").contentUri("urn:article:1")
        }
      }
    }
    val url = resolveUrl("/subject:1/topic:1/resource:1")
    assertEquals("urn:article:1", url.contentUri.toString())
    assertParents(url, "urn:topic:1", "urn:subject:1")
  }

  @Test
  fun ignores_multiple_or_leading_or_trailing_slashes() {
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC) { t ->
        t.publicId("urn:topic:1").resource { r ->
          r.publicId("urn:resource:1").contentUri("urn:article:1")
        }
      }
    }

    run {
      val url = resolveUrl("/subject:1/topic:1/resource:1")
      assertEquals("urn:article:1", url.contentUri.toString())
      assertParents(url, "urn:topic:1", "urn:subject:1")
      assertEquals("/subject:1/topic:1/resource:1", url.path)
    }

    run {
      val url = resolveUrl("subject:1/topic:1/resource:1")
      assertEquals("urn:article:1", url.contentUri.toString())
      assertParents(url, "urn:topic:1", "urn:subject:1")
      assertEquals("/subject:1/topic:1/resource:1", url.path)
    }

    run {
      val url = resolveUrl("/subject:1/topic:1/resource:1/")
      assertEquals("urn:article:1", url.contentUri.toString())
      assertParents(url, "urn:topic:1", "urn:subject:1")
      assertEquals("/subject:1/topic:1/resource:1", url.path)
    }
    run {
      val url = resolveUrl("//subject:1////topic:1/resource:1//")
      assertEquals("urn:article:1", url.contentUri.toString())
      assertParents(url, "urn:topic:1", "urn:subject:1")
      assertEquals("/subject:1/topic:1/resource:1", url.path)
    }
  }

  @Test
  fun gets_200_on_wrong_path_to_resource() {
    builder.node(NodeType.SUBJECT) {
      it.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC) { t ->
        t.publicId("urn:topic:1").resource { r -> r.publicId("urn:resource:1") }
      }
    }

    testUtils.getResource("/v1/url/resolve?path=/subject:1/topic:2/resource:1", status().isOk())
    testUtils.getResource("/v1/url/resolve?path=/subject:1/topic:1/resource:1", status().isOk())
  }

  @Test
  fun sends_404_when_not_found() {
    val path = "/no/such/element"
    testUtils.getResource("/v1/url/resolve?path=$path", status().isNotFound())
  }

  private fun assertParents(path: ResolvedUrl, vararg expected: String) {
    assertEquals(expected.toList(), path.parents.map { it.toString() })
  }

  private fun resolveUrl(url: String): ResolvedUrl =
      testUtils.getObject(
          ResolvedUrl::class.java,
          testUtils.getResource("/v1/url/resolve?path=$url"),
      )
}
