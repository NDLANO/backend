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
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.rest.v1.dtos.SubjectTopicDTO;
import no.ndla.taxonomy.rest.v1.dtos.SubjectTopicPOST;
import no.ndla.taxonomy.rest.v1.dtos.SubjectTopicPUT;
import no.ndla.taxonomy.service.RankableConnectionUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class SubjectTopicsTest extends RestTest {

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
        nodeConnectionRepository.deleteAllAndFlush();
    }

    @Test
    public void can_add_topic_to_subject() throws Exception {
        URI subjectId, topicId;
        subjectId = newSubject().name("physics").getPublicId();
        topicId = newTopic().name("trigonometry").getPublicId();

        URI id = getId(testUtils.createResource("/v1/subject-topics", new SubjectTopicPOST() {
            {
                this.subjectid = subjectId;
                this.topicid = topicId;
            }
        }));

        final var connection = nodeConnectionRepository.findByPublicId(id);
        assertNotNull(connection);

        Node physics = nodeRepository.getByPublicId(subjectId);
        assertEquals(1, physics.getChildNodes().size());
        assertAnyTrue(physics.getChildNodes(), t -> "trigonometry".equals(t.getName()));
    }

    @Test
    public void cannot_add_existing_topic_to_subject() throws Exception {
        Node physics = newSubject().name("physics");
        Node trigonometry = newTopic().name("trigonometry");
        NodeConnection.create(physics, trigonometry, Relevance.CORE);

        URI subjectId = physics.getPublicId();
        URI topicId = trigonometry.getPublicId();

        testUtils.createResource(
                "/v1/subject-topics",
                new SubjectTopicPOST() {
                    {
                        this.subjectid = subjectId;
                        this.topicid = topicId;
                    }
                },
                status().isConflict());
    }

    @Test
    public void can_delete_subject_topic() throws Exception {
        URI id = save(NodeConnection.create(newSubject(), newTopic(), Relevance.CORE))
                .getPublicId();
        testUtils.deleteResource("/v1/subject-topics/" + id);
        assertNull(nodeRepository.findByPublicId(id));
    }

    @Test
    public void can_update_subject_rank() throws Exception {
        URI id = save(NodeConnection.create(newSubject(), newTopic(), Relevance.CORE))
                .getPublicId();

        MockHttpServletResponse responseBefore = testUtils.getResource("/v1/subject-topics/" + id.toString());
        SubjectTopicDTO connection = testUtils.getObject(SubjectTopicDTO.class, responseBefore);
        assertEquals(0, connection.rank);

        testUtils.updateResource("/v1/subject-topics/" + id, new SubjectTopicPUT() {
            {
                primary = Optional.of(true);
                rank = Optional.of(12);
            }
        });

        MockHttpServletResponse responseAfter = testUtils.getResource("/v1/subject-topics/" + id);
        SubjectTopicDTO connectionAfter = testUtils.getObject(SubjectTopicDTO.class, responseAfter);

        assertEquals(12, connectionAfter.rank);
    }

    @Test
    public void update_subject_rank_modifies_other_contiguous_ranks() throws Exception {
        List<NodeConnection> subjectTopics = createTenContiguousRankedNodeConnections(); // creates ranks 1, 2, 3, 4, 5,
        // 6, 7, 8, 9, 10
        Map<String, Integer> mappedRanks = mapNodeConnectionRanks(subjectTopics);

        // make the last object the first
        NodeConnection updatedConnection = subjectTopics.getLast();
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource(
                "/v1/subject-topics/" + updatedConnection.getPublicId().toString(), new SubjectTopicPUT() {
                    {
                        primary = Optional.of(true);
                        rank = Optional.of(1);
                    }
                });
        assertEquals(1, updatedConnection.getRank());

        // verify that the other connections have been updated
        for (NodeConnection subjectTopic : subjectTopics) {
            MockHttpServletResponse response = testUtils.getResource(
                    "/v1/subject-topics/" + subjectTopic.getPublicId().toString());
            SubjectTopicDTO connectionFromDb = testUtils.getObject(SubjectTopicDTO.class, response);
            // verify that the other connections have had their rank bumped up 1
            if (!connectionFromDb.id.equals(updatedConnection.getPublicId())) {
                int oldRank = mappedRanks.get(connectionFromDb.id.toString());
                assertEquals(oldRank + 1, connectionFromDb.rank);
            }
        }
    }

    @Test
    public void update_subject_rank_modifies_other_noncontiguous_ranks() throws Exception {
        List<NodeConnection> subjectTopics = createTenNonContiguousRankedNodeConnections(); // creates ranks 1, 2, 3, 4,
        // 5, 60, 70, 80, 90, 100
        Map<String, Integer> mappedRanks = mapNodeConnectionRanks(subjectTopics);

        // make the last object the first
        NodeConnection updatedConnection = subjectTopics.getLast();
        assertEquals(100, updatedConnection.getRank());
        testUtils.updateResource(
                "/v1/subject-topics/" + updatedConnection.getPublicId().toString(), new SubjectTopicPUT() {
                    {
                        primary = Optional.of(true);
                        rank = Optional.of(1);
                    }
                });
        assertEquals(1, updatedConnection.getRank());

        // verify that the other connections have been updated
        for (NodeConnection subjectTopic : subjectTopics) {
            MockHttpServletResponse response = testUtils.getResource(
                    "/v1/subject-topics/" + subjectTopic.getPublicId().toString());
            SubjectTopicDTO connectionFromDb = testUtils.getObject(SubjectTopicDTO.class, response);
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
    public void update_subject_rank_higher_rank_does_not_modify_existing_connections() throws Exception {
        List<NodeConnection> subjectTopics = createTenContiguousRankedNodeConnections();
        Map<String, Integer> mappedRanks = mapNodeConnectionRanks(subjectTopics);

        // set rank for last object to higher than any existing
        NodeConnection updatedConnection = subjectTopics.getLast();
        assertEquals(10, updatedConnection.getRank());
        testUtils.updateResource(
                "/v1/subject-topics/" + subjectTopics.get(9).getPublicId().toString(), new SubjectTopicPUT() {
                    {
                        primary = Optional.of(true);
                        rank = Optional.of(99);
                    }
                });
        assertEquals(99, updatedConnection.getRank());

        // verify that the other connections are unchanged
        for (NodeConnection subjectTopic : subjectTopics) {
            MockHttpServletResponse response = testUtils.getResource(
                    "/v1/subject-topics/" + subjectTopic.getPublicId().toString());
            SubjectTopicDTO connection = testUtils.getObject(SubjectTopicDTO.class, response);
            if (!connection.id.equals(updatedConnection.getPublicId())) {
                assertEquals(mappedRanks.get(connection.id.toString()).intValue(), connection.rank);
            }
        }
    }

    @Test
    public void update_subject_rank_no_existing_connections_returns_single_connection() {
        Node subject = new Node(NodeType.SUBJECT);
        Node topic = new Node(NodeType.TOPIC);

        NodeConnection st = NodeConnection.create(subject, topic, Relevance.CORE);
        List<NodeConnection> rankedList = RankableConnectionUpdater.rank(new ArrayList<>(), st, 99);
        assertEquals(1, rankedList.size());
    }

    @Test
    public void can_get_topics() throws Exception {
        Node physics = newSubject().name("physics");
        Node electricity = newTopic().name("electricity");
        save(NodeConnection.create(physics, electricity, Relevance.CORE));

        Node mathematics = newSubject().name("mathematics");
        Node trigonometry = newTopic().name("trigonometry");
        save(NodeConnection.create(mathematics, trigonometry, Relevance.CORE));

        URI physicsId = physics.getPublicId();
        URI electricityId = electricity.getPublicId();
        URI mathematicsId = mathematics.getPublicId();
        URI trigonometryId = trigonometry.getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subject-topics");
        SubjectTopicDTO[] subjectTopics = testUtils.getObject(SubjectTopicDTO[].class, response);

        assertEquals(2, subjectTopics.length);
        assertAnyTrue(subjectTopics, t -> physicsId.equals(t.subjectid) && electricityId.equals(t.topicid));
        assertAnyTrue(subjectTopics, t -> mathematicsId.equals(t.subjectid) && trigonometryId.equals(t.topicid));
        assertAllTrue(subjectTopics, t -> isValidId(t.id));
    }

    @Test
    public void can_get_subject_topic() throws Exception {
        Node physics = newSubject().name("physics");
        Node electricity = newTopic().name("electricity");
        NodeConnection subjectTopic = save(NodeConnection.create(physics, electricity, Relevance.CORE));

        URI subjectid = physics.getPublicId();
        URI topicid = electricity.getPublicId();
        URI id = subjectTopic.getPublicId();

        MockHttpServletResponse resource = testUtils.getResource("/v1/subject-topics/" + id);
        SubjectTopicDTO subjectTopicIndexDocument = testUtils.getObject(SubjectTopicDTO.class, resource);
        assertEquals(subjectid, subjectTopicIndexDocument.subjectid);
        assertEquals(topicid, subjectTopicIndexDocument.topicid);
    }

    @Test
    public void topic_has_default_rank() throws Exception {
        builder.node(
                NodeType.SUBJECT,
                s -> s.isContext(true).name("Mathematics").child(NodeType.TOPIC, t -> t.name("Geometry")));

        MockHttpServletResponse response = testUtils.getResource("/v1/subject-topics");
        SubjectTopicDTO[] topics = testUtils.getObject(SubjectTopicDTO[].class, response);

        assertAllTrue(topics, t -> t.rank == 0);
    }

    @Test
    public void can_change_sorting_order_for_topics() throws Exception {
        Node mathematics = builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Mathematics").publicId("urn:subject:1"));
        Node geometry = builder.node(NodeType.TOPIC, t -> t.name("Geometry").publicId("urn:topic:1"));
        Node statistics = builder.node(NodeType.TOPIC, t -> t.name("Statistics").publicId("urn:topic:2"));
        NodeConnection geometryMaths = save(NodeConnection.create(mathematics, geometry, Relevance.CORE));
        NodeConnection statisticsMaths = save(NodeConnection.create(mathematics, statistics, Relevance.CORE));

        testUtils.updateResource("/v1/subject-topics/" + geometryMaths.getPublicId(), new SubjectTopicPUT() {
            {
                primary = Optional.of(true);
                rank = Optional.of(2);
            }
        });

        testUtils.updateResource("/v1/subject-topics/" + statisticsMaths.getPublicId(), new SubjectTopicPUT() {
            {
                primary = Optional.of(true);
                rank = Optional.of(1);
            }
        });

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/topics");
        SubjectTopicDTO[] topics = testUtils.getObject(SubjectTopicDTO[].class, response);

        assertEquals(statistics.getPublicId(), topics[0].id);
        assertEquals(geometry.getPublicId(), topics[1].id);
    }

    @Test
    public void can_change_sorting_order_for_subtopics() throws Exception {
        Node mathematics = builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Mathematics").publicId("urn:subject:1"));
        Node geometry = builder.node(NodeType.TOPIC, t -> t.name("Geometry").publicId("urn:topic:1"));
        Node statistics = builder.node(NodeType.TOPIC, t -> t.name("Statistics").publicId("urn:topic:2"));
        Node subtopic1 = builder.node(NodeType.TOPIC, t -> t.name("Subtopic 1").publicId("urn:topic:aa"));
        Node subtopic2 = builder.node(NodeType.TOPIC, t -> t.name("Subtopic 2").publicId("urn:topic:ab"));
        NodeConnection geometryMaths = save(NodeConnection.create(mathematics, geometry, Relevance.CORE));
        NodeConnection statisticsMaths = save(NodeConnection.create(mathematics, statistics, Relevance.CORE));
        NodeConnection tst1 = save(NodeConnection.create(geometry, subtopic1, Relevance.CORE));
        NodeConnection tst2 = save(NodeConnection.create(geometry, subtopic2, Relevance.CORE));

        testUtils.updateResource("/v1/subject-topics/" + geometryMaths.getPublicId(), new SubjectTopicPUT() {
            {
                primary = Optional.of(true);
                rank = Optional.of(2);
            }
        });

        testUtils.updateResource("/v1/subject-topics/" + statisticsMaths.getPublicId(), new SubjectTopicPUT() {
            {
                primary = Optional.of(true);
                rank = Optional.of(1);
            }
        });

        testUtils.updateResource("/v1/topic-subtopics/" + tst1.getPublicId(), new SubjectTopicPUT() {
            {
                primary = Optional.of(true);
                rank = Optional.of(2);
            }
        });

        testUtils.updateResource("/v1/topic-subtopics/" + tst2.getPublicId(), new SubjectTopicPUT() {
            {
                primary = Optional.of(true);
                rank = Optional.of(1);
            }
        });
        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/topics?recursive=true");
        SubjectTopicDTO[] topics = testUtils.getObject(SubjectTopicDTO[].class, response);

        assertEquals(statistics.getPublicId(), topics[0].id);
        assertEquals(geometry.getPublicId(), topics[1].id);
        assertEquals(subtopic2.getPublicId(), topics[2].id);
        assertEquals(subtopic1.getPublicId(), topics[3].id);
    }

    @Test
    public void can_create_topic_with_rank() throws Exception {
        Node mathematics = builder.node(
                NodeType.SUBJECT, s -> s.isContext(true).name("Mathematics").publicId("urn:subject:1"));
        Node geometry = builder.node(NodeType.TOPIC, t -> t.name("Geometry").publicId("urn:topic:1"));
        Node statistics = builder.node(NodeType.TOPIC, t -> t.name("Statistics").publicId("urn:topic:2"));

        testUtils.createResource("/v1/subject-topics", new SubjectTopicPOST() {
            {
                subjectid = mathematics.getPublicId();
                topicid = geometry.getPublicId();
                rank = Optional.of(2);
            }
        });
        testUtils.createResource("/v1/subject-topics", new SubjectTopicPOST() {
            {
                subjectid = mathematics.getPublicId();
                topicid = statistics.getPublicId();
                rank = Optional.of(1);
            }
        });

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/topics");
        SubjectTopicDTO[] topics = testUtils.getObject(SubjectTopicDTO[].class, response);

        assertEquals(statistics.getPublicId(), topics[0].id);
        assertEquals(geometry.getPublicId(), topics[1].id);
    }

    private Map<String, Integer> mapNodeConnectionRanks(List<NodeConnection> subjectTopics) {
        Map<String, Integer> mappedRanks = new HashMap<>();
        for (NodeConnection st : subjectTopics) {
            mappedRanks.put(st.getPublicId().toString(), st.getRank());
        }
        return mappedRanks;
    }

    private List<NodeConnection> createTenContiguousRankedNodeConnections() {
        List<NodeConnection> connections = new ArrayList<>();
        Node subject = newSubject();
        for (int i = 1; i < 11; i++) {
            Node topic = newTopic();
            NodeConnection subjectTopic = NodeConnection.create(subject, topic, Relevance.CORE);
            subjectTopic.setRank(i);
            connections.add(subjectTopic);
            save(subjectTopic);
        }
        return connections;
    }

    private List<NodeConnection> createTenNonContiguousRankedNodeConnections() {
        List<NodeConnection> connections = new ArrayList<>();
        Node subject = newSubject();
        for (int i = 1; i < 11; i++) {
            Node topic = newTopic();
            NodeConnection subjectTopic = NodeConnection.create(subject, topic, Relevance.CORE);
            if (i <= 5) {
                subjectTopic.setRank(i);
            } else {
                subjectTopic.setRank(i * 10);
            }
            connections.add(subjectTopic);
            save(subjectTopic);
        }
        return connections;
    }
}
