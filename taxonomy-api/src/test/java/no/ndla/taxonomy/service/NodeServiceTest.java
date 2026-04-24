/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static no.ndla.taxonomy.TestUtils.assertAnyTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
public class NodeServiceTest extends AbstractIntegrationTest {
    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private Builder builder;

    @MockitoBean
    private NodeConnectionService nodeConnectionService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private SearchService searchService;

    @BeforeEach
    void clearAllRepos() {
        nodeRepository.deleteAllAndFlush();
    }

    @Test
    public void delete() {
        final var createdTopic = builder.node(n -> n.nodeType(NodeType.TOPIC));
        final var topicId = createdTopic.getPublicId();
        nodeService.delete(topicId);

        assertFalse(nodeRepository.findFirstByPublicId(topicId).isPresent());
        verify(nodeConnectionService).disconnectAllParents(createdTopic);
        verify(nodeConnectionService).disconnectAllChildren(createdTopic);
    }

    @Test
    public void getAllConnections() {
        final var topicId = builder.node(n -> n.nodeType(NodeType.TOPIC)).getPublicId();

        final var subjectTopic = mock(NodeConnection.class);
        final var parentTopicSubtopic = mock(NodeConnection.class);
        final var childTopicSubtopic = mock(NodeConnection.class);

        when(subjectTopic.getPublicId()).thenReturn(URI.create("urn:subject-topic"));
        when(subjectTopic.getConnectionType()).thenReturn(NodeConnectionType.BRANCH);
        when(parentTopicSubtopic.getPublicId()).thenReturn(URI.create("urn:parent-topic"));
        when(parentTopicSubtopic.getConnectionType()).thenReturn(NodeConnectionType.BRANCH);
        when(childTopicSubtopic.getPublicId()).thenReturn(URI.create("urn:child-topic"));
        when(childTopicSubtopic.getConnectionType()).thenReturn(NodeConnectionType.BRANCH);

        final var parentConnectionsToReturn = Set.of(subjectTopic);
        final var childConnectionsToReturn = Set.of(childTopicSubtopic);

        when(nodeConnectionService.getParentConnections(any(Node.class))).thenAnswer(invocationOnMock -> {
            final var topic = (Node) invocationOnMock.getArgument(0);
            assertEquals(topicId, topic.getPublicId());

            return parentConnectionsToReturn;
        });
        when(nodeConnectionService.getChildConnections(any(Node.class))).thenAnswer(invocationOnMock -> {
            final var topic = (Node) invocationOnMock.getArgument(0);
            assertEquals(topicId, topic.getPublicId());

            return childConnectionsToReturn;
        });

        final var returnedConnections = nodeService.getAllConnections(topicId);

        assertEquals(2, returnedConnections.size());
        returnedConnections.forEach(connection -> {
            if (connection.getConnectionId().equals(URI.create("urn:subject-topic"))) {
                assertEquals("parent", connection.getType());
            } else if (connection.getConnectionId().equals(URI.create("urn:child-topic"))) {
                assertEquals("child", connection.getType());
            } else {
                fail();
            }
        });
    }

    @Test
    public void allSearch() {
        builder.node(n -> n.nodeType(NodeType.TOPIC));
        builder.node(n -> n.nodeType(NodeType.TOPIC));
        builder.node(n -> n.nodeType(NodeType.TOPIC));
        builder.node(n -> n.nodeType(NodeType.TOPIC));
        var subject = builder.node(n -> n.nodeType(NodeType.SUBJECT));

        var subjects = searchService.searchByNodeType(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "",
                true,
                false,
                10,
                1,
                Optional.of(List.of(NodeType.SUBJECT)),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        var topics = searchService.searchByNodeType(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "",
                true,
                false,
                10,
                1,
                Optional.of(List.of(NodeType.TOPIC)),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        var all = searchService.searchByNodeType(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "",
                true,
                false,
                10,
                1,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertEquals(subjects.getResults().getFirst().getId(), subject.getPublicId());

        assertEquals(1, subjects.getTotalCount());
        assertEquals(4, topics.getTotalCount());
        assertEquals(5, all.getTotalCount());
    }

    @Test
    public void querySearchWorks() {
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Apekatt"));
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Katt"));
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Hund"));
        var tiger = builder.node(n -> n.nodeType(NodeType.TOPIC).name("Tiger"));

        var result = searchService.search(
                Optional.of("tiger"),
                Optional.empty(),
                Optional.empty(),
                "",
                true,
                false,
                10,
                1,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertEquals(result.getResults().getFirst().getId(), tiger.getPublicId());
        assertEquals(1, result.getTotalCount());
    }

    @Test
    public void idsAndQuerySearch() throws URISyntaxException {
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Apekatt").publicId("urn:topic:1"));
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Katt").publicId("urn:topic:2"));
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Hund").publicId("urn:topic:3"));
        builder.node(n -> n.nodeType(NodeType.TOPIC).name("Tiger").publicId("urn:topic:4"));

        var idList = new ArrayList<String>();
        idList.add("urn:topic:1");
        idList.add("urn:topic:2");

        var result = searchService.search(
                Optional.empty(),
                Optional.of(idList),
                Optional.empty(),
                "",
                true,
                false,
                10,
                1,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertEquals(new URI("urn:topic:1"), result.getResults().get(0).getId());
        assertEquals(new URI("urn:topic:2"), result.getResults().get(1).getId());
        assertEquals(2, result.getTotalCount());

        var result2 = searchService.search(
                Optional.of("Ape"),
                Optional.of(idList),
                Optional.empty(),
                "",
                true,
                false,
                10,
                1,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertEquals(new URI("urn:topic:1"), result2.getResults().getFirst().getId());
        assertEquals(1, result2.getTotalCount());
    }

    @Test
    public void contentUriSearch() {
        builder.node(n -> n.nodeType(NodeType.TOPIC)
                .name("Apekatt")
                .publicId("urn:topic:1")
                .contentUri("urn:article:1"));
        builder.node(n ->
                n.nodeType(NodeType.TOPIC).name("Katt").publicId("urn:topic:2").contentUri("urn:article:2"));
        builder.node(n ->
                n.nodeType(NodeType.TOPIC).name("Hund").publicId("urn:topic:3").contentUri("urn:article:3"));
        builder.node(n ->
                n.nodeType(NodeType.TOPIC).name("Tiger").publicId("urn:topic:4").contentUri("urn:article:1"));

        var idList = new ArrayList<String>();
        idList.add("urn:article:1");

        var result = searchService.search(
                Optional.empty(),
                Optional.empty(),
                Optional.of(idList),
                "",
                true,
                false,
                10,
                1,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertAnyTrue(result.getResults(), res -> res.getId().equals(URI.create("urn:topic:1")));
        assertAnyTrue(result.getResults(), res -> res.getId().equals(URI.create("urn:topic:4")));
        assertEquals(2, result.getTotalCount());
    }
}
