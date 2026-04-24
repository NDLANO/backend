/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.Set;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.integration.DraftApiClient;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
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
public class NodeConnectionServiceImplTest extends AbstractIntegrationTest {
    @Autowired
    private NodeConnectionRepository nodeConnectionRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EntityManager entityManager;

    private ContextUpdaterService contextUpdaterService;

    private NodeConnectionServiceImpl service;

    @Autowired
    private QualityEvaluationService qualityEvaluationService;

    @Autowired
    private Builder builder;

    private DraftApiClient draftApiClient;

    @BeforeEach
    public void setUp() throws Exception {
        contextUpdaterService = mock(ContextUpdaterService.class);
        draftApiClient = mock(DraftApiClient.class);

        service = new NodeConnectionServiceImpl(
                nodeConnectionRepository,
                contextUpdaterService,
                nodeRepository,
                qualityEvaluationService,
                draftApiClient);
    }

    private void refresh(Node node) {
        entityManager.flush();
        entityManager.refresh(node);
    }

    private void assertQualityEvaluationAverage(Node node, double expectedAverage, int expectedCount) {
        var average = node.getChildQualityEvaluationAverage().orElseThrow();
        assertEquals(expectedCount, average.getCount());
        assertEquals(expectedAverage, average.getAverageValue());
    }

    @Test
    public void connectTopicSubtopic() {
        final var topic1 = builder.node(NodeType.TOPIC);
        final var topic2 = builder.node(NodeType.TOPIC);
        final var topic4 = builder.node(NodeType.TOPIC);
        final var topic5 = builder.node(NodeType.TOPIC);
        final var topic6 = builder.node(NodeType.TOPIC);
        final var topic7 = builder.node(NodeType.TOPIC);
        final var topic8 = builder.node(NodeType.TOPIC);
        final var topic9 = builder.node(NodeType.TOPIC);

        final var relevance = Relevance.CORE;

        final var connection1 = service.connectParentChild(topic2, topic1, relevance, 1);
        assertNotNull(connection1);
        assertNotNull(connection1.getId());
        assertEquals(1, connection1.getRank());
        assertSame(topic2, connection1.getParent().orElse(null));
        assertSame(topic1, connection1.getChild().orElse(null));

        verify(contextUpdaterService, atLeastOnce()).updateContexts(topic1);

        // Test ranking
        final var connection4 = service.connectParentChild(topic4, topic5, relevance, null);
        assertEquals(1, connection4.getRank());

        final var connection5 = service.connectParentChild(topic4, topic6, relevance, null);
        assertEquals(1, connection4.getRank());
        assertEquals(2, connection5.getRank());

        final var connection6 = service.connectParentChild(topic4, topic7, relevance, 1);
        assertEquals(2, connection4.getRank());
        assertEquals(3, connection5.getRank());
        assertEquals(1, connection6.getRank());

        final var connection7 = service.connectParentChild(topic4, topic8, relevance, 3);
        assertEquals(2, connection4.getRank());
        assertEquals(4, connection5.getRank());
        assertEquals(1, connection6.getRank());
        assertEquals(3, connection7.getRank());

        final var connection8 = service.connectParentChild(topic4, topic9, relevance, 5);
        assertEquals(2, connection4.getRank());
        assertEquals(4, connection5.getRank());
        assertEquals(1, connection6.getRank());
        assertEquals(3, connection7.getRank());
        assertEquals(5, connection8.getRank());

        try {
            service.connectParentChild(topic4, topic8, relevance, 0);
        } catch (DuplicateConnectionException ignored) {

        }

        // try to create loops
        final var topic10 = builder.node(NodeType.TOPIC);
        final var topic11 = builder.node(NodeType.TOPIC);
        final var topic12 = builder.node(NodeType.TOPIC);

        try {
            service.connectParentChild(topic10, topic10, relevance, 0);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        service.connectParentChild(topic10, topic11, relevance, 0);
        service.connectParentChild(topic11, topic12, relevance, 0);

        try {
            service.connectParentChild(topic12, topic10, relevance, 0);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        try {
            service.connectParentChild(topic11, topic10, relevance, 0);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        try {
            service.connectParentChild(topic12, topic11, relevance, 0);
            fail("Expected DuplicateConnectionException");
        } catch (DuplicateConnectionException ignored) {

        }

        try {
            service.connectParentChild(topic11, topic10, relevance, 0);
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }
    }

    @Test
    public void connectTopicResource() {
        final var topic1 = builder.node(NodeType.TOPIC);
        final var topic2 = builder.node(NodeType.TOPIC);
        final var topic3 = builder.node(NodeType.TOPIC);
        final var topic4 = builder.node(NodeType.TOPIC);

        final var resource1 = builder.node(NodeType.RESOURCE);
        final var resource2 = builder.node(NodeType.RESOURCE);
        final var resource3 = builder.node(NodeType.RESOURCE);
        final var resource4 = builder.node(NodeType.RESOURCE);
        final var resource5 = builder.node(NodeType.RESOURCE);
        final var resource6 = builder.node(NodeType.RESOURCE);
        final var resource7 = builder.node(NodeType.RESOURCE);

        final var relevance = Relevance.CORE;

        final var connection1 = service.connectParentChild(
                topic1, resource1, relevance, null, Optional.of(true), NodeConnectionType.BRANCH);
        assertNotNull(connection1);
        assertSame(topic1, connection1.getParent().orElse(null));
        assertSame(resource1, connection1.getResource().orElse(null));
        assertTrue(connection1.isPrimary().orElseThrow());
        assertEquals(1, connection1.getRank());

        verify(contextUpdaterService, atLeastOnce()).updateContexts(resource1);

        final var connection2 = service.connectParentChild(
                topic1, resource2, relevance, null, Optional.of(true), NodeConnectionType.BRANCH);
        assertNotNull(connection2);
        assertSame(topic1, connection2.getParent().orElse(null));
        assertSame(resource2, connection2.getResource().orElse(null));
        assertTrue(connection2.isPrimary().orElseThrow());
        assertEquals(2, connection2.getRank());

        assertTrue(connection1.isPrimary().orElseThrow());
        assertEquals(1, connection1.getRank());

        final var connection3 = service.connectParentChild(
                topic2, resource2, relevance, null, Optional.of(false), NodeConnectionType.BRANCH);
        assertFalse(connection3.isPrimary().orElseThrow());
        assertEquals(1, connection3.getRank());

        // Has not changed the first connection since setting this to non-primary
        assertTrue(connection2.isPrimary().orElseThrow());

        // Test setting primary, should set old primary to non-primary
        final var connection4 = service.connectParentChild(
                topic3, resource2, relevance, null, Optional.of(true), NodeConnectionType.BRANCH);
        assertTrue(connection4.isPrimary().orElseThrow());
        assertEquals(1, connection4.getRank());

        assertFalse(connection3.isPrimary().orElseThrow());
        assertFalse(connection2.isPrimary().orElseThrow());

        // Test ranking
        final var connection5 = service.connectParentChild(
                topic4, resource1, relevance, null, Optional.of(true), NodeConnectionType.BRANCH);
        assertEquals(1, connection5.getRank());

        final var connection6 = service.connectParentChild(
                topic4, resource2, relevance, null, Optional.of(true), NodeConnectionType.BRANCH);
        assertEquals(1, connection5.getRank());
        assertEquals(2, connection6.getRank());

        final var connection7 = service.connectParentChild(
                topic4, resource3, relevance, 1, Optional.of(true), NodeConnectionType.BRANCH);
        assertEquals(2, connection5.getRank());
        assertEquals(3, connection6.getRank());
        assertEquals(1, connection7.getRank());

        final var connection8 = service.connectParentChild(
                topic4, resource4, relevance, 2, Optional.of(true), NodeConnectionType.BRANCH);
        assertEquals(3, connection5.getRank());
        assertEquals(4, connection6.getRank());
        assertEquals(1, connection7.getRank());
        assertEquals(2, connection8.getRank());

        final var connection9 = service.connectParentChild(
                topic4, resource5, relevance, 5, Optional.of(true), NodeConnectionType.BRANCH);
        assertEquals(3, connection5.getRank());
        assertEquals(4, connection6.getRank());
        assertEquals(1, connection7.getRank());
        assertEquals(2, connection8.getRank());
        assertEquals(5, connection9.getRank());

        // First topic connection for a resource will be primary regardless of request
        final var forcedPrimaryConnection1 = service.connectParentChild(
                topic4, resource6, relevance, 0, Optional.of(false), NodeConnectionType.BRANCH);
        assertTrue(forcedPrimaryConnection1.isPrimary().orElseThrow());
        final var forcedPrimaryConnection2 = service.connectParentChild(
                topic4, resource7, relevance, 1, Optional.of(false), NodeConnectionType.BRANCH);
        assertTrue(forcedPrimaryConnection2.isPrimary().orElseThrow());

        // Trying to add duplicate connection
        try {
            service.connectParentChild(topic4, resource4, relevance, 0, Optional.of(false), NodeConnectionType.BRANCH);
            fail("Expected DuplicateConnectionException");
        } catch (DuplicateConnectionException ignored) {
        }
    }

    @Test
    public void disconnectTopicSubtopic() {
        final var topic1 = builder.node(NodeType.TOPIC);
        final var subtopic1 = builder.node(NodeType.TOPIC);
        final var subtopic2 = builder.node(NodeType.TOPIC);
        final var subtopic3 = builder.node(NodeType.TOPIC);
        final var relevance = Relevance.CORE;

        final var topic1subtopic1 = NodeConnection.create(topic1, subtopic1, relevance);
        final var topic1subtopic2 = NodeConnection.create(topic1, subtopic2, relevance);
        final var topic1subtopic3 = NodeConnection.create(topic1, subtopic3, relevance);

        // Just verifies the pre-conditions of the created objects that is used for the test
        assertTrue(topic1.getChildConnections().containsAll(Set.of(topic1subtopic1, topic1subtopic2, topic1subtopic3)));
        assertSame(
                topic1subtopic1,
                subtopic1.getParentConnections().stream().findFirst().orElseThrow());
        assertSame(
                topic1subtopic2,
                subtopic2.getParentConnections().stream().findFirst().orElseThrow());
        assertSame(
                topic1subtopic3,
                subtopic3.getParentConnections().stream().findFirst().orElseThrow());
        assertEquals(3, topic1.getChildConnections().size());

        reset(contextUpdaterService);

        service.disconnectParentChildConnection(topic1subtopic1);

        verify(contextUpdaterService).updateContexts(subtopic1);

        assertFalse(topic1subtopic1.getParent().isPresent());
        assertFalse(topic1subtopic1.getChild().isPresent());
        assertFalse(topic1.getChildConnections().contains(topic1subtopic1));
        assertEquals(2, topic1.getChildConnections().size());
        assertFalse(subtopic1.getParentConnections().stream().findFirst().isPresent());
        assertSame(
                topic1subtopic2,
                subtopic2.getParentConnections().stream().findFirst().orElseThrow());
        assertSame(
                topic1subtopic3,
                subtopic3.getParentConnections().stream().findFirst().orElseThrow());

        service.disconnectParentChild(topic1, subtopic2);
        assertFalse(topic1subtopic2.getParent().isPresent());
        assertFalse(topic1subtopic2.getChild().isPresent());
        assertFalse(topic1.getChildConnections().contains(topic1subtopic2));
        assertEquals(1, topic1.getChildConnections().size());
        assertFalse(subtopic1.getParentConnections().stream().findFirst().isPresent());
        assertFalse(subtopic2.getParentConnections().stream().findFirst().isPresent());
        assertSame(
                topic1subtopic3,
                subtopic3.getParentConnections().stream().findFirst().orElseThrow());
    }

    @Test
    public void disconnectTopicResource() {
        final var topic1 = builder.node(NodeType.TOPIC);
        final var topic2 = builder.node(NodeType.TOPIC);
        final var topic3 = builder.node(NodeType.TOPIC);

        final var resource1 = builder.node(NodeType.RESOURCE);
        final var resource2 = builder.node(NodeType.RESOURCE);
        final var resource3 = builder.node(NodeType.RESOURCE);

        final var relevance = Relevance.CORE;

        final var topic1resource1 = NodeConnection.create(topic1, resource1, relevance, true);
        final var topic1resource2 = NodeConnection.create(topic1, resource2, relevance, true);
        final var topic1resource3 = NodeConnection.create(topic1, resource3, relevance, true);

        final var topic2resource1 = NodeConnection.create(topic2, resource1, relevance, false);
        final var topic3resource1 = NodeConnection.create(topic3, resource1, relevance, false);

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());
        assertFalse(topic3resource1.isPrimary().orElseThrow());

        assertTrue(topic1.getResourceChildren().contains(topic1resource1));
        assertTrue(resource1.getParentConnections().contains(topic1resource1));

        reset(contextUpdaterService);

        service.disconnectParentChild(topic1, resource1);

        verify(contextUpdaterService, atLeastOnce()).updateContexts(resource1);

        assertTrue(topic2resource1.isPrimary().orElseThrow()
                ^ topic3resource1.isPrimary().orElseThrow());
        assertFalse(topic1resource1.getResource().isPresent());
        assertFalse(topic1resource1.getParent().isPresent());
        assertFalse(topic1.getResourceChildren().contains(topic1resource1));
        assertFalse(resource1.getParentConnections().contains(topic1resource1));

        service.disconnectParentChildConnection(topic2resource1);
        assertTrue(topic3resource1.isPrimary().orElseThrow());

        assertTrue(resource2.getParentConnections().contains(topic1resource2));
        assertTrue(resource3.getParentConnections().contains(topic1resource3));

        service.disconnectParentChildConnection(topic1resource2);
        service.disconnectParentChildConnection(topic1resource3);

        assertFalse(resource2.getParentConnections().contains(topic1resource2));
        assertFalse(resource3.getParentConnections().contains(topic1resource3));
    }

    @Test
    public void updateTopicSubtopic() {
        final var rootTopic1 = builder.node(NodeType.TOPIC);

        final var subTopic1 = builder.node(NodeType.TOPIC);

        final var relevance = Relevance.CORE;

        final var connection1 = NodeConnection.create(rootTopic1, subTopic1, relevance);

        connection1.setRank(1);

        assertEquals(1, connection1.getRank());

        service.updateParentChild(connection1, relevance, Optional.of(2), Optional.empty());
        assertEquals(2, connection1.getRank());
    }

    @Test
    public void updateTopicResource() {
        final var topic1 = builder.node(NodeType.TOPIC);
        final var topic2 = builder.node(NodeType.TOPIC);

        final var resource1 = builder.node(NodeType.RESOURCE);
        final var resource2 = builder.node(NodeType.RESOURCE);
        final var resource3 = builder.node(NodeType.RESOURCE);

        final var relevance = Relevance.CORE;

        final var topic1resource1 = NodeConnection.create(topic1, resource1, relevance, true);
        final var topic1resource2 = NodeConnection.create(topic1, resource2, relevance, true);
        final var topic1resource3 = NodeConnection.create(topic1, resource3, relevance, true);

        final var topic2resource1 = NodeConnection.create(topic2, resource1, relevance, false);

        topic1resource1.setRank(1);
        topic1resource2.setRank(2);
        topic1resource3.setRank(3);

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());

        service.updateParentChild(topic2resource1, relevance, Optional.empty(), Optional.of(true));

        assertFalse(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertTrue(topic2resource1.isPrimary().orElseThrow());

        service.updateParentChild(topic2resource1, relevance, Optional.empty(), Optional.of(false));

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic1resource2.isPrimary().orElseThrow());
        assertTrue(topic1resource3.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());

        try {
            service.updateParentChild(topic1resource3, relevance, Optional.empty(), Optional.of(false));
            fail("Expected InvalidArgumentServiceException");
        } catch (InvalidArgumentServiceException ignored) {

        }

        assertEquals(1, topic1resource1.getRank());
        assertEquals(2, topic1resource2.getRank());
        assertEquals(3, topic1resource3.getRank());
        service.updateParentChild(topic1resource3, relevance, Optional.of(1), Optional.of(true));
        assertEquals(2, topic1resource1.getRank());
        assertEquals(3, topic1resource2.getRank());
        assertEquals(1, topic1resource3.getRank());

        service.updateParentChild(topic1resource2, relevance, Optional.of(2), Optional.of(true));
        assertEquals(3, topic1resource1.getRank());
        assertEquals(2, topic1resource2.getRank());
        assertEquals(1, topic1resource3.getRank());
    }

    @Test
    public void replacePrimaryConnectionsFor() {
        final var subject1 = builder.node(NodeType.SUBJECT);

        final var topic1 = builder.node(NodeType.TOPIC);
        final var topic2 = builder.node(NodeType.TOPIC);

        final var resource1 = builder.node(NodeType.RESOURCE);

        final var relevance = Relevance.CORE;

        NodeConnection.create(subject1, topic1, relevance);

        final var topic1resource1 = NodeConnection.create(topic1, resource1, relevance, true);
        final var topic2resource1 = NodeConnection.create(topic2, resource1, relevance, false);

        assertTrue(topic1resource1.isPrimary().orElseThrow());
        assertFalse(topic2resource1.isPrimary().orElseThrow());

        service.replacePrimaryConnectionsFor(topic1);

        assertFalse(topic1resource1.isPrimary().orElseThrow());
        assertTrue(topic2resource1.isPrimary().orElseThrow());
    }

    @Test
    public void add_both_child_and_link_to_same_topic() throws Exception {
        final var subject = builder.node(NodeType.SUBJECT);
        final var topic = builder.node(NodeType.TOPIC);
        {
            var c1 = service.connectParentChild(
                    subject, topic, Relevance.CORE, 1, Optional.of(true), NodeConnectionType.BRANCH);
            assertTrue(c1.isPrimary().orElseThrow());
            var c2 = service.connectParentChild(
                    subject, topic, Relevance.CORE, 2, Optional.of(true), NodeConnectionType.LINK);
            assertTrue(c2.isPrimary().orElseThrow());
        }
        final var subject2 = builder.node(NodeType.SUBJECT);
        {
            // Two connections to the same topic with same type should fail
            try {
                service.connectParentChild(
                        subject2, topic, Relevance.CORE, 2, Optional.of(true), NodeConnectionType.BRANCH);
                fail("Expected DuplicateConnectionException");
            } catch (DuplicateConnectionException ignored) {
            }
            var c1 = service.connectParentChild(
                    subject2, topic, Relevance.CORE, 1, Optional.of(true), NodeConnectionType.LINK);
            assertTrue(c1.isPrimary().orElseThrow());
        }
        final var subject3 = builder.node(NodeType.SUBJECT);
        {
            // A topic can have more than one link connection
            var c1 = service.connectParentChild(
                    subject3, topic, Relevance.CORE, 1, Optional.of(true), NodeConnectionType.LINK);
            assertTrue(c1.isPrimary().orElseThrow());
        }
    }

    @Test
    public void branch_resource_connections_update_quality_evaluation_for_parent_tree() {
        final var subject = builder.node(NodeType.SUBJECT);
        final var topic = builder.node(NodeType.TOPIC);
        final var resource = builder.node(NodeType.RESOURCE, node -> node.qualityEvaluation(Grade.Five));

        service.connectParentChild(subject, topic, Relevance.CORE, 1, Optional.of(true), NodeConnectionType.BRANCH);
        var resourceConnection = service.connectParentChild(
                topic, resource, Relevance.CORE, 1, Optional.of(true), NodeConnectionType.BRANCH);

        refresh(topic);
        refresh(subject);

        assertQualityEvaluationAverage(topic, 5.0, 1);
        assertQualityEvaluationAverage(subject, 5.0, 1);

        service.disconnectParentChildConnection(resourceConnection);

        refresh(topic);
        refresh(subject);

        assertTrue(topic.getChildQualityEvaluationAverage().isEmpty());
        assertTrue(subject.getChildQualityEvaluationAverage().isEmpty());
    }

    @Test
    public void branch_topic_connections_update_quality_evaluation_for_parent_tree() {
        final var subject = builder.node(NodeType.SUBJECT);
        final var parentTopic = builder.node(NodeType.TOPIC);
        final var childTopic = builder.node(NodeType.TOPIC);
        final var firstResource = builder.node(NodeType.RESOURCE, node -> node.qualityEvaluation(Grade.Five));
        final var secondResource = builder.node(NodeType.RESOURCE, node -> node.qualityEvaluation(Grade.Three));

        service.connectParentChild(
                subject, parentTopic, Relevance.CORE, 1, Optional.of(true), NodeConnectionType.BRANCH);
        service.connectParentChild(
                childTopic, firstResource, Relevance.CORE, 1, Optional.of(true), NodeConnectionType.BRANCH);
        service.connectParentChild(
                childTopic, secondResource, Relevance.CORE, 2, Optional.of(true), NodeConnectionType.BRANCH);

        refresh(childTopic);
        assertQualityEvaluationAverage(childTopic, 4.0, 2);

        var topicConnection = service.connectParentChild(
                parentTopic, childTopic, Relevance.CORE, 1, Optional.of(true), NodeConnectionType.BRANCH);

        refresh(parentTopic);
        refresh(subject);

        assertQualityEvaluationAverage(parentTopic, 4.0, 2);
        assertQualityEvaluationAverage(subject, 4.0, 2);

        service.disconnectParentChildConnection(topicConnection);

        refresh(parentTopic);
        refresh(subject);

        assertTrue(parentTopic.getChildQualityEvaluationAverage().isEmpty());
        assertTrue(subject.getChildQualityEvaluationAverage().isEmpty());
    }

    @Test
    public void link_connections_do_not_affect_quality_evaluation() {
        final var topic = builder.node(NodeType.TOPIC);
        final var branchResource = builder.node(NodeType.RESOURCE, resource -> resource.qualityEvaluation(Grade.Five));
        final var linkedResource = builder.node(NodeType.RESOURCE, resource -> resource.qualityEvaluation(Grade.One));

        service.connectParentChild(
                topic, branchResource, Relevance.CORE, 1, Optional.of(true), NodeConnectionType.BRANCH);

        refresh(topic);

        assertTrue(topic.getChildQualityEvaluationAverage().isPresent());
        assertEquals(1, topic.getChildQualityEvaluationAverage().orElseThrow().getCount());
        assertEquals(5, topic.getChildQualityEvaluationAverage().orElseThrow().getAverageValue());

        var linkConnection = service.connectParentChild(
                topic, linkedResource, Relevance.CORE, 2, Optional.of(false), NodeConnectionType.LINK);

        refresh(topic);

        assertTrue(topic.getChildQualityEvaluationAverage().isPresent());
        assertEquals(1, topic.getChildQualityEvaluationAverage().orElseThrow().getCount());
        assertEquals(5, topic.getChildQualityEvaluationAverage().orElseThrow().getAverageValue());

        service.disconnectParentChildConnection(linkConnection);

        refresh(topic);

        assertTrue(topic.getChildQualityEvaluationAverage().isPresent());
        assertEquals(1, topic.getChildQualityEvaluationAverage().orElseThrow().getCount());
        assertEquals(5, topic.getChildQualityEvaluationAverage().orElseThrow().getAverageValue());
    }
}
