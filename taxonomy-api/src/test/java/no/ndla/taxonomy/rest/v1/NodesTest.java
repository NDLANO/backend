/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.commands.NodePostPut;
import no.ndla.taxonomy.rest.v1.dtos.NodeConnectionPOST;
import no.ndla.taxonomy.rest.v1.dtos.NodeResourcePOST;
import no.ndla.taxonomy.service.dtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;

public class NodesTest extends RestTest {
    @Autowired
    EntityManager entityManager;

    @Autowired
    private TestSeeder testSeeder;

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
        resourceTypeRepository.deleteAllAndFlush();
        resourceResourceTypeRepository.deleteAllAndFlush();
    }

    @Test
    public void can_get_single_node() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("maths").publicId("urn:subject:1").child(t -> t.nodeType(NodeType.TOPIC)
                        .name("trigonometry")
                        .contentUri("urn:article:1")
                        .publicId("urn:topic:1")));

        var response = testUtils.getResource("/v1/nodes/urn:topic:1");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertEquals("trigonometry", node.getName());
        assertEquals("Optional[urn:article:1]", node.getContentUri().toString());
        assertEquals("/subject:1/topic:1", node.getPath().get());
        assertEquals(List.of("maths", "trigonometry"), node.getBreadcrumbs());

        assertNotNull(node.getMetadata());
        assertTrue(node.getMetadata().isVisible());
    }

    @Test
    void can_get_single_node_with_wanted_context() throws Exception {
        Node resource = builder.node(NodeType.RESOURCE, r -> r.name("Resource").publicId("urn:resource:1"));
        builder.node(NodeType.SUBJECT, s1 -> s1.isContext(true)
                .name("Subject1")
                .publicId("urn:subject:1")
                .child(
                        NodeType.TOPIC,
                        t1 -> t1.name("Topic1").publicId("urn:topic:1").resource(resource)));
        builder.node(NodeType.SUBJECT, s2 -> s2.isContext(true)
                .name("Subject2")
                .publicId("urn:subject:2")
                .child(NodeType.TOPIC, t2 -> t2.name("Topic2")
                        .publicId("urn:topic:2")
                        .isContext(true)
                        .child(resource)
                        .child(
                                NodeType.TOPIC,
                                t3 -> t3.name("Topic3").publicId("urn:topic:3").resource(resource))));

        // Specifying rootId and/or parentId picks wanted path
        {
            var response = testUtils.getResource("/v1/nodes/urn:resource:1?rootId=urn:subject:1");
            final var node = testUtils.getObject(NodeDTO.class, response);
            assertEquals("Resource", node.getName());
            assertEquals(5, node.getContexts().size());
            assertEquals(2, node.getContext().get().parents().size());
            assertEquals("/subject:1/topic:1/resource:1", node.getPath().get());
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:resource:1?parentId=urn:topic:1");
            final var node = testUtils.getObject(NodeDTO.class, response);
            assertEquals("Resource", node.getName());
            assertEquals(2, node.getContext().get().parents().size());
            assertEquals("/subject:1/topic:1/resource:1", node.getPath().get());
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:resource:1?rootId=urn:subject:2");
            final var node = testUtils.getObject(NodeDTO.class, response);
            assertEquals("Resource", node.getName());
            assertEquals(2, node.getContext().get().parents().size());
            assertEquals("/subject:2/topic:2/resource:1", node.getPath().get());
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:resource:1?rootId=urn:topic:2");
            final var node = testUtils.getObject(NodeDTO.class, response);
            assertEquals("Resource", node.getName());
            assertEquals(1, node.getContext().get().parents().size());
            assertEquals("/topic:2/resource:1", node.getPath().get());
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:resource:1?rootId=urn:topic:2&parentId=urn:topic:3");
            final var node = testUtils.getObject(NodeDTO.class, response);
            assertEquals("Resource", node.getName());
            assertEquals("/topic:2/topic:3/resource:1", node.getPath().get());
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:resource:1?rootId=urn:subject:2&parentId=urn:topic:3");
            final var node = testUtils.getObject(NodeDTO.class, response);
            assertEquals("Resource", node.getName());
            assertEquals(3, node.getContext().get().parents().size());
            assertEquals("/subject:2/topic:2/topic:3/resource:1", node.getPath().get());
        }
    }

    @Test
    public void single_node_has_no_url() throws Exception {
        builder.node(NodeType.NODE, t -> t.publicId("urn:node:1"));

        var response = testUtils.getResource("/v1/nodes/urn:node:1");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertTrue(node.getPath().isEmpty());
    }

    @Test
    public void single_context_node_has_url() throws Exception {
        builder.node(NodeType.NODE, t -> t.isContext(true).publicId("urn:node:1"));

        var response = testUtils.getResource("/v1/nodes/urn:node:1");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertEquals("/node:1", node.getPath().get());
    }

    @Test
    public void can_get_nodes_by_contentURI() throws Exception {
        builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> {
                    t.name("photo synthesis");
                    t.contentUri(URI.create("urn:test:1"));
                }));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> {
            t.name("trigonometry");
            t.contentUri(URI.create("urn:test:2"));
        }));

        {
            final var response = testUtils.getResource("/v1/nodes?contentURI=urn:test:1");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("photo synthesis", nodes[0].getName());
            assertEquals(List.of("Basic science", "photo synthesis"), nodes[0].getBreadcrumbs());
        }

        {
            final var response = testUtils.getResource("/v1/nodes?contentURI=urn:test:2");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("trigonometry", nodes[0].getName());
            assertEquals(List.of("Maths", "trigonometry"), nodes[0].getBreadcrumbs());
        }
    }

    @Test
    public void can_get_nodes_by_contextId() {
        Node resource = builder.node(NodeType.RESOURCE, r -> r.name("Resource"));
        builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> {
                    t.name("photo synthesis");
                    t.contentUri(URI.create("urn:test:1")).resource(resource);
                }));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> {
            t.name("trigonometry");
            t.contentUri(URI.create("urn:test:2")).resource(resource);
        }));

        Optional<Node> fromDB = nodeRepository.findFirstByPublicId(resource.getPublicId());
        assertTrue(fromDB.isPresent());
        assertEquals(2, fromDB.get().getContexts().size());

        // Check fetching by contextid and make sure correct path and breadcrumbs is used
        fromDB.get().getContexts().forEach(context -> {
            try {
                final var response =
                        testUtils.getResource("/v1/nodes?nodeType=RESOURCE&contextId=" + context.contextId());
                final var nodes = testUtils.getObject(NodeDTO[].class, response);
                assertEquals(1, nodes.length);
                assertEquals("Resource", nodes[0].getName());
                assertTrue(nodes[0].getUrl().get().endsWith(String.format("resource/%s", context.contextId())));
                assertEquals(context.path(), nodes[0].getPath().get());
                assertTrue(nodes[0].getBreadcrumbs()
                        .containsAll(context.breadcrumbs().get("nb")));
            } catch (Exception e) {
                // Not happening
            }
        });
    }

    @Test
    public void can_get_moved_nodes_by_contextId() {
        Node resource = builder.node(NodeType.RESOURCE, r -> r.name("Resource"));
        builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> {
                    t.name("photo synthesis");
                    t.contentUri(URI.create("urn:test:1")).resource(resource);
                }));

        Optional<Node> fromDB = nodeRepository.findFirstByPublicId(resource.getPublicId());
        assertTrue(fromDB.isPresent());
        var node = fromDB.get();
        assertEquals(1, node.getContexts().size());
        var contextId = node.getContextIds().stream().findFirst().get();

        // Disconnect from original placement
        node.removeParentConnection(
                fromDB.get().getParentConnections().stream().findFirst().get());
        nodeRepository.save(node);

        // Add resource to new placement
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> {
            t.name("trigonometry");
            t.contentUri(URI.create("urn:test:2")).resource(resource);
        }));

        // Try fetching by old contextid
        try {
            final var response = testUtils.getResource("/v1/nodes?nodeType=RESOURCE&contextId=" + contextId);
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("Resource", nodes[0].getName());
            // Should not be the old contextid
            assertFalse(nodes[0].getUrl().get().endsWith(String.format("resource/%s", contextId)));
        } catch (Exception e) {
            // Not happening
        }
    }

    @Test
    public void can_get_nodes_by_key_and_value() throws Exception {
        builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Basic science").child(t -> {
                    t.nodeType(NodeType.TOPIC);
                    t.name("photo synthesis");
                    t.contentUri(URI.create("urn:test:1"));
                    t.grepCode("GREP1");
                    t.customField("test", "value");
                }));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> {
            t.name("trigonometry");
            t.contentUri(URI.create("urn:test:2"));
            t.grepCode("GREP2");
            t.customField("test", "value2");
        }));

        {
            final var response = testUtils.getResource("/v1/nodes?value=value");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("photo synthesis", nodes[0].getName());
            assertNotNull(nodes[0].getMetadata());
            assertNotNull(nodes[0].getMetadata().getGrepCodes());
            assertEquals(Set.of("GREP1"), nodes[0].getMetadata().getGrepCodes());
        }
        {
            final var response = testUtils.getResource("/v1/nodes?key=test&value=value");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("photo synthesis", nodes[0].getName());
            assertNotNull(nodes[0].getMetadata());
            assertNotNull(nodes[0].getMetadata().getGrepCodes());
            assertEquals(Set.of("GREP1"), nodes[0].getMetadata().getGrepCodes());
        }
        {
            final var response = testUtils.getResource("/v1/nodes?key=test&value=value2");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("trigonometry", nodes[0].getName());
            assertNotNull(nodes[0].getMetadata());
            assertNotNull(nodes[0].getMetadata().getGrepCodes());
            assertEquals(Set.of("GREP2"), nodes[0].getMetadata().getGrepCodes());
        }
    }

    @Test
    public void can_get_all_nodes() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> t.name("photo synthesis")));
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> t.name("trigonometry")));

        var response = testUtils.getResource("/v1/nodes");
        final var nodes = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(4, nodes.length);

        assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
        assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
        assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
        assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
        assertAllTrue(nodes, t -> isValidId(t.getId()));
        assertAllTrue(nodes, t -> t.getPath().get().contains("subject"));

        assertAllTrue(nodes, t -> t.getMetadata() != null);
        assertAllTrue(nodes, t -> t.getMetadata().isVisible());
        assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().isEmpty());
    }

    @Test
    public void can_get_nodes_paginated() throws Exception {
        var node1 = builder.node(NodeType.NODE);
        var node2 = builder.node(NodeType.NODE);

        var response = testUtils.getResource("/v1/nodes/page?nodeType=NODE&page=1&pageSize=1");
        var page1 = testUtils.getObject(SearchResultDTO.class, response);
        assertEquals(1, page1.getResults().size());

        var response2 = testUtils.getResource("/v1/nodes/page?nodeType=NODE&page=2&pageSize=1");
        var page2 = testUtils.getObject(SearchResultDTO.class, response2);
        assertEquals(1, page2.getResults().size());

        var result = Stream.concat(page1.getResults().stream(), page2.getResults().stream())
                .toList();

        // noinspection SuspiciousMethodCalls
        assertTrue(Stream.of(node1, node2)
                .map(DomainEntity::getPublicId)
                .map(Object::toString)
                .toList()
                .containsAll(result.stream()
                        .map(r -> ((LinkedHashMap<String, String>) r).get("id"))
                        .toList()));
    }

    @Test
    public void can_get_all_root_nodes() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> t.name("photo synthesis")));
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Maths").child(NodeType.TOPIC, t -> t.name("trigonometry")));
        builder.node(NodeType.SUBJECT, s -> s.name("Arts and crafts"));
        builder.node(
                NodeType.NODE, n -> n.isContext(true).name("Random node").child(NodeType.NODE, c -> c.name("Subnode")));

        {
            {
                var response = testUtils.getResource("/v1/nodes?isRoot=true");
                final var nodes = testUtils.getObject(NodeDTO[].class, response);
                assertEquals(3, nodes.length);
                assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            }
        }
        {
            {
                var response = testUtils.getResource("/v1/nodes?isContext=true");
                final var nodes = testUtils.getObject(NodeDTO[].class, response);
                assertEquals(3, nodes.length);
                assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            }
        }
        {
            {
                var response = testUtils.getResource("/v1/nodes?isContext=false");
                final var nodes = testUtils.getObject(NodeDTO[].class, response);
                assertEquals(4, nodes.length);
                assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
                assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Arts and crafts".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Subnode".equals(t.getName()));
            }
        }
        {
            {
                var response = testUtils.getResource("/v1/nodes?isContext=true&nodeType=SUBJECT");
                final var nodes = testUtils.getObject(NodeDTO[].class, response);
                assertEquals(2, nodes.length);
                assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
                assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
            }
        }
        {
            var response = testUtils.getResource("/v1/nodes?isContext=true");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(3, nodes.length);

            assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            assertAnyTrue(nodes, t -> t.getPath().get().contains("subject"));
            assertAllTrue(nodes, t -> isValidId(t.getId()));

            assertAllTrue(nodes, t -> t.getMetadata() != null);
            assertAllTrue(nodes, t -> t.getMetadata().isVisible());
            assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().isEmpty());
        }
    }

    @Test
    public void can_get_sub_nodes() throws Exception {
        Node subject = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("Basic science")
                .child(NodeType.TOPIC, t -> t.name("photo synthesis").resource(r -> r.name("mithocondria")))
                .child(NodeType.TOPIC, t -> t.name("trigonometry").resource(r -> r.name("angles")))
                .child(NodeType.TOPIC, t -> t.name("Random node").child(NodeType.NODE, c -> c.name("Subnode"))));
        {
            var response = testUtils.getResource(
                    "/v1/nodes/" + subject.getPublicId() + "/nodes?recursive=true&nodeType=TOPIC,RESOURCE,NODE");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(6, nodes.length);

            assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
            assertAnyTrue(nodes, t -> "mithocondria".equals(t.getName()));
            assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
            assertAnyTrue(nodes, t -> "angles".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Subnode".equals(t.getName()));
            assertAnyTrue(nodes, t -> t.getPath().get().contains("subject"));
            assertAllTrue(nodes, t -> isValidId(t.getId()));

            assertAllTrue(nodes, t -> t.getMetadata() != null);
            assertAllTrue(nodes, t -> t.getMetadata().isVisible());
            assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().isEmpty());
        }
        {
            var response = testUtils.getResource(
                    "/v1/nodes/" + subject.getPublicId() + "/nodes?recursive=true&nodeType=TOPIC,RESOURCE");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(5, nodes.length);

            assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
            assertAnyTrue(nodes, t -> "mithocondria".equals(t.getName()));
            assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
            assertAnyTrue(nodes, t -> "angles".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            assertAnyTrue(nodes, t -> t.getPath().get().contains("subject"));
            assertAllTrue(nodes, t -> isValidId(t.getId()));

            assertAllTrue(nodes, t -> t.getMetadata() != null);
            assertAllTrue(nodes, t -> t.getMetadata().isVisible());
            assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().isEmpty());
        }
        {
            var response = testUtils.getResource(
                    "/v1/nodes/" + subject.getPublicId() + "/nodes?recursive=true&nodeType=TOPIC");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(3, nodes.length);

            assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
            assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            assertAnyTrue(nodes, t -> t.getPath().get().contains("subject"));
            assertAllTrue(nodes, t -> isValidId(t.getId()));

            assertAllTrue(nodes, t -> t.getMetadata() != null);
            assertAllTrue(nodes, t -> t.getMetadata().isVisible());
            assertAllTrue(nodes, t -> t.getMetadata().getGrepCodes().isEmpty());
        }
    }

    @Test
    public void can_filter_nodes() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Basic science").child(NodeType.TOPIC, t -> t.name("photo synthesis")));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("Maths")
                .isVisible(false)
                .child(NodeType.TOPIC, t -> t.name("trigonometry")));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Arts and crafts"));
        builder.node(
                NodeType.NODE, n -> n.isContext(true).name("Random node").child(NodeType.NODE, c -> c.name("Subnode")
                        .contentUri("urn:article:1")
                        .isVisible(false)));

        {
            var response = testUtils.getResource("/v1/nodes?contentURI=urn:article:1");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(1, nodes.length);
            assertAnyTrue(nodes, t -> "Subnode".equals(t.getName()));
            assertAnyTrue(nodes, t -> t.getPath().get().contains("node"));
            assertAllTrue(nodes, t -> isValidId(t.getId()));
        }
        {
            var response = testUtils.getResource("/v1/nodes?nodeType=TOPIC,NODE");
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(4, nodes.length);
            assertAnyTrue(nodes, t -> "photo synthesis".equals(t.getName()));
            assertAnyTrue(nodes, t -> "trigonometry".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Random node".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Subnode".equals(t.getName()));
        }
    }

    @Test
    public void can_filter_nodes_on_ids() throws Exception {
        var node1 = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("Basic science")
                .publicId("urn:subject:1")
                .child(NodeType.TOPIC, t -> t.name("photo synthesis")));
        var node2 = builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("Maths")
                .isVisible(false)
                .child(NodeType.TOPIC, t -> t.name("trigonometry")));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true).name("Arts and crafts"));
        builder.node(
                NodeType.NODE, n -> n.isContext(true).name("Random node").child(NodeType.NODE, c -> c.name("Subnode")
                        .contentUri("urn:article:1")
                        .isVisible(false)));

        {
            var response = testUtils.getResource("/v1/nodes?ids=" + node1.getPublicId() + "," + node2.getPublicId());
            final var nodes = testUtils.getObject(NodeDTO[].class, response);
            assertEquals(2, nodes.length);
            assertAnyTrue(nodes, t -> "Basic science".equals(t.getName()));
            assertAnyTrue(nodes, t -> "Maths".equals(t.getName()));
        }
    }

    @Test
    void fetching_subnodes_uses_correct_context() throws Exception {
        Node resource = builder.node(NodeType.RESOURCE, r -> r.name("Leaf").publicId("urn:resource:1"));
        builder.node(NodeType.SUBJECT, s1 -> s1.isContext(true)
                .name("Subject1")
                .publicId("urn:subject:1")
                .child(
                        NodeType.TOPIC,
                        t1 -> t1.name("Topic1").publicId("urn:topic:1").resource(resource)));
        builder.node(NodeType.SUBJECT, s2 -> s2.isContext(true)
                .name("Subject2")
                .publicId("urn:subject:2")
                .child(NodeType.TOPIC, t2 -> t2.name("Topic2")
                        .publicId("urn:topic:2")
                        .isContext(true)
                        .child(
                                NodeType.TOPIC,
                                t3 -> t3.name("Topic3").publicId("urn:topic:3").resource(resource))));

        {
            var response =
                    testUtils.getResource("/v1/nodes/urn:subject:1/nodes?nodeType=TOPIC,RESOURCE&recursive=true");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(2, nodes.length);
            assertAllTrue(nodes, t -> t.getPath().get().contains("/subject:1/topic:1"));
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes?nodeType=RESOURCE");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("Leaf", nodes[0].getName());
            assertEquals(3, nodes[0].getBreadcrumbs().size());
            assertEquals("/subject:1/topic:1/resource:1", nodes[0].getPath().get());
        }
        {
            var response =
                    testUtils.getResource("/v1/nodes/urn:subject:2/nodes?nodeType=TOPIC,RESOURCE&recursive=true");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(3, nodes.length);
            assertAllTrue(nodes, t -> t.getPath().get().contains("/subject:2/topic:2"));
        }
        {
            // Topic2 is context
            var response = testUtils.getResource("/v1/nodes/urn:topic:2/nodes?nodeType=TOPIC,RESOURCE&recursive=true");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(2, nodes.length);
            assertAllTrue(nodes, t -> t.getPath().get().contains("/topic:2/topic:3"));
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:topic:3/nodes?nodeType=RESOURCE");
            final var nodes = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(1, nodes.length);
            assertEquals("Leaf", nodes[0].getName());
            // Can't predict which of the two contexts with topic:3 that is picked
            assertAllTrue(nodes, t -> t.getPaths().contains("/topic:2/topic:3/resource:1"));
        }
    }

    @Test
    public void can_place_subject_below_subject() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Maths").publicId("urn:subject:1").child(NodeType.SUBJECT, t -> t.name(
                                "Maths vg1")
                        .contentUri("urn:frontpage:1")
                        .publicId("urn:subject:2")));

        var response = testUtils.getResource("/v1/nodes/urn:subject:2");
        final var node = testUtils.getObject(NodeDTO.class, response);

        assertEquals("Maths vg1", node.getName());
        assertEquals("Optional[urn:frontpage:1]", node.getContentUri().toString());
        assertEquals("/subject:1/subject:2", node.getPath().get());

        assertNotNull(node.getMetadata());
        assertTrue(node.getMetadata().isVisible());
        assertEquals(0, node.getMetadata().getGrepCodes().size());
    }

    @Test
    public void create_link_to_another_node() throws Exception {
        Node electricity =
                builder.node(NodeType.TOPIC, s -> s.name("Electricity").publicId("urn:topic:1"));
        Node chemistry = builder.node(NodeType.TOPIC, s -> s.name("Chemistry").publicId("urn:topic:2"));
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Physics").publicId("urn:subject:1").child(electricity));
        builder.node(NodeType.SUBJECT, s -> s.isContext(true)
                .name("Biology")
                .publicId("urn:subject:2")
                .child(chemistry)
                .link(electricity));

        {
            var response = testUtils.getResource("/v1/nodes/urn:subject:1/nodes");
            final var children = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(1, children.length);
            assertEquals(URI.create("urn:topic:1"), children[0].getId());
            assertEquals(Optional.of("/subject:1/topic:1"), children[0].getPath());
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:subject:2/nodes");
            final var children = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(1, children.length);
            assertEquals(URI.create("urn:topic:2"), children[0].getId());
            assertEquals(Optional.of("/subject:2/topic:2"), children[0].getPath());
        }
        {
            var response =
                    testUtils.getResource("/v1/nodes/urn:subject:2/nodes?connectionTypes=" + NodeConnectionType.LINK);
            final var links = testUtils.getObject(NodeChildDTO[].class, response);
            assertEquals(1, links.length);
            assertEquals(URI.create("urn:topic:1"), links[0].getId());
            assertEquals(Optional.of("/subject:1/topic:1"), links[0].getPath()); // Links points to default path
            assertEquals(1, links[0].getContexts().size());
        }
    }

    /**
     * This test creates a structure of subjects and topics as follows:
     *
     * <pre>
     *   S:1
     *    \
     *     T:1
     *      \
     *       T:2
     *      /  \
     *    T:3   T:4
     * </pre>
     * <p>
     * S:1 = urn:subject:1000 S:2 = urn:subject:2000 T:1 = urn:topic:1000 T:2 = urn:topic:2000 T:3 = urn:topic:3000 T:4
     * = urn:topic:4000
     * <p>
     * The test examines the T:2 node and verifies that it reports the correct parent-subject, parent-topic and subtopic
     * connections. As shown in the figure above, it should have 1 parent-subject (S:2), 1 parent-topic (T:1), and 2
     * subtopics (T:3 and T:4).
     */
    @Test
    public void can_get_all_connections() throws Exception {
        testSeeder.topicNodeConnectionsTestSetup();

        var response = testUtils.getResource("/v1/nodes/urn:topic:2000/connections");
        final var connections = testUtils.getObject(ConnectionDTO[].class, response);

        assertEquals(3, connections.length, "Correct number of connections");

        connectionsHaveCorrectTypes(connections);
    }

    private void connectionsHaveCorrectTypes(ConnectionDTO[] connections) {
        ConnectionTypeCounter connectionTypeCounter = new ConnectionTypeCounter(connections).countTypes();
        assertEquals(0, connectionTypeCounter.getSubjectCount());
        assertEquals(1, connectionTypeCounter.getParentCount());
        assertEquals(2, connectionTypeCounter.getChildCount());
    }

    @Test
    public void subnodes_are_sorted_by_rank() throws Exception {
        testSeeder.subtopicsByNodeIdAndRelevanceTestSetup();

        var response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes");
        final var subtopics = testUtils.getObject(NodeChildDTO[].class, response);
        assertEquals(7, subtopics.length);

        assertEquals("urn:topic:2", subtopics[0].getId().toString());
        assertEquals("urn:topic:3", subtopics[1].getId().toString());
        assertEquals("urn:topic:4", subtopics[2].getId().toString());
        assertEquals("urn:topic:5", subtopics[3].getId().toString());
        assertEquals("urn:topic:6", subtopics[4].getId().toString());
        assertEquals("urn:topic:7", subtopics[5].getId().toString());
    }

    @Test
    public void can_get_unfiltered_subnodes() throws Exception {
        testSeeder.subtopicsByNodeIdAndRelevanceTestSetup();

        var response = testUtils.getResource("/v1/nodes/urn:topic:1/nodes");
        final var subtopics = testUtils.getObject(NodeChildDTO[].class, response);
        assertEquals(7, subtopics.length, "Unfiltered subtopics");

        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata() != null);
        assertAllTrue(subtopics, subtopic -> subtopic.getMetadata().isVisible());
        assertAllTrue(
                subtopics, subtopic -> subtopic.getMetadata().getGrepCodes().isEmpty());
    }

    @Test
    public void can_create_node() throws Exception {
        final var createNodeCommand = new NodePostPut() {
            {
                nodeType = NodeType.NODE;
                name = Optional.of("node");
                contentUri = Optional.of(URI.create("urn:article:1"));
                context = Optional.of(Boolean.TRUE);
            }
        };

        var response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(NodeType.NODE, node.getNodeType());
        assertEquals("node", node.getName());
        assertEquals(URI.create("urn:article:1"), node.getContentUri());
        assertTrue(node.isContext());
    }

    @Test
    public void can_create_invisible_node() throws Exception {
        final var createNodeCommand = new NodePostPut() {
            {
                nodeType = NodeType.NODE;
                name = Optional.of("node");
                contentUri = Optional.of(URI.create("urn:article:1"));
                context = Optional.of(Boolean.TRUE);
                visible = Optional.of(Boolean.FALSE);
            }
        };

        var response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(NodeType.NODE, node.getNodeType());
        assertEquals("node", node.getName());
        assertEquals(URI.create("urn:article:1"), node.getContentUri());
        assertTrue(node.isContext());
        assertFalse(node.isVisible());
    }

    @Test
    public void can_create_topic() throws Exception {
        final var createNodeCommand = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                name = Optional.of("trigonometry");
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };

        var response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(NodeType.TOPIC, node.getNodeType());
        assertEquals("trigonometry", node.getName());
        assertEquals(URI.create("urn:article:1"), node.getContentUri());
    }

    @Test
    public void can_create_subject() throws Exception {
        final var createNodeCommand = new NodePostPut() {
            {
                nodeType = NodeType.SUBJECT;
                name = Optional.of("Maths");
                contentUri = Optional.of(URI.create("urn:frontpage:1"));
            }
        };

        var response = testUtils.createResource("/v1/nodes", createNodeCommand);
        URI id = getId(response);

        Node node = nodeRepository.getByPublicId(id);
        assertEquals(NodeType.SUBJECT, node.getNodeType());
        assertEquals("Maths", node.getName());
        assertEquals(URI.create("urn:frontpage:1"), node.getContentUri());
    }

    @Test
    public void can_create_topic_with_id() throws Exception {
        final var createNodeCommand = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                nodeId = Optional.of("1");
                name = Optional.of("trigonometry");
            }
        };

        testUtils.createResource("/v1/nodes", createNodeCommand);

        Node node = nodeRepository.getByPublicId(createNodeCommand.getPublicId());
        assertEquals("trigonometry", node.getName());
    }

    @Test
    void creating_node_with_content_uri_is_handled_correct() throws Exception {
        final var createTopic = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                name = Optional.of("topic");
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };
        final var createResource = new NodePostPut() {
            {
                nodeType = NodeType.RESOURCE;
                name = Optional.of("resource");
                contentUri = Optional.of(URI.create("urn:article:2"));
            }
        };

        testUtils.createResource("/v1/nodes", createTopic, status().isCreated());
        testUtils.createResource("/v1/nodes", createTopic, status().isCreated());
        testUtils.createResource("/v1/nodes", createResource, status().isCreated());
        testUtils.createResource("/v1/nodes", createResource, status().isConflict());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                nodeId = Optional.of("1");
                name = Optional.of("name");
            }
        };

        testUtils.createResource("/v1/nodes", command, status().isCreated());
        testUtils.createResource("/v1/nodes", command, status().isConflict());
    }

    @Test
    public void can_update_node() throws Exception {
        Node n = builder.node();

        testUtils.updateResource("/v1/nodes/" + n.getPublicId(), new NodePostPut() {
            {
                nodeType = n.getNodeType();
                nodeId = Optional.of(n.getIdent());
                name = Optional.of("trigonometry");
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        });

        Node node = nodeRepository.getByPublicId(n.getPublicId());
        assertEquals("trigonometry", node.getName());
        assertEquals("urn:article:1", node.getContentUri().toString());
    }

    @Test
    public void can_update_node_with_new_id() throws Exception {
        URI publicId = builder.node(NodeType.TOPIC).getPublicId();
        URI randomId = URI.create("urn:topic:random");

        testUtils.updateResource("/v1/nodes/" + publicId, new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                nodeId = Optional.of("random");
                name = Optional.of("trigonometry");
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        });

        Node node = nodeRepository.getByPublicId(randomId);
        assertEquals("trigonometry", node.getName());
        assertEquals("urn:article:1", node.getContentUri().toString());
    }

    @Test
    public void can_update_node_with_new_type() throws Exception {
        Node n = builder.node(); // NODE
        String ident = n.getIdent();

        var command = new NodePostPut() {
            {
                nodeType = NodeType.SUBJECT;
                nodeId = Optional.of(ident);
                name = Optional.of("trigonometry");
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };

        testUtils.updateResource("/v1/nodes/" + n.getPublicId(), command);

        Node node = nodeRepository.getByPublicId(command.getPublicId());
        assertEquals(
                NodeType.SUBJECT.getName() + ":" + ident, node.getPublicId().getSchemeSpecificPart());
        assertEquals("trigonometry", node.getName());
        assertEquals("urn:article:1", node.getContentUri().toString());
    }

    @Test
    public void can_update_node_without_changing_metadata() throws Exception {
        Node n = builder.node(s -> s.isVisible(false).grepCode("KM123").customField("key", "value"));

        final var command = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                name = Optional.of("physics");
                contentUri = Optional.of(URI.create("urn:article:1"));
            }
        };

        testUtils.updateResource("/v1/nodes/" + n.getPublicId(), command);

        Node node = nodeRepository.getByPublicId(n.getPublicId());
        assertEquals(NodeType.TOPIC, node.getNodeType());
        assertEquals("physics", node.getName());
        assertEquals(URI.create("urn:article:1"), node.getContentUri());

        assertFalse(node.getMetadata().isVisible());
        assertTrue(node.getMetadata().getGrepCodes().stream()
                .map(JsonGrepCode::getCode)
                .collect(Collectors.toSet())
                .contains("KM123"));
        assertTrue(node.getCustomFields().containsValue("value"));
    }

    @Test
    public void can_delete_node_with_2_subnodes() throws Exception {
        Node childTopic1 = builder.node(NodeType.TOPIC, child -> child.name("DELETE EDGE TO ME"));
        Node childTopic2 = builder.node(NodeType.TOPIC, child -> child.name("DELETE EDGE TO ME ALSO"));

        URI parentId = builder.node(
                        NodeType.TOPIC, parent -> parent.child(childTopic1).child(childTopic2))
                .getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
    }

    @Test
    public void can_delete_node_with_2_resources() throws Exception {
        Node topic = builder.node(NodeType.TOPIC, child -> child.name("MAIN TOPIC")
                .translation("nb", tr -> tr.name("HovedEmne"))
                .resource(r -> r.publicId("urn:resource:1"))
                .resource(r -> r.publicId("urn:resource:2")));

        final var topicId = topic.getPublicId();

        testUtils.deleteResource("/v1/nodes/" + topicId);

        assertNull(nodeRepository.findByPublicId(topicId));
    }

    @Test
    public void can_delete_node_but_subnodes_remain() throws Exception {
        Node childTopic = builder.node(NodeType.TOPIC, child -> child.name("DELETE EDGE TO ME")
                .translation("nb", tr -> tr.name("emne"))
                .child(NodeType.TOPIC, sub -> sub.publicId("urn:topic:1"))
                .resource(r -> r.publicId("urn:resource:1")));

        URI parentId =
                builder.node(NodeType.TOPIC, parent -> parent.child(childTopic)).getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
        assertNotNull(nodeRepository.findByPublicId(childTopic.getPublicId()));
    }

    @Test
    public void can_delete_nodes_but_resources_remain() throws Exception {
        var x = builder.resourceType("rt", rt -> rt.name("Learning path"));
        Node resource = builder.node(NodeType.RESOURCE, r -> r.translation("nb", tr -> tr.name("ressurs")));

        var hallo = resource.addResourceType(x);
        entityManager.persist(hallo);

        URI parentId = builder.node(NodeType.TOPIC, parent -> parent.resource(resource))
                .getPublicId();

        testUtils.deleteResource("/v1/nodes/" + parentId);

        assertNull(nodeRepository.findByPublicId(parentId));
        assertNotNull(nodeRepository.findByPublicId(resource.getPublicId()));
    }

    @Test
    void publishing_node_fails_if_no_target_version() throws Exception {
        Node node = builder.node();
        var response = testUtils.updateResource(
                "/v1/nodes/" + node.getPublicId() + "/publish", null, status().is4xxClientError());
        assertEquals(400, response.getStatus());
    }

    @Test
    void publishing_node_fails_if_node_not_found() throws Exception {
        var response = testUtils.updateResource("/v1/nodes/urn:node:random/publish", null, status().is4xxClientError());
        assertEquals(400, response.getStatus());
    }

    @Test
    void making_resources_primary_sets_other_contexts_not_primary() throws Exception {
        var resource = builder.node(NodeType.RESOURCE);
        var resource2 = builder.node(NodeType.RESOURCE);
        var node1 = builder.node(NodeType.TOPIC, n -> n.resource(resource, false)
                .child(NodeType.TOPIC, c -> c.publicId("urn:topic:1").resource(resource2, false)));
        var node2 = builder.node(NodeType.TOPIC, n -> n.resource(resource, true)
                .child(NodeType.TOPIC, c -> c.publicId("urn:topic:2").resource(resource2, true)));

        {
            // Only set primary on one level
            var response = testUtils.updateResource(
                    "/v1/nodes/" + node1.getPublicId() + "/makeResourcesPrimary", null, status().isOk());
            assertEquals(200, response.getStatus());
            var updated1 = nodeRepository.getByPublicId(node1.getPublicId());
            assertTrue(updated1.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            var updated2 = nodeRepository.getByPublicId(node2.getPublicId());
            assertFalse(updated2.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            // Following should be unchanged
            var updated3 = nodeRepository.getByPublicId(URI.create("urn:topic:1"));
            assertFalse(updated3.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            var updated4 = nodeRepository.getByPublicId(URI.create("urn:topic:2"));
            assertTrue(updated4.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
        }
        {
            // Recursive flag updates all levels
            var response = testUtils.updateResource(
                    "/v1/nodes/" + node1.getPublicId() + "/makeResourcesPrimary?recursive=true", null, status().isOk());
            assertEquals(200, response.getStatus());
            var updated1 = nodeRepository.getByPublicId(node1.getPublicId());
            assertTrue(updated1.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            var updated2 = nodeRepository.getByPublicId(node2.getPublicId());
            assertFalse(updated2.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            // Switched order from previous block
            var updated3 = nodeRepository.getByPublicId(URI.create("urn:topic:1"));
            assertTrue(updated3.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
            var updated4 = nodeRepository.getByPublicId(URI.create("urn:topic:2"));
            assertFalse(updated4.getResourceChildren().stream()
                    .allMatch(nr -> nr.isPrimary().orElse(false)));
        }
    }

    @Test
    public void full_node_has_parents() throws Exception {
        testSeeder.resourceInDualSubjectsTestSetup();
        {
            var response = testUtils.getResource("/v1/nodes/urn:resource:1/full");
            final var result = testUtils.getObject(NodeWithParents.class, response);
            assertNotNull(result.getPaths());
            assertEquals(2, result.getParents().size());
            assertEquals(0, result.getContexts().size());
        }
        {
            var response = testUtils.getResource("/v1/nodes/urn:resource:1/full?includeContexts=true");
            final var result = testUtils.getObject(NodeWithParents.class, response);
            assertNotNull(result.getPaths());
            assertEquals(2, result.getParents().size());
            assertEquals(2, result.getContexts().size());
        }
    }

    @Test
    public void can_clone_node() throws Exception {
        URI publicId = builder.node(NodeType.RESOURCE, r -> r.name("Resource")
                        .resourceType(rt -> rt.name("Fagstoff"))
                        .translation("nb", tr -> tr.name("Fagstoff nb"))
                        .contentUri("urn:article:1"))
                .getPublicId();

        final var command = new NodePostPut() {
            {
                contentUri = Optional.of(URI.create("urn:article:2"));
            }
        };
        URI id = getId(testUtils.createResource("/v1/nodes/" + publicId + "/clone", command));
        assertNotNull(id);

        var oldRes = nodeRepository.getByPublicId(publicId);
        var newRes = nodeRepository.getByPublicId(id);
        assertNotEquals(publicId, id);
        assertEquals(oldRes.getName(), newRes.getName());
        assertEquals("urn:article:1", oldRes.getContentUri().toString());
        assertEquals("urn:article:2", newRes.getContentUri().toString());
        assertNotEquals(
                oldRes.getContentUri().toString(), newRes.getContentUri().toString());
        assertEquals(oldRes.getTranslations().size(), newRes.getTranslations().size());
        assertEquals(oldRes.getResourceTypes().size(), newRes.getResourceTypes().size());

        // contentUri can be null
        URI id2 = getId(testUtils.createResource("/v1/nodes/" + publicId + "/clone", new NodePostPut()));
        assertNotNull(id2);
        var resWithoutContentUri = nodeRepository.findByPublicId(id2);
        assertNull(resWithoutContentUri.getContentUri());
    }

    @Test
    public void can_update_delete_and_leave_quality_evaluation() throws Exception {
        var node = builder.node(NodeType.TOPIC);
        var publicId = node.getPublicId();

        testUtils.updateResourceRawInput(
                "/v1/nodes/" + publicId,
                "{\"nodeType\":\"TOPIC\",\"name\":\"some-name\",\"contentUri\":\"urn:article:1\",\"qualityEvaluation\": {\"grade\": 5}}");
        var found = nodeRepository.getByPublicId(publicId);
        assertEquals(found.getQualityEvaluationGrade(), Optional.of(Grade.Five));

        testUtils.updateResourceRawInput(
                "/v1/nodes/" + publicId,
                "{\"nodeType\":\"TOPIC\",\"name\":\"some-name\",\"contentUri\":\"urn:article:1\"}");
        var found2 = nodeRepository.getByPublicId(publicId);
        assertEquals(found2.getQualityEvaluationGrade(), Optional.of(Grade.Five));

        testUtils.updateResourceRawInput(
                "/v1/nodes/" + publicId,
                "{\"nodeType\":\"TOPIC\",\"name\":\"some-name\",\"contentUri\":\"urn:article:1\",\"qualityEvaluation\": null}");
        var found3 = nodeRepository.getByPublicId(publicId);
        assertEquals(found3.getQualityEvaluationGrade(), Optional.empty());
    }

    @Test
    public void update_and_deletion_of_quality_evaluation_does_not_break_calculation() throws Exception {
        var subjectId = "urn:subject:1";
        var topicId = "urn:topic:1";
        var resourceId = "urn:resource:1";
        builder.node(NodeType.SUBJECT, n -> n.name("S1").publicId(subjectId).child(NodeType.TOPIC, c -> c.name("T1")
                .publicId(topicId)
                .child(NodeType.RESOURCE, rc -> rc.name("R1").publicId(resourceId))));

        { // Set quality evaluation on resource and check that average on subject & topic is updated
            testUtils.updateResourceRawInput("/v1/nodes/" + resourceId, "{\"qualityEvaluation\": {\"grade\": 5}}");
            var dbResource = nodeRepository.getByPublicId(URI.create(resourceId));
            var dbTopic = nodeRepository.getByPublicId(URI.create(topicId));
            var dbSuject = nodeRepository.getByPublicId(URI.create(subjectId));
            assertEquals(dbResource.getQualityEvaluationGrade(), Optional.of(Grade.Five));
            var expectedFive = Optional.of(new GradeAverage(5, 1));
            assertEquals(dbSuject.getChildQualityEvaluationAverage(), expectedFive);
            assertEquals(dbTopic.getChildQualityEvaluationAverage(), expectedFive);
        }

        { // Remove quality evaluation on resource and check that average on subject & topic is removed
            testUtils.updateResourceRawInput("/v1/nodes/" + resourceId, "{\"qualityEvaluation\": null}");
            var dbResource = nodeRepository.getByPublicId(URI.create(resourceId));
            var dbTopic = nodeRepository.getByPublicId(URI.create(topicId));
            var dbSuject = nodeRepository.getByPublicId(URI.create(subjectId));
            assertEquals(dbResource.getQualityEvaluationGrade(), Optional.empty());
            var actualSubject = dbSuject.getChildQualityEvaluationAverage();
            assertEquals(actualSubject, Optional.empty());
            var actualTopic = dbTopic.getChildQualityEvaluationAverage();
            assertEquals(actualTopic, Optional.empty());
        }
    }

    @Test
    public void deleting_resource_updates_parent_quality_evaluation_average() throws Exception {
        var subjectId = "urn:subject:1";
        var topicId = "urn:topic:1";
        var resourceId = "urn:resource:1";
        builder.node(NodeType.SUBJECT, n -> n.name("S1").publicId(subjectId).child(NodeType.TOPIC, c -> c.name("T1")
                .publicId(topicId)
                .child(NodeType.RESOURCE, rc -> rc.name("R1").publicId(resourceId))));

        testUtils.updateResourceRawInput("/v1/nodes/" + resourceId, "{\"qualityEvaluation\": {\"grade\": 2}}");
        var dbTopic = nodeRepository.getByPublicId(URI.create(topicId));
        var dbSubject = nodeRepository.getByPublicId(URI.create(subjectId));
        assertTrue(dbTopic.getChildQualityEvaluationAverage().isPresent());
        assertTrue(dbSubject.getChildQualityEvaluationAverage().isPresent());

        testUtils.deleteResource("/v1/nodes/" + resourceId);

        var dbTopicAfter = nodeRepository.getByPublicId(URI.create(topicId));
        var dbSubjectAfter = nodeRepository.getByPublicId(URI.create(subjectId));
        assertTrue(dbTopicAfter.getChildQualityEvaluationAverage().isEmpty());
        assertTrue(dbSubjectAfter.getChildQualityEvaluationAverage().isEmpty());
    }

    @Test
    public void update_and_clone_topic_resource_and_quality_evaluation_matches() throws Exception {
        // Create structure with resources and quality evaluation
        var parent = builder.node(NodeType.SUBJECT, s1 -> s1.name("S1")
                .publicId("urn:subject:1")
                .child(NodeType.TOPIC, t1 -> t1.name("T1")
                        .publicId("urn:topic:1")
                        .child(
                                NodeType.RESOURCE,
                                r1 -> r1.name("R1").publicId("urn:resource:1").qualityEvaluation(Grade.Five))
                        .child(
                                NodeType.RESOURCE,
                                r2 -> r2.name("R2").publicId("urn:resource:2").qualityEvaluation(Grade.Four))
                        .child(
                                NodeType.RESOURCE,
                                r3 -> r3.name("R3").publicId("urn:resource:3").qualityEvaluation(Grade.One)))
                .child(NodeType.TOPIC, t2 -> t2.name("T2").publicId("urn:topic:2")));

        parent.updateEntireAverageTree();

        // Clone resources
        var cloned1Id = getId(testUtils.createResource("/v1/nodes/urn:resource:1/clone", new NodePostPut()));
        var cloned2Id = getId(testUtils.createResource("/v1/nodes/urn:resource:2/clone", new NodePostPut()));
        var cloned3Id = getId(testUtils.createResource("/v1/nodes/urn:resource:3/clone", new NodePostPut()));

        // Check that resources have cloned quality evaluation
        var cloned1 = nodeRepository.getByPublicId(cloned1Id);
        var cloned2 = nodeRepository.getByPublicId(cloned2Id);
        var cloned3 = nodeRepository.getByPublicId(cloned3Id);
        assertEquals(Optional.of(Grade.Five), cloned1.getQualityEvaluationGrade());
        assertEquals(Optional.of(Grade.Four), cloned2.getQualityEvaluationGrade());
        assertEquals(Optional.of(Grade.One), cloned3.getQualityEvaluationGrade());

        { // Connect cloned resources to urn:topic:2
            var connectBody1 = new NodeResourcePOST();
            connectBody1.nodeId = URI.create("urn:topic:2");
            connectBody1.resourceId = cloned1Id;

            var connectBody2 = new NodeResourcePOST();
            connectBody2.nodeId = URI.create("urn:topic:2");
            connectBody2.resourceId = cloned2Id;

            var connectBody3 = new NodeResourcePOST();
            connectBody3.nodeId = URI.create("urn:topic:2");
            connectBody3.resourceId = cloned3Id;

            testUtils.createResource("/v1/node-resources/", connectBody1);
            testUtils.createResource("/v1/node-resources/", connectBody2);
            testUtils.createResource("/v1/node-resources/", connectBody3);
        }

        var topic2 = nodeRepository.getByPublicId(URI.create("urn:topic:2"));
        assertEquals(3, topic2.getChildNodes().size());
        assertTrue(topic2.getChildQualityEvaluationAverage().isPresent());
        assertEquals(3, topic2.getChildQualityEvaluationAverage().get().getCount());
        assertEquals(
                3.3333333333333335,
                topic2.getChildQualityEvaluationAverage().get().getAverageValue());

        var subject1 = nodeRepository.getByPublicId(URI.create("urn:subject:1"));
        assertTrue(subject1.getChildQualityEvaluationAverage().isPresent());
        assertEquals(6, subject1.getChildQualityEvaluationAverage().get().getCount());
        assertEquals(
                3.3333333333333335,
                subject1.getChildQualityEvaluationAverage().get().getAverageValue());
    }

    @Test
    public void moving_a_topic_also_moves_the_quality_evaluation_calculation() throws Exception {
        // Create structure with resources and quality evaluation
        var s1Node = builder.node(
                NodeType.SUBJECT,
                s1 -> s1.name("S1").publicId("urn:subject:1").child(NodeType.TOPIC, t1 -> t1.name("T1")
                        .publicId("urn:topic:1")
                        .child(
                                NodeType.RESOURCE,
                                r1 -> r1.name("R1").publicId("urn:resource:1").qualityEvaluation(Grade.Five))
                        .child(
                                NodeType.RESOURCE,
                                r2 -> r2.name("R2").publicId("urn:resource:2").qualityEvaluation(Grade.Four))
                        .child(
                                NodeType.RESOURCE,
                                r3 -> r3.name("R3").publicId("urn:resource:3").qualityEvaluation(Grade.One))));

        builder.node(NodeType.SUBJECT, s2 -> s2.name("S2").publicId("urn:subject:2"));

        s1Node.updateEntireAverageTree();
        var topic1 = nodeRepository.getByPublicId(URI.create("urn:topic:1"));
        assertEquals(3, topic1.getChildNodes().size());
        topic1.updateEntireAverageTree();

        assertTrue(topic1.getChildQualityEvaluationAverage().isPresent());
        assertEquals(3, topic1.getChildQualityEvaluationAverage().get().getCount());
        assertEquals(
                3.3333333333333335,
                topic1.getChildQualityEvaluationAverage().get().getAverageValue());

        var subject1 = nodeRepository.getByPublicId(URI.create("urn:subject:1"));
        assertTrue(subject1.getChildQualityEvaluationAverage().isPresent());
        assertEquals(3, subject1.getChildQualityEvaluationAverage().get().getCount());
        assertEquals(
                3.3333333333333335,
                subject1.getChildQualityEvaluationAverage().get().getAverageValue());

        var subject2 = nodeRepository.getByPublicId(URI.create("urn:subject:2"));
        assertFalse(subject2.getChildQualityEvaluationAverage().isPresent());

        var topic = nodeRepository.getByPublicId(URI.create("urn:topic:1"));
        assertEquals(1, topic.getParentConnections().size());
        assertTrue(topic.getParentConnections().stream().findFirst().isPresent());
        var connectionId =
                topic.getParentConnections().stream().findFirst().get().getPublicId();

        testUtils.deleteResource("/v1/node-connections/" + connectionId);

        var connectBody = new NodeConnectionPOST();
        connectBody.parentId = URI.create("urn:subject:2");
        connectBody.childId = URI.create("urn:topic:1");

        testUtils.createResource("/v1/node-connections/", connectBody);

        assertEquals(3, topic1.getChildNodes().size());
        assertTrue(topic1.getChildQualityEvaluationAverage().isPresent());
        assertEquals(3, topic1.getChildQualityEvaluationAverage().get().getCount());
        assertEquals(
                3.3333333333333335,
                topic1.getChildQualityEvaluationAverage().get().getAverageValue());

        var s1 = nodeRepository.getByPublicId(URI.create("urn:subject:1"));
        assertFalse(s1.getChildQualityEvaluationAverage().isPresent());

        var s2 = nodeRepository.getByPublicId(URI.create("urn:subject:2"));
        assertTrue(s2.getChildQualityEvaluationAverage().isPresent());
        assertEquals(3, s2.getChildQualityEvaluationAverage().get().getCount());
        assertEquals(
                3.3333333333333335, s2.getChildQualityEvaluationAverage().get().getAverageValue());
    }

    public void connect(Node parent, Node child) throws Exception {
        var connectBody = new NodeConnectionPOST();
        connectBody.parentId = parent.getPublicId();
        connectBody.childId = child.getPublicId();

        testUtils.createResource("/v1/node-connections/", connectBody);
    }

    public void connectId(URI parentId, URI childId) throws Exception {
        var connectBody = new NodeConnectionPOST();
        connectBody.parentId = parentId;
        connectBody.childId = childId;

        testUtils.createResource("/v1/node-connections/", connectBody);
    }

    public void disconnect(Node parent, Node child) throws Exception {
        var connection = nodeConnectionRepository.findByParentIdAndChildId(parent.getId(), child.getId());
        var connectionId = connection.getPublicId();
        testUtils.deleteResource("/v1/node-connections/" + connectionId);
    }

    @Test
    public void that_adding_quality_evaluated_trees_works_recursively() throws Exception {
        var s1 = builder.node(NodeType.SUBJECT, s -> s.name("S1").publicId("urn:subject:1"));

        var t1 = builder.node(
                NodeType.TOPIC, n -> n.name("T1").publicId("urn:topic:1").qualityEvaluation(Grade.Four));
        var t2 = builder.node(NodeType.TOPIC, n -> n.name("T2").qualityEvaluation(Grade.One));
        var t3 = builder.node(NodeType.TOPIC, n -> n.name("T3").qualityEvaluation(Grade.Two));
        var t4 = builder.node(
                NodeType.TOPIC, n -> n.name("T4").publicId("urn:topic:4").qualityEvaluation(Grade.One));

        var r1 = builder.node(NodeType.RESOURCE, n -> n.name("R1").qualityEvaluation(Grade.Five));
        var r2 = builder.node(NodeType.RESOURCE, n -> n.name("R2").qualityEvaluation(Grade.Five));
        var r3 = builder.node(NodeType.RESOURCE, n -> n.name("R3").qualityEvaluation(Grade.Three));
        var r4 = builder.node(NodeType.RESOURCE, n -> n.name("R4").qualityEvaluation(Grade.Four));
        var r5 = builder.node(NodeType.RESOURCE, n -> n.name("R5").qualityEvaluation(Grade.One));
        var r6 = builder.node(NodeType.RESOURCE, n -> n.name("R6").qualityEvaluation(Grade.Five));
        var r7 = builder.node(NodeType.RESOURCE, n -> n.name("R7").qualityEvaluation(Grade.Five));
        var r8 = builder.node(NodeType.RESOURCE, n -> n.name("R8").qualityEvaluation(Grade.Four));
        var r9 = builder.node(NodeType.RESOURCE, n -> n.name("R9").qualityEvaluation(Grade.Four));
        var r10 = builder.node(NodeType.RESOURCE, n -> n.name("R10").qualityEvaluation(Grade.Five));
        var r11 = builder.node(NodeType.RESOURCE, n -> n.name("R11").qualityEvaluation(Grade.Three));
        var r12 = builder.node(NodeType.RESOURCE, n -> n.name("R12").qualityEvaluation(Grade.Two));
        var r13 = builder.node(NodeType.RESOURCE, n -> n.name("R13").qualityEvaluation(Grade.Five));
        var r14 = builder.node(NodeType.RESOURCE, n -> n.name("R14").qualityEvaluation(Grade.One));
        var r15 = builder.node(NodeType.RESOURCE, n -> n.name("R15").qualityEvaluation(Grade.Four));
        var r16 = builder.node(NodeType.RESOURCE, n -> n.name("R16").qualityEvaluation(Grade.Four));

        connect(s1, t1);
        connect(t1, t2);
        connect(t2, t3);

        connect(t1, r1);
        connect(t1, r2);
        connect(t1, r3);
        connect(t1, r4);

        connect(t2, r5);
        connect(t2, r6);
        connect(t2, r7);
        connect(t2, r8);

        connect(t3, r9);
        connect(t3, r10);
        connect(t3, r11);
        connect(t3, r12);

        connect(t4, r13);
        connect(t4, r14);
        connect(t4, r15);
        connect(t4, r16);

        connect(t1, t4);

        testQualityEvaluationAverage(s1, 16, 3.75);
        testQualityEvaluationAverage(t1, 16, 3.75);
        testQualityEvaluationAverage(t2, 8, 3.625);
        testQualityEvaluationAverage(t3, 4, 3.5);
        testQualityEvaluationAverage(t4, 4, 3.5);

        disconnect(t1, t4);

        testQualityEvaluationAverage(s1, 12, 3.8333333333333335);
        testQualityEvaluationAverage(t1, 12, 3.8333333333333335);
    }

    @Test
    public void that_deleting_a_bunch_of_quality_evaluation_works_as_expected() throws Exception {
        var s1 = builder.node(NodeType.SUBJECT, s -> s.name("S1").publicId("urn:subject:1"));

        var t1 = builder.node(
                NodeType.TOPIC, n -> n.name("T1").publicId("urn:topic:1").qualityEvaluation(Grade.Four));
        var t2 = builder.node(NodeType.TOPIC, n -> n.name("T2").qualityEvaluation(Grade.One));

        var r1 = builder.node(NodeType.RESOURCE, n -> n.name("R1").qualityEvaluation(Grade.Five));
        var r2 = builder.node(NodeType.RESOURCE, n -> n.name("R2").qualityEvaluation(Grade.Five));
        var r3 = builder.node(NodeType.RESOURCE, n -> n.name("R3").qualityEvaluation(Grade.Three));
        var r4 = builder.node(NodeType.RESOURCE, n -> n.name("R4").qualityEvaluation(Grade.Four));
        var r5 = builder.node(NodeType.RESOURCE, n -> n.name("R5").qualityEvaluation(Grade.One));
        var r6 = builder.node(NodeType.RESOURCE, n -> n.name("R6").qualityEvaluation(Grade.Five));
        var r7 = builder.node(NodeType.RESOURCE, n -> n.name("R7").qualityEvaluation(Grade.Five));
        var r8 = builder.node(NodeType.RESOURCE, n -> n.name("R8").qualityEvaluation(Grade.Four));

        connect(s1, t1);
        connect(t1, t2);

        connect(t1, r1);
        connect(t1, r2);
        connect(t1, r3);
        connect(t1, r4);

        testQualityEvaluationAverage(t1, 4, 4.25);

        disconnect(t1, t2);

        {
            var node = nodeRepository.findFirstByPublicId(t2.getPublicId());
            var qe = node.get().getChildQualityEvaluationAverage();
            assertFalse(qe.isPresent());
        }

        connect(t2, r5);
        connect(t2, r6);
        connect(t2, r7);
        connect(t2, r8);

        testQualityEvaluationAverage(t2, 4, 3.75);
        testQualityEvaluationAverage(t1, 4, 4.25);

        disconnect(s1, t1);
        connect(t1, t2);

        testQualityEvaluationAverage(t1, 8, 4.0);

        connect(s1, t1);

        testQualityEvaluationAverage(s1, 8, 4.0);
    }

    @Test
    public void that_updating_after_quality_evaluation_works_as_expected() throws Exception {
        var s1 = builder.node(NodeType.SUBJECT, s -> s.name("S1").publicId("urn:subject:1"));
        var t1 = builder.node(
                NodeType.TOPIC, n -> n.name("T1").publicId("urn:topic:1").qualityEvaluation(Grade.Four));
        var r1 = builder.node(NodeType.RESOURCE, n -> n.name("R1").qualityEvaluation(Grade.Three));

        connect(s1, t1);
        connect(t1, r1);

        testQualityEvaluationAverage(s1, 1, 3.0);
        testQualityEvaluationAverage(t1, 1, 3.0);

        var updateBody = new NodePostPut();
        updateBody.name = Optional.of("Resource 1");
        testUtils.updateResource("/v1/nodes/" + r1.getPublicId(), updateBody);

        testQualityEvaluationAverage(s1, 1, 3.0);
        testQualityEvaluationAverage(t1, 1, 3.0);
    }

    public URI createNode(String name, NodeType nodeType, Optional<Integer> qualityEvaluation) {
        var n = new NodePostPut();
        n.name = Optional.of(name);
        n.nodeType = nodeType;
        qualityEvaluation.ifPresent(x -> {
            var comment = Optional.of("La til karakter " + x + " her da...");
            var qe = new QualityEvaluationDTO(Grade.fromInt(x), comment);
            n.qualityEvaluation = UpdateOrDelete.Update(qe);
        });

        try {
            return getId(testUtils.createResource("/v1/nodes", n));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Integer> getQualityEvaluationGradesOf(List<URI> ids) {
        return ids.stream()
                .map(id -> nodeRepository
                        .getByPublicId(id)
                        .getQualityEvaluationGrade()
                        .orElseThrow()
                        .toInt())
                .toList();
    }

    public List<GradeAverage> getQEAverageGradesOf(List<URI> ids) {
        return ids.stream()
                .map(id -> nodeRepository
                        .getByPublicId(id)
                        .getChildQualityEvaluationAverage()
                        .orElse(null))
                .toList();
    }

    @Test
    public void update_super_nested_quality_evaluation() throws Exception {
        // Create subjects
        var s1id = createNode("S1", NodeType.SUBJECT, Optional.of(5));
        var s2id = createNode("S2", NodeType.SUBJECT, Optional.of(4));
        var s3id = createNode("S3", NodeType.SUBJECT, Optional.of(3));

        // Create topics
        var t1id = createNode("S1T1", NodeType.TOPIC, Optional.of(5));
        var t2id = createNode("S1T2", NodeType.TOPIC, Optional.of(5));
        var t3id = createNode("S2T3", NodeType.TOPIC, Optional.of(5));
        var t4id = createNode("S1T1T4", NodeType.TOPIC, Optional.of(2));
        var t5id = createNode("S1T1T5", NodeType.TOPIC, Optional.of(3));
        var t6id = createNode("S1T2T6", NodeType.TOPIC, Optional.of(4));
        var t7id = createNode("S1T4T7", NodeType.TOPIC, Optional.of(3));
        var t8id = createNode("S1T4T8", NodeType.TOPIC, Optional.of(2));
        var t9id = createNode("S1T6T9", NodeType.TOPIC, Optional.of(3));
        var t10id = createNode("S1T5T10", NodeType.TOPIC, Optional.of(2));
        var t11id = createNode("S1T4T7T11", NodeType.TOPIC, Optional.of(5));
        var t12id = createNode("S1T4T7T12", NodeType.TOPIC, Optional.of(4));

        // Hook topics to subjects
        connectId(s1id, t1id);
        connectId(s1id, t2id);
        connectId(s2id, t3id);
        connectId(t1id, t4id);
        connectId(t1id, t5id);
        connectId(t2id, t6id);
        connectId(t4id, t7id);
        connectId(t4id, t8id);
        connectId(t6id, t9id);
        connectId(t5id, t10id);
        connectId(t7id, t11id);
        connectId(t7id, t12id);

        // Create resources
        var r1id = createNode("S1T1R1", NodeType.RESOURCE, Optional.of(5));
        var r2id = createNode("S1T1R2", NodeType.RESOURCE, Optional.of(4));
        var r3id = createNode("S1T2R3", NodeType.RESOURCE, Optional.of(3));
        var r4id = createNode("S1T2T6R4", NodeType.RESOURCE, Optional.of(5));
        var r5id = createNode("S2T3R5", NodeType.RESOURCE, Optional.of(4));
        var r6id = createNode("S1T1T4R6", NodeType.RESOURCE, Optional.of(2));
        var r7id = createNode("S1T6R7", NodeType.RESOURCE, Optional.of(2));
        var r8id = createNode("S1T4T7R8", NodeType.RESOURCE, Optional.of(3));
        var r9id = createNode("S1T4T7R9", NodeType.RESOURCE, Optional.of(1));
        var r10id = createNode("S1T4T8R10", NodeType.RESOURCE, Optional.of(1));
        var r11id = createNode("S1T5T10R11", NodeType.RESOURCE, Optional.of(4));
        var r12id = createNode("S1T5T10R12", NodeType.RESOURCE, Optional.of(2));
        var r13id = createNode("S1T5T10R13", NodeType.RESOURCE, Optional.of(3));
        var r14id = createNode("S1T5T10R14", NodeType.RESOURCE, Optional.of(5));
        var r15id = createNode("S1T4T7T11R15", NodeType.RESOURCE, Optional.of(1));
        var r16id = createNode("S1T4T7T11R16", NodeType.RESOURCE, Optional.of(2));
        var r17id = createNode("S1T4T7T11R17", NodeType.RESOURCE, Optional.of(3));
        var r18id = createNode("S1T4T7T11R18", NodeType.RESOURCE, Optional.of(2));
        var r19id = createNode("S1T4T7T12R19", NodeType.RESOURCE, Optional.of(4));
        var r20id = createNode("S1T4T7T12R20", NodeType.RESOURCE, Optional.of(1));

        // Hook resources to topics
        connectId(t1id, r1id);
        connectId(t1id, r2id);
        connectId(t2id, r3id);
        connectId(t6id, r4id);
        connectId(t3id, r5id);
        connectId(t4id, r6id);
        connectId(t6id, r7id);
        connectId(t7id, r8id);
        connectId(t7id, r9id);
        connectId(t8id, r10id);
        connectId(t10id, r11id);
        connectId(t10id, r12id);
        connectId(t10id, r13id);
        connectId(t10id, r14id);
        connectId(t11id, r15id);
        connectId(t11id, r16id);
        connectId(t11id, r17id);
        connectId(t11id, r18id);
        connectId(t12id, r19id);
        connectId(t12id, r20id);

        var allIds = List.of(
                s1id, s2id, s3id, t1id, t2id, t3id, t4id, t5id, t6id, t7id, t8id, t9id, t10id, t11id, t12id, r1id, r2id,
                r3id, r4id, r5id, r6id, r7id, r8id, r9id, r10id, r11id, r12id, r13id, r14id, r15id, r16id, r17id, r18id,
                r19id, r20id);

        var originalGrades = getQualityEvaluationGradesOf(allIds);
        var originalAvgs = getQEAverageGradesOf(allIds);

        testUtils.createResource("/v1/admin/buildAverageTree");
        nodeRepository.flush();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        var originalAfterRebuild = getQualityEvaluationGradesOf(allIds);
        var originalAvgsAfterRebuild = getQEAverageGradesOf(allIds);
        assertEquals(originalAfterRebuild, originalGrades);
        assertEquals(originalAvgsAfterRebuild, originalAvgs);

        // Update all quality evaluations
        var updateBody = new NodePostPut();
        updateBody.qualityEvaluation = UpdateOrDelete.Update(getRandomGrade());
        for (var id : allIds) {
            testUtils.updateResource("/v1/nodes/" + id, updateBody);
        }

        var updatedGrades = getQualityEvaluationGradesOf(allIds);
        var updatedAvgs = getQEAverageGradesOf(allIds);

        testUtils.createResource("/v1/admin/buildAverageTree");
        nodeRepository.flush();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        var updatedAfterRebuild = getQualityEvaluationGradesOf(allIds);
        var updatedAvgsAfterRebuild = getQEAverageGradesOf(allIds);
        assertEquals(updatedGrades, updatedAfterRebuild);
        assertEquals(updatedAvgs, updatedAvgsAfterRebuild);
    }

    @Test
    public void that_moving_quality_evaluated_trees_works_recursively() throws Exception {
        var s1 = builder.node(NodeType.SUBJECT, s -> s.name("S1").publicId("urn:subject:1"));

        var t1 = builder.node(
                NodeType.TOPIC, n -> n.name("T1").publicId("urn:topic:1").qualityEvaluation(Grade.Four));
        var t2 = builder.node(NodeType.TOPIC, n -> n.name("T2").qualityEvaluation(Grade.One));
        var t3 = builder.node(NodeType.TOPIC, n -> n.name("T3").qualityEvaluation(Grade.Two));
        var t4 = builder.node(
                NodeType.TOPIC, n -> n.name("T4").publicId("urn:topic:4").qualityEvaluation(Grade.One));

        var s2 = builder.node(NodeType.SUBJECT, s -> s.name("S2").publicId("urn:subject:2"));
        var t5 = builder.node(NodeType.TOPIC, n -> n.name("T5").qualityEvaluation(Grade.Two));

        var r1 = builder.node(NodeType.RESOURCE, n -> n.name("R1").qualityEvaluation(Grade.Five));
        var r2 = builder.node(NodeType.RESOURCE, n -> n.name("R2").qualityEvaluation(Grade.Five));
        var r3 = builder.node(NodeType.RESOURCE, n -> n.name("R3").qualityEvaluation(Grade.Three));
        var r4 = builder.node(NodeType.RESOURCE, n -> n.name("R4").qualityEvaluation(Grade.Four));
        var r5 = builder.node(NodeType.RESOURCE, n -> n.name("R5").qualityEvaluation(Grade.One));
        var r6 = builder.node(NodeType.RESOURCE, n -> n.name("R6").qualityEvaluation(Grade.Five));
        var r7 = builder.node(NodeType.RESOURCE, n -> n.name("R7").qualityEvaluation(Grade.Five));
        var r8 = builder.node(NodeType.RESOURCE, n -> n.name("R8").qualityEvaluation(Grade.Four));
        var r9 = builder.node(NodeType.RESOURCE, n -> n.name("R9").qualityEvaluation(Grade.Four));
        var r10 = builder.node(NodeType.RESOURCE, n -> n.name("R10").qualityEvaluation(Grade.Five));
        var r11 = builder.node(NodeType.RESOURCE, n -> n.name("R11").qualityEvaluation(Grade.Three));
        var r12 = builder.node(NodeType.RESOURCE, n -> n.name("R12").qualityEvaluation(Grade.Two));
        var r13 = builder.node(NodeType.RESOURCE, n -> n.name("R13").qualityEvaluation(Grade.Five));
        var r14 = builder.node(NodeType.RESOURCE, n -> n.name("R14").qualityEvaluation(Grade.One));
        var r15 = builder.node(NodeType.RESOURCE, n -> n.name("R15").qualityEvaluation(Grade.Four));
        var r16 = builder.node(NodeType.RESOURCE, n -> n.name("R16").qualityEvaluation(Grade.Four));
        var r17 = builder.node(NodeType.RESOURCE, n -> n.name("R17").qualityEvaluation(Grade.Three));
        var r18 = builder.node(NodeType.RESOURCE, n -> n.name("R18").qualityEvaluation(Grade.Two));

        connect(s1, t1);
        connect(t1, t2);
        connect(t2, t3);

        connect(t1, r1);
        connect(t1, r2);
        connect(t1, r3);
        connect(t1, r4);

        connect(t2, r5);
        connect(t2, r6);
        connect(t2, r7);
        connect(t2, r8);

        connect(t3, r9);
        connect(t3, r10);
        connect(t3, r11);
        connect(t3, r12);

        connect(t4, r13);
        connect(t4, r14);
        connect(t4, r15);
        connect(t4, r16);

        connect(t1, t4);

        connect(t5, r17);
        connect(t5, r18);
        connect(s2, t5);

        testQualityEvaluationAverage(s1, 16, 3.75);
        testQualityEvaluationAverage(t1, 16, 3.75);
        testQualityEvaluationAverage(t2, 8, 3.625);
        testQualityEvaluationAverage(t3, 4, 3.5);
        testQualityEvaluationAverage(t4, 4, 3.5);

        disconnect(t1, t4);

        testQualityEvaluationAverage(t5, 2, 2.5);
        testQualityEvaluationAverage(s2, 2, 2.5);
        connect(t5, t4);

        testQualityEvaluationAverage(s2, 6, 3.1666666666666665);

        disconnect(t5, t4);
        testQualityEvaluationAverage(s2, 2, 2.5);

        connect(t1, t4);
        testQualityEvaluationAverage(s1, 16, 3.75);
        testQualityEvaluationAverage(t1, 16, 3.75);
        testQualityEvaluationAverage(t2, 8, 3.625);
        testQualityEvaluationAverage(t3, 4, 3.5);
        testQualityEvaluationAverage(t4, 4, 3.5);
    }

    @Test
    public void that_reusing_resources_and_updating_them_works_everywhere() throws Exception {
        var s1 = builder.node(NodeType.SUBJECT, s -> s.name("S1").publicId("urn:subject:1"));

        var t1 = builder.node(
                NodeType.TOPIC, n -> n.name("T1").publicId("urn:topic:1").qualityEvaluation(Grade.Four));
        var t2 = builder.node(NodeType.TOPIC, n -> n.name("T2").qualityEvaluation(Grade.One));
        var t3 = builder.node(NodeType.TOPIC, n -> n.name("T3").qualityEvaluation(Grade.Two));
        var t4 = builder.node(
                NodeType.TOPIC, n -> n.name("T4").publicId("urn:topic:4").qualityEvaluation(Grade.One));

        var s2 = builder.node(NodeType.SUBJECT, s -> s.name("S2").publicId("urn:subject:2"));
        var t5 = builder.node(NodeType.TOPIC, n -> n.name("T5").qualityEvaluation(Grade.Two));

        var r1 = builder.node(NodeType.RESOURCE, n -> n.name("R1").qualityEvaluation(Grade.Five));
        var r2 = builder.node(NodeType.RESOURCE, n -> n.name("R2").qualityEvaluation(Grade.Five));
        var r3 = builder.node(NodeType.RESOURCE, n -> n.name("R3").qualityEvaluation(Grade.Three));
        var r4 = builder.node(NodeType.RESOURCE, n -> n.name("R4").qualityEvaluation(Grade.Four));
        var r5 = builder.node(NodeType.RESOURCE, n -> n.name("R5").qualityEvaluation(Grade.One));
        var r6 = builder.node(NodeType.RESOURCE, n -> n.name("R6").qualityEvaluation(Grade.Five));
        var r7 = builder.node(NodeType.RESOURCE, n -> n.name("R7").qualityEvaluation(Grade.Five));
        var r8 = builder.node(NodeType.RESOURCE, n -> n.name("R8").qualityEvaluation(Grade.Four));
        var r9 = builder.node(NodeType.RESOURCE, n -> n.name("R9").qualityEvaluation(Grade.Four));
        var r10 = builder.node(NodeType.RESOURCE, n -> n.name("R10").qualityEvaluation(Grade.Five));
        var r11 = builder.node(NodeType.RESOURCE, n -> n.name("R11").qualityEvaluation(Grade.Three));
        var r12 = builder.node(NodeType.RESOURCE, n -> n.name("R12").qualityEvaluation(Grade.Two));
        var r13 = builder.node(NodeType.RESOURCE, n -> n.name("R13").qualityEvaluation(Grade.Five));
        var r14 = builder.node(NodeType.RESOURCE, n -> n.name("R14").qualityEvaluation(Grade.One));
        var r15 = builder.node(NodeType.RESOURCE, n -> n.name("R15").qualityEvaluation(Grade.Four));
        var r16 = builder.node(NodeType.RESOURCE, n -> n.name("R16").qualityEvaluation(Grade.Four));

        connect(s1, t1);
        connect(t1, t2);
        connect(t2, t3);

        connect(t1, r1);
        connect(t1, r2);
        connect(t1, r3);
        connect(t1, r4);

        connect(t2, r5);
        connect(t2, r6);
        connect(t2, r7);
        connect(t2, r8);

        connect(t3, r9);
        connect(t3, r10);
        connect(t3, r11);
        connect(t3, r12);

        connect(t4, r13);
        connect(t4, r14);
        connect(t4, r15);
        connect(t4, r16);

        connect(t1, t4);

        connect(t5, r9);
        connect(t5, r10);
        connect(t5, r11);
        connect(t5, r12);
        connect(s2, t5);

        testQualityEvaluationAverage(s1, 16, 3.75);
        testQualityEvaluationAverage(t1, 16, 3.75);
        testQualityEvaluationAverage(t2, 8, 3.625);
        testQualityEvaluationAverage(t3, 4, 3.5);
        testQualityEvaluationAverage(t4, 4, 3.5);
        testQualityEvaluationAverage(t5, 4, 3.5);
        testQualityEvaluationAverage(s2, 4, 3.5);

        {
            var updater12 = new NodePostPut();
            var qe12 = new QualityEvaluationDTO(Grade.fromInt(5), Optional.of("Hei"));
            updater12.qualityEvaluation = UpdateOrDelete.Update(qe12);
            testUtils.updateResource("/v1/nodes/" + r12.getPublicId(), updater12);
        }

        testQualityEvaluationAverage(t3, 4, 4.25);
        testQualityEvaluationAverage(t5, 4, 4.25);
        testQualityEvaluationAverage(s2, 4, 4.25);
        testQualityEvaluationAverage(t2, 8, 4.0);
        testQualityEvaluationAverage(t1, 16, 3.9375);
        testQualityEvaluationAverage(s1, 16, 3.9375);
    }

    public QualityEvaluationDTO getRandomGrade() {
        var x = new Random().nextInt(5) + 1;
        return new QualityEvaluationDTO(Grade.fromInt(x), Optional.of("Random grade " + x));
    }

    @Test
    public void requires_technical_evaluation_defaults_to_empty() {
        var node = builder.node(NodeType.TOPIC);
        var id = node.getPublicId();

        var found = nodeRepository.getByPublicId(id);
        assertTrue(found.requiresTechnicalEvaluation().isEmpty());
        assertTrue(found.getTechnicalEvaluationComment().isEmpty());
    }

    @Test
    public void that_updating_requires_technical_evaluation_to_false_removes_comment() throws Exception {
        var command = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                technicalEvaluation =
                        UpdateOrDelete.Update(new TechnicalEvaluationDTO(true, Optional.of("Needs review")));
            }
        };
        var id = getId(testUtils.createResource("/v1/nodes", command));

        var found = nodeRepository.getByPublicId(id);
        assertTrue(found.requiresTechnicalEvaluation().orElseThrow());
        assertEquals("Needs review", found.getTechnicalEvaluationComment().orElseThrow());

        var command2 = new NodePostPut() {
            {
                technicalEvaluation =
                        UpdateOrDelete.Update(new TechnicalEvaluationDTO(false, Optional.of("Needs review")));
            }
        };
        testUtils.updateResource("/v1/nodes/" + id, command2);
        var found2 = nodeRepository.getByPublicId(id);
        assertFalse(found2.requiresTechnicalEvaluation().orElseThrow());
        assertTrue(found2.getTechnicalEvaluationComment().isEmpty());
    }

    @Test
    public void that_technical_evluation_comment_can_be_updated() throws Exception {
        var command = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                technicalEvaluation = UpdateOrDelete.Update(new TechnicalEvaluationDTO(true, Optional.empty()));
            }
        };
        var id = getId(testUtils.createResource("/v1/nodes", command));

        var found = nodeRepository.getByPublicId(id);
        assertTrue(found.requiresTechnicalEvaluation().orElseThrow());
        assertTrue(found.getTechnicalEvaluationComment().isEmpty());

        var command2 = new NodePostPut() {
            {
                technicalEvaluation =
                        UpdateOrDelete.Update(new TechnicalEvaluationDTO(true, Optional.of("Needs review")));
            }
        };
        testUtils.updateResource("/v1/nodes/" + id, command2);

        var found2 = nodeRepository.getByPublicId(id);
        assertTrue(found2.requiresTechnicalEvaluation().orElseThrow());
        assertEquals("Needs review", found2.getTechnicalEvaluationComment().orElseThrow());
    }

    @Test
    public void that_comment_cannot_be_set_without_requiring_technical_evaluation() throws Exception {
        var command = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                technicalEvaluation =
                        UpdateOrDelete.Update(new TechnicalEvaluationDTO(false, Optional.of("Needs review")));
            }
        };
        var id = getId(testUtils.createResource("/v1/nodes", command));

        var found = nodeRepository.getByPublicId(id);
        assertFalse(found.requiresTechnicalEvaluation().orElseThrow());
        assertTrue(found.getTechnicalEvaluationComment().isEmpty());
    }

    @Test
    public void that_technical_evaluation_is_included_in_node_response() throws Exception {
        var command = new NodePostPut() {
            {
                nodeType = NodeType.TOPIC;
                technicalEvaluation =
                        UpdateOrDelete.Update(new TechnicalEvaluationDTO(true, Optional.of("Needs review")));
            }
        };
        var id = getId(testUtils.createResource("/v1/nodes", command));

        var response = testUtils.getResource("/v1/nodes/" + id);

        var nodeDTO = testUtils.getObject(NodeDTO.class, response);
        var te = nodeDTO.getTechnicalEvaluation().orElseThrow();
        assertTrue(te.requiresEvaluation());
        assertEquals("Needs review", te.getComment().orElseThrow());
    }

    public void testQualityEvaluationAverage(Node inputNode, int expectedCount, double expectedAverage) {
        var node = nodeRepository.findFirstByPublicId(inputNode.getPublicId()).orElseThrow();
        var qe = node.getChildQualityEvaluationAverage().orElseThrow();
        assertEquals(expectedCount, qe.getCount());
        assertEquals(expectedAverage, qe.getAverageValue());
    }

    private static class ConnectionTypeCounter {
        private final ConnectionDTO[] connections;
        private int subjectCount;
        private int parentCount;
        private int childCount;

        ConnectionTypeCounter(ConnectionDTO[] connections) {
            this.connections = connections;
        }

        int getSubjectCount() {
            return subjectCount;
        }

        int getParentCount() {
            return parentCount;
        }

        int getChildCount() {
            return childCount;
        }

        ConnectionTypeCounter countTypes() {
            subjectCount = 0;
            parentCount = 0;
            childCount = 0;
            for (ConnectionDTO connection : connections) {
                switch (connection.getType()) {
                    case "parent-subject" -> subjectCount++;
                    case "parent" -> parentCount++;
                    case "child" -> childCount++;
                    default -> fail("Unexpected connection type :" + connection.getType());
                }
            }
            return this;
        }
    }
}
