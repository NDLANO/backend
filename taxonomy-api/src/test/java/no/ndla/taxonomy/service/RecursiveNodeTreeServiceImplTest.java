/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class RecursiveNodeTreeServiceImplTest extends AbstractIntegrationTest {
    private NodeRepository nodeRepository;
    private NodeConnectionRepository nodeConnectionRepository;

    private RecursiveNodeTreeService service;
    private Relevance core;

    @BeforeEach
    void setUp(
            @Autowired NodeConnectionRepository nodeConnectionRepository,
            @Autowired NodeRepository nodeRepository,
            @Autowired TestSeeder testSeeder) {
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;

        nodeRepository.deleteAllAndFlush();

        core = testSeeder.recursiveNodesBySubjectNodeIdTestSetup();

        service = new RecursiveNodeTreeService(nodeConnectionRepository);
    }

    @Test
    void getRecursiveTopics_by_subject() {
        final var subject =
                nodeRepository.findFirstByPublicId(URI.create("urn:subject:1")).orElseThrow();
        final var recursiveNodes = service.getRecursiveNodes(subject);

        final var nodesToFind = new HashSet<>(Set.of(
                "urn:subject:1",
                "urn:topic:1",
                "urn:topic:2",
                "urn:topic:3",
                "urn:topic:4",
                "urn:topic:5",
                "urn:topic:6",
                "urn:topic:7",
                "urn:topic:8"));

        final var topic1 =
                nodeRepository.findFirstByPublicId(URI.create("urn:topic:1")).orElseThrow();
        final var topic3 =
                nodeRepository.findFirstByPublicId(URI.create("urn:topic:3")).orElseThrow();
        final var topic5 =
                nodeRepository.findFirstByPublicId(URI.create("urn:topic:5")).orElseThrow();

        recursiveNodes.forEach(topicTreeElement -> {
            final var node =
                    nodeRepository.findFirstByPublicId(topicTreeElement.getId()).orElseThrow();

            if (!nodesToFind.contains(node.getPublicId().toString())) {
                fail("Topic found is unknown or duplicated " + node.getPublicId());
            }

            switch (node.getPublicId().toString()) {
                case "urn:subject:1":
                    // Root node
                    assertEquals(subject.getPublicId(), topicTreeElement.getId());
                    assertFalse(topicTreeElement.getParentId().isPresent());
                    break;
                case "urn:topic:1":
                    // Child if subject
                    assertEquals(
                            subject.getPublicId(),
                            topicTreeElement.getParentId().orElseThrow());
                    assertTrue(topicTreeElement.getParentId().isPresent());
                    assertEquals(1, topicTreeElement.getRank());
                    break;
                case "urn:topic:2":
                    // Child of topic 1
                    assertTrue(topicTreeElement.getParentId().isPresent());
                    assertEquals(
                            topic1.getPublicId(), topicTreeElement.getParentId().orElseThrow());
                    assertEquals(1, topicTreeElement.getRank());
                    break;
                case "urn:topic:3":
                    // Child if subject
                    assertEquals(
                            subject.getPublicId(),
                            topicTreeElement.getParentId().orElseThrow());
                    assertTrue(topicTreeElement.getParentId().isPresent());
                    assertEquals(2, topicTreeElement.getRank());
                    break;
                case "urn:topic:4":
                    // Child of topic 3
                    assertTrue(topicTreeElement.getParentId().isPresent());
                    assertEquals(
                            topic3.getPublicId(), topicTreeElement.getParentId().orElseThrow());
                    assertEquals(1, topicTreeElement.getRank());
                    break;
                case "urn:topic:5":
                    // Child if subject
                    assertEquals(
                            subject.getPublicId(),
                            topicTreeElement.getParentId().orElseThrow());
                    assertTrue(topicTreeElement.getParentId().isPresent());
                    assertEquals(3, topicTreeElement.getRank());
                    break;
                case "urn:topic:6":
                    // Child of topic 5
                    assertTrue(topicTreeElement.getParentId().isPresent());
                    assertEquals(
                            topic5.getPublicId(), topicTreeElement.getParentId().orElseThrow());
                    assertEquals(1, topicTreeElement.getRank());
                    break;
                case "urn:topic:7":
                    // Child of topic 5
                    assertTrue(topicTreeElement.getParentId().isPresent());
                    assertEquals(
                            topic5.getPublicId(), topicTreeElement.getParentId().orElseThrow());
                    assertEquals(2, topicTreeElement.getRank());
                    break;
                case "urn:topic:8":
                    // Child of topic 5
                    assertTrue(topicTreeElement.getParentId().isPresent());
                    assertEquals(
                            topic5.getPublicId(), topicTreeElement.getParentId().orElseThrow());
                    assertEquals(3, topicTreeElement.getRank());
                    break;
                default:
                    fail();
            }

            nodesToFind.remove(node.getPublicId().toString());
        });

        assertEquals(0, nodesToFind.size());
    }

    @Test
    void getRecursiveTopics_by_topic() {

        final var topic1 =
                nodeRepository.findFirstByPublicId(URI.create("urn:topic:1")).orElseThrow();
        final var topic3 =
                nodeRepository.findFirstByPublicId(URI.create("urn:topic:3")).orElseThrow();
        final var topic5 =
                nodeRepository.findFirstByPublicId(URI.create("urn:topic:5")).orElseThrow();

        // Search for subtopics of topic1
        {
            final var topicElements = service.getRecursiveNodes(topic1);
            final var topicsToFind = new HashSet<>(Set.of("urn:topic:1", "urn:topic:2"));

            topicElements.forEach(topicTreeElement -> {
                final var topic = nodeRepository
                        .findFirstByPublicId(topicTreeElement.getId())
                        .orElseThrow();

                if (!topicsToFind.contains(topic.getPublicId().toString())) {
                    fail("Topic found is unknown or duplicated " + topic.getPublicId());
                }

                switch (topic.getPublicId().toString()) {
                    case "urn:topic:1":
                        assertFalse(topicTreeElement.getParentId().isPresent());
                        assertEquals(0, topicTreeElement.getRank());
                        break;
                    case "urn:topic:2":
                        assertTrue(topicTreeElement.getParentId().isPresent());
                        assertEquals(
                                topic1.getPublicId(),
                                topicTreeElement.getParentId().orElseThrow());
                        assertEquals(1, topicTreeElement.getRank());
                        break;
                    default:
                        fail();
                }

                topicsToFind.remove(topic.getPublicId().toString());
            });

            assertEquals(0, topicsToFind.size());
        }

        // Search for subtopics of topic3
        {
            final var topicElements = service.getRecursiveNodes(topic3);
            final var topicsToFind = new HashSet<>(Set.of("urn:topic:3", "urn:topic:4"));

            topicElements.forEach(topicTreeElement -> {
                final var topic = nodeRepository
                        .findFirstByPublicId(topicTreeElement.getId())
                        .orElseThrow();

                if (!topicsToFind.contains(topic.getPublicId().toString())) {
                    fail("Topic found is unknown or duplicated " + topic.getPublicId());
                }

                switch (topic.getPublicId().toString()) {
                    case "urn:topic:3":
                        assertFalse(topicTreeElement.getParentId().isPresent());
                        assertEquals(0, topicTreeElement.getRank());
                        break;
                    case "urn:topic:4":
                        assertTrue(topicTreeElement.getParentId().isPresent());
                        assertEquals(
                                topic3.getPublicId(),
                                topicTreeElement.getParentId().orElseThrow());
                        assertEquals(1, topicTreeElement.getRank());
                        break;
                    default:
                        fail();
                }

                topicsToFind.remove(topic.getPublicId().toString());
            });

            assertEquals(0, topicsToFind.size());
        }

        // Search for subtopics of topic5
        {
            final var topicElements = service.getRecursiveNodes(topic5);
            final var topicsToFind = new HashSet<>(Set.of("urn:topic:5", "urn:topic:6", "urn:topic:7", "urn:topic:8"));

            topicElements.forEach(topicTreeElement -> {
                final var topic = nodeRepository
                        .findFirstByPublicId(topicTreeElement.getId())
                        .orElseThrow();

                if (!topicsToFind.contains(topic.getPublicId().toString())) {
                    fail("Topic found is unknown or duplicated " + topic.getPublicId());
                }

                switch (topic.getPublicId().toString()) {
                    case "urn:topic:5":
                        assertFalse(topicTreeElement.getParentId().isPresent());
                        assertEquals(0, topicTreeElement.getRank());
                        break;
                    case "urn:topic:6":
                        assertTrue(topicTreeElement.getParentId().isPresent());
                        assertEquals(
                                topic5.getPublicId(),
                                topicTreeElement.getParentId().orElseThrow());
                        assertEquals(1, topicTreeElement.getRank());
                        break;
                    case "urn:topic:7":
                        assertTrue(topicTreeElement.getParentId().isPresent());
                        assertEquals(
                                topic5.getPublicId(),
                                topicTreeElement.getParentId().orElseThrow());
                        assertEquals(2, topicTreeElement.getRank());
                        break;
                    case "urn:topic:8":
                        assertTrue(topicTreeElement.getParentId().isPresent());
                        assertEquals(
                                topic5.getPublicId(),
                                topicTreeElement.getParentId().orElseThrow());
                        assertEquals(3, topicTreeElement.getRank());
                        break;
                    default:
                        fail();
                }

                topicsToFind.remove(topic.getPublicId().toString());
            });

            assertEquals(0, topicsToFind.size());
        }
    }

    @Test
    void getRecursiveTopics_with_infinite_loop() {
        // This condition should not be possible as validation when inserting should prevent it,
        // this test tests that the breaker works in case it happens, for example by manual editing.
        // Otherwise it would result in a StackOverflowError at some point

        final var topic1 = new Node(NodeType.TOPIC);
        final var topic2 = new Node(NodeType.TOPIC);
        final var topic3 = new Node(NodeType.TOPIC);
        final var topic4 = new Node(NodeType.TOPIC);

        nodeRepository.saveAll(Set.of(topic1, topic2, topic3, topic4));

        // Link it in the order of
        // → topic 1 → topic 2 ↓
        // ↑ topic 4 ← topic 3 ←

        nodeConnectionRepository.saveAll(Set.of(
                NodeConnection.create(topic1, topic2, core),
                NodeConnection.create(topic2, topic3, core),
                NodeConnection.create(topic3, topic4, core),
                NodeConnection.create(topic4, topic1, core)));

        assertThrows(IllegalStateException.class, () -> service.getRecursiveNodes(topic1));
        assertThrows(IllegalStateException.class, () -> service.getRecursiveNodes(topic2));
        assertThrows(IllegalStateException.class, () -> service.getRecursiveNodes(topic3));
        assertThrows(IllegalStateException.class, () -> service.getRecursiveNodes(topic4));
    }
}
