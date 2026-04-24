/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.service.dtos.ResolvedUrl;
import org.junit.jupiter.api.Test;

public class UrlResolverTest extends RestTest {

    @Test
    public void can_resolve_url_for_subject() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .contentUri("urn:article:1")
                .name("the subject"));

        ResolvedUrl url = resolveUrl("/subject:1");

        assertEquals("urn:subject:1", url.getId().toString());
        assertEquals("the subject", url.getName());
        assertEquals("urn:article:1", url.getContentUri().toString());
        assertEquals(0, url.getParents().size());
    }

    @Test
    public void can_resolve_url_for_topic() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .name("the topic")
                        .contentUri("urn:article:1")));

        ResolvedUrl url = resolveUrl("/subject:1/topic:1");

        assertEquals("urn:topic:1", url.getId().toString());
        assertEquals("the topic", url.getName());
        assertEquals("urn:article:1", url.getContentUri().toString());
        assertParents(url, "urn:subject:1");
    }

    @Test
    public void can_resolve_url_for_subtopic() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .child(NodeType.TOPIC, st -> st.publicId("urn:topic:2").contentUri("urn:article:1"))));

        ResolvedUrl url = resolveUrl("/subject:1/topic:1/topic:2");
        assertEquals("urn:article:1", url.getContentUri().toString());
        assertParents(url, "urn:topic:1", "urn:subject:1");
    }

    @Test
    public void can_resolve_url_for_resource() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .resource(r -> r.publicId("urn:resource:1").contentUri("urn:article:1"))));

        ResolvedUrl url = resolveUrl("/subject:1/topic:1/resource:1");
        assertEquals("urn:article:1", url.getContentUri().toString());
        assertParents(url, "urn:topic:1", "urn:subject:1");
    }

    @Test
    public void ignores_multiple_or_leading_or_trailing_slashes() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).publicId("urn:subject:1").child(NodeType.TOPIC, t -> t.publicId("urn:topic:1")
                        .resource(r -> r.publicId("urn:resource:1").contentUri("urn:article:1"))));

        {
            ResolvedUrl url = resolveUrl("/subject:1/topic:1/resource:1");
            assertEquals("urn:article:1", url.getContentUri().toString());
            assertParents(url, "urn:topic:1", "urn:subject:1");
            assertEquals("/subject:1/topic:1/resource:1", url.getPath());
        }
        {
            ResolvedUrl url = resolveUrl("subject:1/topic:1/resource:1");
            assertEquals("urn:article:1", url.getContentUri().toString());
            assertParents(url, "urn:topic:1", "urn:subject:1");
            assertEquals("/subject:1/topic:1/resource:1", url.getPath());
        }
        {
            ResolvedUrl url = resolveUrl("/subject:1/topic:1/resource:1/");
            assertEquals("urn:article:1", url.getContentUri().toString());
            assertParents(url, "urn:topic:1", "urn:subject:1");
            assertEquals("/subject:1/topic:1/resource:1", url.getPath());
        }
        {
            ResolvedUrl url = resolveUrl("//subject:1////topic:1/resource:1//");
            assertEquals("urn:article:1", url.getContentUri().toString());
            assertParents(url, "urn:topic:1", "urn:subject:1");
            assertEquals("/subject:1/topic:1/resource:1", url.getPath());
        }
    }

    @Test
    public void gets_200_on_wrong_path_to_resource() throws Exception {
        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .publicId("urn:subject:1")
                .child(NodeType.TOPIC, t -> t.publicId("urn:topic:1").resource(r -> r.publicId("urn:resource:1"))));

        testUtils.getResource("/v1/url/resolve?path=/subject:1/topic:2/resource:1", status().isOk());
        testUtils.getResource("/v1/url/resolve?path=/subject:1/topic:1/resource:1", status().isOk());
    }

    @Test
    public void sends_404_when_not_found() throws Exception {
        String path = "/no/such/element";
        testUtils.getResource("/v1/url/resolve?path=" + path, status().isNotFound());
    }

    private void assertParents(ResolvedUrl path, String... expected) {
        assertEquals(expected.length, path.getParents().size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], path.getParents().get(i).toString());
        }
    }

    private ResolvedUrl resolveUrl(String url) throws Exception {
        return testUtils.getObject(ResolvedUrl.class, testUtils.getResource("/v1/url/resolve?path=" + url));
    }
}
