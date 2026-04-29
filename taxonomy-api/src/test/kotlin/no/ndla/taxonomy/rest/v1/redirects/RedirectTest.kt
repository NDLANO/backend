/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.redirects

import no.ndla.taxonomy.rest.v1.RestTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class RedirectTest : RestTest() {

    private fun assertRedirect(from: String, expectedLocation: String) {
        val response = testUtils.getResource(from, status().isPermanentRedirect())
        assertEquals(expectedLocation, response.getHeader("Location"))
    }

    // /v1/subjects → /v1/nodes?nodeType=SUBJECT
    @Test
    fun `subjects root redirects with SUBJECT nodeType filter`() {
        assertRedirect("/v1/subjects", "/v1/nodes?nodeType=SUBJECT")
    }

    @Test
    fun `subjects root preserves query string and appends nodeType filter`() {
        assertRedirect(
            "/v1/subjects?language=nb",
            "/v1/nodes?language=nb&nodeType=SUBJECT",
        )
    }

    @Test
    fun `subjects search redirects to nodes search with SUBJECT filter`() {
        assertRedirect(
            "/v1/subjects/search?query=foo",
            "/v1/nodes/search?query=foo&nodeType=SUBJECT",
        )
    }

    @Test
    fun `subjects page redirects to nodes page with SUBJECT filter`() {
        assertRedirect(
            "/v1/subjects/page?page=1&pageSize=10",
            "/v1/nodes/page?page=1&pageSize=10&nodeType=SUBJECT",
        )
    }

    @Test
    fun `subject by id redirects to node by id`() {
        assertRedirect("/v1/subjects/urn:subject:1", "/v1/nodes/urn:subject:1")
    }

    @Test
    fun `subject by id preserves query string`() {
        assertRedirect(
            "/v1/subjects/urn:subject:1?language=nb",
            "/v1/nodes/urn:subject:1?language=nb",
        )
    }

    @Test
    fun `subject topics endpoint redirects to nodes children with TOPIC filter`() {
        assertRedirect(
            "/v1/subjects/urn:subject:1/topics",
            "/v1/nodes/urn:subject:1/nodes?nodeType=TOPIC",
        )
    }

    @Test
    fun `subject topics endpoint preserves query string`() {
        assertRedirect(
            "/v1/subjects/urn:subject:1/topics?recursive=true",
            "/v1/nodes/urn:subject:1/nodes?recursive=true&nodeType=TOPIC",
        )
    }

    @Test
    fun `subject resources endpoint forces recursive=true`() {
        assertRedirect(
            "/v1/subjects/urn:subject:1/resources",
            "/v1/nodes/urn:subject:1/resources?recursive=true",
        )
    }

    @Test
    fun `subject metadata endpoint redirects through generic fallback`() {
        assertRedirect(
            "/v1/subjects/urn:subject:1/metadata",
            "/v1/nodes/urn:subject:1/metadata",
        )
    }

    // /v1/topics → /v1/nodes?nodeType=TOPIC
    @Test
    fun `topics root redirects with TOPIC nodeType filter`() {
        assertRedirect("/v1/topics", "/v1/nodes?nodeType=TOPIC")
    }

    @Test
    fun `topics search redirects with TOPIC filter`() {
        assertRedirect(
            "/v1/topics/search?query=x",
            "/v1/nodes/search?query=x&nodeType=TOPIC",
        )
    }

    @Test
    fun `topic by id redirects to node by id`() {
        assertRedirect("/v1/topics/urn:topic:1", "/v1/nodes/urn:topic:1")
    }

    @Test
    fun `topic subtopic-via-topics endpoint redirects to nodes children with TOPIC filter`() {
        assertRedirect(
            "/v1/topics/urn:topic:1/topics",
            "/v1/nodes/urn:topic:1/nodes?nodeType=TOPIC",
        )
    }

    @Test
    fun `topic resources endpoint redirects to nodes resources without recursive override`() {
        assertRedirect(
            "/v1/topics/urn:topic:1/resources",
            "/v1/nodes/urn:topic:1/resources",
        )
    }

    // /v1/resources → /v1/nodes?nodeType=RESOURCE
    @Test
    fun `resources root redirects with RESOURCE nodeType filter`() {
        assertRedirect("/v1/resources", "/v1/nodes?nodeType=RESOURCE")
    }

    @Test
    fun `resource search redirects with RESOURCE filter`() {
        assertRedirect(
            "/v1/resources/search?query=x",
            "/v1/nodes/search?query=x&nodeType=RESOURCE",
        )
    }

    @Test
    fun `resource by id redirects to node by id`() {
        assertRedirect("/v1/resources/urn:resource:1", "/v1/nodes/urn:resource:1")
    }

    @Test
    fun `resource by id preserves query string`() {
        assertRedirect(
            "/v1/resources/urn:resource:1?language=nb",
            "/v1/nodes/urn:resource:1?language=nb",
        )
    }

    // /v1/subject-topics → /v1/node-connections
    @Test
    fun `subject-topics root redirects to node-connections root`() {
        assertRedirect("/v1/subject-topics", "/v1/node-connections")
    }

    @Test
    fun `subject-topics by id redirects to node-connections by id`() {
        assertRedirect(
            "/v1/subject-topics/urn:subject-topic:1",
            "/v1/node-connections/urn:subject-topic:1",
        )
    }

    @Test
    fun `subject-topics root preserves query string`() {
        assertRedirect(
            "/v1/subject-topics?page=1&pageSize=10",
            "/v1/node-connections?page=1&pageSize=10",
        )
    }

    // /v1/topic-subtopics → /v1/node-connections
    @Test
    fun `topic-subtopics root redirects to node-connections root`() {
        assertRedirect("/v1/topic-subtopics", "/v1/node-connections")
    }

    @Test
    fun `topic-subtopics by id redirects to node-connections by id`() {
        assertRedirect(
            "/v1/topic-subtopics/urn:topic-subtopic:1",
            "/v1/node-connections/urn:topic-subtopic:1",
        )
    }

    // /v1/topic-resources → /v1/node-resources
    @Test
    fun `topic-resources root redirects to node-resources root`() {
        assertRedirect("/v1/topic-resources", "/v1/node-resources")
    }

    @Test
    fun `topic-resources by id redirects to node-resources by id`() {
        assertRedirect(
            "/v1/topic-resources/urn:topic-resource:1",
            "/v1/node-resources/urn:topic-resource:1",
        )
    }

    @Test
    fun `topic-resources by id preserves query string`() {
        assertRedirect(
            "/v1/topic-resources/urn:topic-resource:1?language=nb",
            "/v1/node-resources/urn:topic-resource:1?language=nb",
        )
    }
}
