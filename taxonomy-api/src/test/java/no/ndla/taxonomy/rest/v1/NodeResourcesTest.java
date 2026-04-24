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

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.dtos.NodeResourceDTO;
import no.ndla.taxonomy.rest.v1.dtos.NodeResourcePOST;
import no.ndla.taxonomy.rest.v1.dtos.NodeResourcePUT;
import no.ndla.taxonomy.service.dtos.MetadataDTO;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NodeResourcesTest extends RestTest {

    @BeforeEach
    public void add_core_relevance() {
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
    }

    @Test
    public void can_add_resource_to_node() throws Exception {
        URI integrationId, calculusId;
        calculusId = newTopic().name("calculus").getPublicId();
        var resource = newResource();
        resource.setName("Introduction to integration");
        integrationId = resource.getPublicId();

        URI id = getId(testUtils.createResource("/v1/node-resources", new NodeResourcePOST() {
            {
                nodeId = calculusId;
                resourceId = integrationId;
            }
        }));

        final var connection = nodeConnectionRepository.findByPublicId(id);
        assertTrue(connection.isPrimary().orElseThrow());

        final var calculus = nodeRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getResources().size());
        assertAnyTrue(calculus.getResources(), t -> "Introduction to integration".equals(t.getName()));
        assertNotNull(nodeConnectionRepository.getByPublicId(id));
        assertTrue(calculus.getResourceChildren().iterator().next().isPrimary().orElseThrow());
    }

    @Test
    public void can_add_secondary_resource_to_node() throws Exception {
        final var calculusId = newTopic().name("calculus").getPublicId();
        var resource = newResource();
        resource.setName("Introduction to integration");
        final var integrationId = resource.getPublicId();

        final var node2 = newTopic();
        final var node2Id = node2.getPublicId();

        // 20190819 JEP@Cerpus: Behavior change: It was possible to create a non-primary resource
        // when no resource existed
        // before, but it has changed. The first connection to a resource will ignore the primary
        // parameter and will be
        // forced to become primary (subject-topics already did this)

        final var id = getId(testUtils.createResource("/v1/node-resources", new NodeResourcePOST() {
            {
                nodeId = calculusId;
                resourceId = integrationId;
                primary = Optional.of(false);
            }
        }));

        final var calculus = nodeRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getResources().size());
        assertAnyTrue(calculus.getResources(), t -> "Introduction to integration".equals(t.getName()));
        assertNotNull(nodeConnectionRepository.getByPublicId(id));
        // First topic connection will always be primary
        assertTrue(calculus.getResourceChildren().iterator().next().isPrimary().orElseThrow());

        // After behavior change: Add the resource again to another topic with primary = false
        // should create a non-primary resource connection
        final var resource2ConnectionPublicId =
                getId(testUtils.createResource("/v1/node-resources", new NodeResourcePOST() {
                    {
                        nodeId = node2Id;
                        resourceId = integrationId;
                        primary = Optional.of(false);
                    }
                }));

        final var resource2Connection = nodeConnectionRepository
                .findFirstByPublicId(resource2ConnectionPublicId)
                .orElse(null);

        assertNotNull(resource2Connection);
        assertSame(node2, resource2Connection.getParent().orElse(null));
        assertSame(resource, resource2Connection.getResource().orElse(null));
        assertFalse(resource2Connection.isPrimary().orElseThrow());

        assertEquals(1, node2.getResources().size());
        assertEquals(2, resource.getParentConnections().size());
    }

    @Test
    public void cannot_add_existing_resource_to_node() throws Exception {
        final var calculus = newTopic().name("calculus");
        final var integration = newResource();
        integration.setName("Introduction to integration");
        NodeConnection.create(calculus, integration, Relevance.CORE);

        final var calculusId = calculus.getPublicId();
        final var integrationId = integration.getPublicId();

        testUtils.createResource(
                "/v1/node-resources",
                new NodeResourcePOST() {
                    {
                        nodeId = calculusId;
                        resourceId = integrationId;
                    }
                },
                status().isConflict());
    }

    @Test
    public void can_delete_node_resource() throws Exception {
        var topic = newTopic();
        var resource = newResource();
        var connection = save(NodeConnection.create(topic, resource, Relevance.CORE));
        var connectionId = connection.getPublicId();
        testUtils.deleteResource("/v1/node-resources/" + connectionId);
        assertNull(nodeRepository.findByPublicId(connectionId));
    }

    @Test
    public void can_update_node_resource() throws Exception {
        URI id = save(NodeConnection.create(newTopic(), newResource(), Relevance.CORE))
                .getPublicId();

        testUtils.updateResource("/v1/node-resources/" + id, new NodeResourcePUT() {
            {
                primary = Optional.of(true);
            }
        });

        assertTrue(nodeConnectionRepository.getByPublicId(id).isPrimary().orElseThrow());
    }

    @Test
    public void cannot_unset_primary_node() throws Exception {
        URI id = save(NodeConnection.create(newTopic(), newResource(), Relevance.CORE, NodeConnectionType.BRANCH, true))
                .getPublicId();

        testUtils.updateResource(
                "/v1/node-resources/" + id,
                new NodeResourcePUT() {
                    {
                        primary = Optional.of(false);
                    }
                },
                status().is4xxClientError());
    }

    @Test
    public void deleted_primary_node_is_replaced() throws Exception {
        var resource = builder.node(NodeType.RESOURCE, r -> r.name("resource"));
        var primary = builder.node(NodeType.TOPIC, t -> t.name("primary").resource(resource));
        builder.node(NodeType.TOPIC, t -> t.name("other").resource(resource, true));

        testUtils.deleteResource("/v1/nodes/" + primary.getPublicId());

        assertEquals("other", resource.getPrimaryNode().get().getName());
    }

    @Test
    public void can_get_resources() throws Exception {
        Node electricity = newTopic().name("electricity");
        var alternatingCurrent = newResource();
        alternatingCurrent.setName("How alternating current works");
        save(NodeConnection.create(electricity, alternatingCurrent, Relevance.CORE));

        var calculus = newTopic().name("calculus");
        var integration = newResource();
        integration.setName("Introduction to integration");
        save(NodeConnection.create(calculus, integration, Relevance.CORE));

        var response = testUtils.getResource("/v1/node-resources");
        var topicResources = testUtils.getObject(NodeResourceDTO[].class, response);

        assertEquals(2, topicResources.length);
        assertAnyTrue(
                topicResources,
                t -> electricity.getPublicId().equals(t.nodeId)
                        && alternatingCurrent.getPublicId().equals(t.resourceId));
        assertAnyTrue(
                topicResources,
                t -> calculus.getPublicId().equals(t.nodeId)
                        && integration.getPublicId().equals(t.resourceId));
        assertAllTrue(topicResources, t -> isValidId(t.id));
    }

    @Test
    public void can_get_resource_connections_paginated() throws Exception {
        var connections = createTenContiguousRankedConnections();

        var response = testUtils.getResource("/v1/node-resources/page?page=1&pageSize=5");
        var page1 = testUtils.getObject(SearchResultDTO.class, response);
        assertEquals(5, page1.getResults().size());

        var response2 = testUtils.getResource("/v1/node-resources/page?page=2&pageSize=5");
        var page2 = testUtils.getObject(SearchResultDTO.class, response2);
        assertEquals(5, page2.getResults().size());

        var result = Stream.concat(page1.getResults().stream(), page2.getResults().stream())
                .toList();

        // noinspection SuspiciousMethodCalls
        assertTrue(connections.stream()
                .map(DomainEntity::getPublicId)
                .map(Object::toString)
                .toList()
                .containsAll(result.stream()
                        .map(r -> ((LinkedHashMap<String, String>) r).get("id"))
                        .toList()));
    }

    @Test
    public void can_get_topic_resource() throws Exception {
        Node electricity = newTopic().name("electricity");
        var alternatingCurrent = newResource();
        alternatingCurrent.setName("How alternating current works");
        var topicResource = save(NodeConnection.create(electricity, alternatingCurrent, Relevance.CORE));

        var resource = testUtils.getResource("/v1/node-resources/" + topicResource.getPublicId());
        var topicResourceIndexDocument = testUtils.getObject(NodeResourceDTO.class, resource);
        assertEquals(electricity.getPublicId(), topicResourceIndexDocument.nodeId);
        assertEquals(alternatingCurrent.getPublicId(), topicResourceIndexDocument.resourceId);
    }

    @Test
    public void resource_can_only_have_one_primary_node() throws Exception {
        var graphs = builder.node(NodeType.RESOURCE, r -> r.name("graphs"));

        builder.node(NodeType.TOPIC, t -> t.name("elementary maths").resource(graphs));

        Node graphTheory = builder.node(NodeType.TOPIC, t -> t.name("graph theory"));

        testUtils.createResource("/v1/node-resources", new NodeResourcePOST() {
            {
                nodeId = graphTheory.getPublicId();
                resourceId = graphs.getPublicId();
                primary = Optional.of(true);
            }
        });

        graphs.getParentConnections().forEach(nodeResource -> {
            if (nodeResource.getParent().orElseThrow(RuntimeException::new).equals(graphTheory)) {
                assertTrue(nodeResource.isPrimary().orElseThrow());
            } else {
                assertFalse(nodeResource.isPrimary().orElseThrow());
            }
        });
    }

    @Test
    public void can_order_resources() throws Exception {
        Node geometry = builder.node(NodeType.TOPIC, t -> t.name("Geometry").publicId("urn:topic:1"));
        var squares = builder.node(NodeType.RESOURCE, r -> r.name("Squares").publicId("urn:resource:1"));
        var circles = builder.node(NodeType.RESOURCE, r -> r.name("Circles").publicId("urn:resource:2"));

        URI geometrySquares =
                save(NodeConnection.create(geometry, squares, Relevance.CORE)).getPublicId();
        URI geometryCircles =
                save(NodeConnection.create(geometry, circles, Relevance.CORE)).getPublicId();
        testUtils.updateResource("/v1/node-resources/" + geometryCircles, new NodeResourcePUT() {
            {
                primary = Optional.of(true);
                rank = Optional.of(1);
            }
        });
        testUtils.updateResource("/v1/node-resources/" + geometrySquares, new NodeResourcePUT() {
            {
                primary = Optional.of(true);
                rank = Optional.of(2);
            }
        });

        var response = testUtils.getResource("/v1/nodes/" + geometry.getPublicId() + "/resources");
        var resources = testUtils.getObject(NodeChildDTO[].class, response);
        assertEquals(circles.getPublicId(), resources[0].getId());
        assertEquals(squares.getPublicId(), resources[1].getId());
    }

    @Test
    public void resources_can_have_default_rank() throws Exception {
        builder.node(
                NodeType.TOPIC,
                t -> t.name("elementary maths").resource(r -> r.name("graphs")).resource(r -> r.name("sets")));

        var response = testUtils.getResource("/v1/node-resources");
        NodeResourceDTO[] topicResources = testUtils.getObject(NodeResourceDTO[].class, response);
        assertAllTrue(topicResources, tr -> tr.rank == 0);
    }

    @Test
    public void can_create_resources_with_rank() throws Exception {
        Node geometry = builder.node(NodeType.TOPIC, t -> t.name("Geometry").publicId("urn:topic:1"));
        var squares = builder.node(NodeType.RESOURCE, r -> r.name("Squares").publicId("urn:resource:1"));
        var circles = builder.node(NodeType.RESOURCE, r -> r.name("Circles").publicId("urn:resource:2"));

        testUtils.createResource("/v1/node-resources", new NodeResourcePOST() {
            {
                primary = Optional.of(true);
                nodeId = geometry.getPublicId();
                resourceId = squares.getPublicId();
                rank = Optional.of(2);
            }
        });

        testUtils.createResource("/v1/node-resources", new NodeResourcePOST() {
            {
                primary = Optional.of(true);
                nodeId = geometry.getPublicId();
                resourceId = circles.getPublicId();
                rank = Optional.of(1);
            }
        });

        var response = testUtils.getResource("/v1/nodes/" + geometry.getPublicId() + "/resources");
        final var resources = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(circles.getPublicId(), resources[0].getId());
        assertEquals(squares.getPublicId(), resources[1].getId());
    }

    @Test
    public void update_child_resource_rank_modifies_other_contiguous_ranks() throws Exception {
        List<NodeConnection> nodeResources = createTenContiguousRankedConnections(); // creates ranks 1, 2, 3, 4, 5, 6,
        // 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeResources);

        // make the last object the first
        var updatedConnection = nodeResources.getLast();
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource(
                "/v1/node-resources/" + updatedConnection.getPublicId().toString(), new NodeResourcePUT() {
                    {
                        primary = Optional.of(true);
                        rank = Optional.of(1);
                    }
                });
        assertEquals(1, updatedConnection.getRank());

        // verify that the other connections have been updated
        for (var nodeResource : nodeResources) {
            var response = testUtils.getResource(
                    "/v1/node-resources/" + nodeResource.getPublicId().toString());
            var connectionFromDb = testUtils.getObject(NodeResourceDTO.class, response);
            // verify that the other connections have had their rank bumped up 1
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                assertEquals(oldRank + 1, connectionFromDb.rank);
            }
        }
    }

    @Test
    public void update_child_resource_rank_does_not_alter_noncontiguous_ranks() throws Exception {

        var nodeResources = createTenNonContiguousRankedConnections(); // creates ranks 1, 2, 3, 4, 5,
        // 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeResources);

        // make the last object the first
        var updatedConnection = nodeResources.getLast();
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource(
                "/v1/node-resources/" + updatedConnection.getPublicId().toString(), new NodeResourcePUT() {
                    {
                        primary = Optional.of(true);
                        rank = Optional.of(1);
                    }
                });
        assertEquals(1, updatedConnection.getRank());

        // verify that the other connections have been updated
        for (var nodeResource : nodeResources) {
            var response = testUtils.getResource(
                    "/v1/node-resources/" + nodeResource.getPublicId().toString());
            var connectionFromDb = testUtils.getObject(NodeResourceDTO.class, response);
            // verify that only the contiguous connections are updated
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                if (oldRank <= 5) {
                    assertEquals(oldRank + 1, connectionFromDb.rank);
                } else {
                    assertEquals(oldRank, connectionFromDb.rank);
                }
            }
        }
    }

    @Test
    public void update_child_resource_rank_higher_rank_does_not_modify_existing_connections() throws Exception {
        List<NodeConnection> nodeResources = createTenContiguousRankedConnections();
        Map<String, Integer> mappedRanks = mapConnectionRanks(nodeResources);

        // set rank for last object to higher than any existing
        var updatedConnection = nodeResources.getLast();
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource(
                "/v1/node-resources/" + nodeResources.get(9).getPublicId().toString(), new NodeResourcePUT() {
                    {
                        primary = Optional.of(true);
                        rank = Optional.of(99);
                    }
                });
        assertEquals(99, updatedConnection.getRank());

        // verify that the other connections are unchanged
        for (var nodeResource : nodeResources) {
            var response = testUtils.getResource(
                    "/v1/node-resources/" + nodeResource.getPublicId().toString());
            var connection = testUtils.getObject(NodeResourceDTO.class, response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    @Test
    public void update_metadata_for_connection() throws Exception {
        URI id = save(NodeConnection.create(newTopic(), newResource(), Relevance.CORE))
                .getPublicId();
        testUtils.updateResource(
                "/v1/node-resources/" + id + "/metadata",
                new MetadataDTO() {
                    {
                        visible = false;
                        grepCodes = Set.of("KM123");
                        customFields = Map.of("key", "value");
                    }
                },
                status().isOk());
        var connection = nodeConnectionRepository.getByPublicId(id);
        assertFalse(connection.getMetadata().isVisible());
        Set<String> codes = connection.getMetadata().getGrepCodes().stream()
                .map(JsonGrepCode::getCode)
                .collect(Collectors.toSet());
        assertTrue(codes.contains("KM123"));
        var customFieldValues = connection.getCustomFields();
        assertTrue(customFieldValues.containsKey("key"));
        assertTrue(customFieldValues.containsValue("value"));
    }

    private Map<String, Integer> mapConnectionRanks(List<NodeConnection> nodeResources) {
        Map<String, Integer> mappedRanks = new HashMap<>();
        for (var nr : nodeResources) {
            mappedRanks.put(nr.getPublicId().toString(), nr.getRank());
        }
        return mappedRanks;
    }

    private List<NodeConnection> createTenContiguousRankedConnections() {
        List<NodeConnection> connections = new ArrayList<>();
        Node parent = newTopic();
        for (int i = 1; i < 11; i++) {
            var sub = newResource();
            var nodeResource = NodeConnection.create(parent, sub, Relevance.CORE);
            nodeResource.setRank(i);
            connections.add(nodeResource);
            save(nodeResource);
        }
        return connections;
    }

    private List<NodeConnection> createTenNonContiguousRankedConnections() {
        List<NodeConnection> connections = new ArrayList<>();
        Node parent = newTopic();
        for (int i = 1; i < 11; i++) {
            var sub = newResource();
            var nodeResource = NodeConnection.create(parent, sub, Relevance.CORE);
            if (i <= 5) {
                nodeResource.setRank(i);
            } else {
                nodeResource.setRank(i * 10);
            }
            connections.add(nodeResource);
            save(nodeResource);
        }
        return connections;
    }
}
