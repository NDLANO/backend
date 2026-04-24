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
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.dtos.SubjectTopicPUT;
import no.ndla.taxonomy.rest.v1.dtos.TopicSubtopicDTO;
import no.ndla.taxonomy.rest.v1.dtos.TopicSubtopicPOST;
import no.ndla.taxonomy.rest.v1.dtos.TopicSubtopicPUT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class TopicSubtopicsTest extends RestTest {

    @BeforeEach
    public void add_core_relevance() {
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
    }

    @Test
    public void can_add_subtopic_to_topic() throws Exception {
        URI integrationId, calculusId;
        calculusId = builder.node(NodeType.TOPIC, t -> t.name("calculus")).getPublicId();
        integrationId = builder.node(NodeType.TOPIC, t -> t.name("integration")).getPublicId();

        URI id = getId(testUtils.createResource("/v1/topic-subtopics", new TopicSubtopicPOST() {
            {
                topicid = calculusId;
                subtopicid = integrationId;
            }
        }));

        final var connection = nodeConnectionRepository.findByPublicId(id);
        assertNotNull(connection);

        Node calculus = nodeRepository.getByPublicId(calculusId);
        assertEquals(1, calculus.getChildNodes().size());
        assertAnyTrue(calculus.getChildNodes(), t -> "integration".equals(t.getName()));
    }

    @Test
    public void cannot_add_existing_subtopic_to_topic() throws Exception {
        URI integrationId = builder.node("integration", NodeType.TOPIC, t -> t.name("integration"))
                .getPublicId();
        URI calculusId = builder.node(NodeType.TOPIC, t -> t.name("calculus").child("integration"))
                .getPublicId();

        testUtils.createResource(
                "/v1/topic-subtopics",
                new TopicSubtopicPOST() {
                    {
                        topicid = calculusId;
                        subtopicid = integrationId;
                    }
                },
                status().isConflict());
    }

    @Test
    public void can_delete_topic_subtopic() throws Exception {
        URI id = save(NodeConnection.create(newTopic(), newTopic(), Relevance.CORE))
                .getPublicId();
        testUtils.deleteResource("/v1/topic-subtopics/" + id);
        assertNull(nodeRepository.findByPublicId(id));
    }

    @Test
    public void can_get_topics() throws Exception {
        URI alternatingCurrentId = builder.node("ac", NodeType.TOPIC, t -> t.name("alternating current"))
                .getPublicId();
        URI electricityId = builder.node(
                        NodeType.TOPIC, t -> t.name("electricity").child("ac", NodeType.TOPIC))
                .getPublicId();
        URI integrationId = builder.node("integration", NodeType.TOPIC, t -> t.name("integration"))
                .getPublicId();
        URI calculusId = builder.node(NodeType.TOPIC, t -> t.name("calculus").child("integration", NodeType.TOPIC))
                .getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/topic-subtopics");
        TopicSubtopicDTO[] topicSubtopics = testUtils.getObject(TopicSubtopicDTO[].class, response);

        assertEquals(2, topicSubtopics.length);
        assertAnyTrue(
                topicSubtopics, t -> electricityId.equals(t.topicid) && alternatingCurrentId.equals(t.subtopicid));
        assertAnyTrue(topicSubtopics, t -> calculusId.equals(t.topicid) && integrationId.equals(t.subtopicid));
        assertAllTrue(topicSubtopics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_topic_subtopic() throws Exception {
        URI topicid, subtopicid, id;
        Node electricity = newTopic().name("electricity");
        Node alternatingCurrent = newTopic().name("alternating current");
        NodeConnection topicSubtopic = save(NodeConnection.create(electricity, alternatingCurrent, Relevance.CORE));

        topicid = electricity.getPublicId();
        subtopicid = alternatingCurrent.getPublicId();
        id = topicSubtopic.getPublicId();

        MockHttpServletResponse resource = testUtils.getResource("/v1/topic-subtopics/" + id);
        TopicSubtopicDTO topicSubtopicIndexDocument = testUtils.getObject(TopicSubtopicDTO.class, resource);

        assertEquals(topicid, topicSubtopicIndexDocument.topicid);

        assertEquals(subtopicid, topicSubtopicIndexDocument.subtopicid);
    }

    @Test
    public void subtopics_have_default_rank() throws Exception {
        builder.node(NodeType.TOPIC, t -> t.name("electricity")
                .child(NodeType.TOPIC, st -> st.name("alternating currents"))
                .child(NodeType.TOPIC, st -> st.name("wiring")));
        MockHttpServletResponse response = testUtils.getResource(("/v1/topic-subtopics"));
        TopicSubtopicDTO[] subtopics = testUtils.getObject(TopicSubtopicDTO[].class, response);

        assertAllTrue(subtopics, st -> st.rank == 0);
    }

    @Test
    public void subtopics_can_be_created_with_rank() throws Exception {
        Node subject = builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Subject").publicId("urn:subject:1"));
        Node electricity =
                builder.node(NodeType.TOPIC, s -> s.name("Electricity").publicId("urn:topic:1"));
        save(NodeConnection.create(subject, electricity, Relevance.CORE));
        Node alternatingCurrents =
                builder.node(NodeType.TOPIC, t -> t.name("Alternating currents").publicId("urn:topic:11"));
        Node wiring = builder.node(NodeType.TOPIC, t -> t.name("Wiring").publicId("urn:topic:12"));

        testUtils.createResource("/v1/topic-subtopics", new TopicSubtopicPOST() {
            {
                topicid = electricity.getPublicId();
                subtopicid = alternatingCurrents.getPublicId();
                rank = Optional.of(2);
            }
        });

        testUtils.createResource("/v1/topic-subtopics", new TopicSubtopicPOST() {
            {
                topicid = electricity.getPublicId();
                subtopicid = wiring.getPublicId();
                rank = Optional.of(1);
            }
        });

        MockHttpServletResponse response =
                testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics?recursive=true");
        TopicSubtopicDTO[] topics = testUtils.getObject(TopicSubtopicDTO[].class, response);

        assertEquals(electricity.getPublicId(), topics[0].id);
        assertEquals(wiring.getPublicId(), topics[1].id);
        assertEquals(alternatingCurrents.getPublicId(), topics[2].id);
    }

    @Test
    public void can_update_subtopic_rank() throws Exception {
        URI id = save(NodeConnection.create(newTopic(), newTopic(), Relevance.CORE))
                .getPublicId();

        testUtils.updateResource("/v1/topic-subtopics/" + id, new TopicSubtopicPUT() {
            {
                primary = Optional.of(true);
                rank = Optional.of(99);
            }
        });

        assertEquals(99, nodeConnectionRepository.getByPublicId(id).getRank());
    }

    @Test
    public void update_subtopic_rank_modifies_other_contiguous_ranks() throws Exception {
        List<NodeConnection> topicSubtopics = createTenContiguousRankedConnections(); // creates ranks 1, 2, 3, 4, 5, 6,
        // 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicSubtopics);

        // make the last object the first
        NodeConnection updatedConnection = topicSubtopics.getLast();
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource(
                "/v1/topic-subtopics/" + updatedConnection.getPublicId().toString(), new TopicSubtopicPUT() {
                    {
                        primary = Optional.of(true);
                        rank = Optional.of(1);
                    }
                });
        assertEquals(1, updatedConnection.getRank());

        // verify that the other connections have been updated
        for (NodeConnection topicSubtopic : topicSubtopics) {
            MockHttpServletResponse response = testUtils.getResource(
                    "/v1/topic-subtopics/" + topicSubtopic.getPublicId().toString());
            TopicSubtopicDTO connectionFromDb = testUtils.getObject(TopicSubtopicDTO.class, response);
            // verify that the other connections have had their rank bumped up 1
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                assertEquals(oldRank + 1, connectionFromDb.rank);
            }
        }
    }

    @Test
    public void update_subtopic_rank_does_not_alter_noncontiguous_ranks() throws Exception {

        List<NodeConnection> topicSubtopics = createTenNonContiguousRankedConnections(); // creates ranks 1, 2, 3, 4, 5,
        // 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicSubtopics);

        // make the last object the first
        NodeConnection updatedConnection = topicSubtopics.getLast();
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource(
                "/v1/topic-subtopics/" + updatedConnection.getPublicId().toString(), new SubjectTopicPUT() {
                    {
                        primary = Optional.of(true);
                        rank = Optional.of(1);
                    }
                });
        assertEquals(1, updatedConnection.getRank());

        // verify that the other connections have been updated
        for (NodeConnection topicSubtopic : topicSubtopics) {
            MockHttpServletResponse response = testUtils.getResource(
                    "/v1/topic-subtopics/" + topicSubtopic.getPublicId().toString());
            TopicSubtopicDTO connectionFromDb = testUtils.getObject(TopicSubtopicDTO.class, response);
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
    public void update_subtopic_rank_higher_rank_does_not_modify_existing_connections() throws Exception {
        List<NodeConnection> topicSubtopics = createTenContiguousRankedConnections();
        Map<String, Integer> mappedRanks = mapConnectionRanks(topicSubtopics);

        // set rank for last object to higher than any existing
        NodeConnection updatedConnection = topicSubtopics.getLast();
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource(
                "/v1/topic-subtopics/" + topicSubtopics.get(9).getPublicId().toString(), new SubjectTopicPUT() {
                    {
                        primary = Optional.of(true);
                        rank = Optional.of(99);
                    }
                });
        assertEquals(99, updatedConnection.getRank());

        // verify that the other connections are unchanged
        for (NodeConnection topicSubtopic : topicSubtopics) {
            MockHttpServletResponse response = testUtils.getResource(
                    "/v1/topic-subtopics/" + topicSubtopic.getPublicId().toString());
            TopicSubtopicDTO connection = testUtils.getObject(TopicSubtopicDTO.class, response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    private Map<String, Integer> mapConnectionRanks(List<NodeConnection> topicSubtopics) {
        Map<String, Integer> mappedRanks = new HashMap<>();
        for (NodeConnection ts : topicSubtopics) {
            mappedRanks.put(ts.getPublicId().toString(), ts.getRank());
        }
        return mappedRanks;
    }

    private List<NodeConnection> createTenContiguousRankedConnections() {
        List<NodeConnection> connections = new ArrayList<>();
        Node parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Node sub = newTopic();
            NodeConnection topicSubtopic = NodeConnection.create(parent, sub, Relevance.CORE);
            topicSubtopic.setRank(i);
            connections.add(topicSubtopic);
            save(topicSubtopic);
        }
        return connections;
    }

    private List<NodeConnection> createTenNonContiguousRankedConnections() {
        List<NodeConnection> connections = new ArrayList<>();
        Node parent = newTopic();
        for (int i = 1; i < 11; i++) {
            Node sub = newTopic();
            NodeConnection topicSubtopic = NodeConnection.create(parent, sub, Relevance.CORE);
            if (i <= 5) {
                topicSubtopic.setRank(i);
            } else {
                topicSubtopic.setRank(i * 10);
            }
            connections.add(topicSubtopic);
            save(topicSubtopic);
        }
        return connections;
    }
}
