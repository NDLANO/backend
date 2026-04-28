/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.Grade;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnectionType;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.UpdateOrDelete;
import no.ndla.taxonomy.integration.DraftApiClient;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.commands.NodePostPut;
import no.ndla.taxonomy.service.dtos.QualityEvaluationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class QualityEvaluationServiceConcurrencyTest extends AbstractIntegrationTest {
    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NodeConnectionRepository nodeConnectionRepository;

    @Autowired
    private QualityEvaluationService qualityEvaluationService;

    @Autowired
    private Builder builder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private NodeConnectionServiceImpl connectionService;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        connectionService = new NodeConnectionServiceImpl(
                nodeConnectionRepository,
                mock(ContextUpdaterService.class),
                nodeRepository,
                qualityEvaluationService,
                mock(DraftApiClient.class));
        nodeRepository.deleteAllAndFlush();
    }

    @Test
    void concurrent_updates_to_same_parent_should_not_lose_quality_evaluation_deltas() throws Exception {
        var parentId = transactionTemplate.execute(status -> {
            var parent =
                    builder.node(NodeType.TOPIC, node -> node.name("Parent").publicId("urn:topic:1"));
            return parent.getPublicId();
        });

        var loadedParent = new CountDownLatch(2);
        var startUpdates = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            var firstUpdate =
                    executor.submit(() -> applyQualityDelta(parentId, Grade.Five, loadedParent, startUpdates));
            var secondUpdate =
                    executor.submit(() -> applyQualityDelta(parentId, Grade.Four, loadedParent, startUpdates));

            assertTrue(loadedParent.await(5, TimeUnit.SECONDS));
            startUpdates.countDown();

            firstUpdate.get(5, TimeUnit.SECONDS);
            secondUpdate.get(5, TimeUnit.SECONDS);
        }

        var updatedParent = transactionTemplate.execute(status -> nodeRepository.getByPublicId(parentId));
        var average = updatedParent.getChildQualityEvaluationAverage().orElseThrow();

        assertEquals(2, average.getCount());
        assertEquals(4.5, average.getAverageValue());
    }

    @Test
    void concurrent_branch_connections_to_same_parent_should_not_lose_quality_evaluation_deltas() throws Exception {
        var ids = transactionTemplate.execute(status -> {
            var parent =
                    builder.node(NodeType.TOPIC, node -> node.name("Parent").publicId("urn:topic:3"));
            var firstChild = builder.node(
                    NodeType.RESOURCE,
                    node -> node.name("First child").publicId("urn:resource:3").qualityEvaluation(Grade.Five));
            var secondChild = builder.node(
                    NodeType.RESOURCE,
                    node -> node.name("Second child").publicId("urn:resource:4").qualityEvaluation(Grade.Four));

            return List.of(parent.getPublicId(), firstChild.getPublicId(), secondChild.getPublicId());
        });
        var parentId = ids.get(0);

        var loadedParent = new CountDownLatch(2);
        var startConnections = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            var firstConnection =
                    executor.submit(() -> connectChild(parentId, ids.get(1), loadedParent, startConnections));
            var secondConnection =
                    executor.submit(() -> connectChild(parentId, ids.get(2), loadedParent, startConnections));

            assertTrue(loadedParent.await(5, TimeUnit.SECONDS));
            startConnections.countDown();

            firstConnection.get(5, TimeUnit.SECONDS);
            secondConnection.get(5, TimeUnit.SECONDS);
        }

        var updatedParent = transactionTemplate.execute(status -> nodeRepository.getByPublicId(parentId));
        var average = updatedParent.getChildQualityEvaluationAverage().orElseThrow();

        assertEquals(2, average.getCount());
        assertEquals(4.5, average.getAverageValue());
    }

    @Test
    void concurrent_branch_connection_deletes_from_same_parent_should_not_lose_quality_evaluation_deltas()
            throws Exception {
        var ids = transactionTemplate.execute(status -> {
            var parent = builder.node(
                    NodeType.TOPIC,
                    node -> node.name("Parent")
                            .publicId("urn:topic:4")
                            .resource(resource -> resource.name("First child")
                                    .publicId("urn:resource:5")
                                    .qualityEvaluation(Grade.Five))
                            .resource(resource -> resource.name("Second child")
                                    .publicId("urn:resource:6")
                                    .qualityEvaluation(Grade.Four)));

            return List.of(parent.getPublicId(), URI.create("urn:resource:5"), URI.create("urn:resource:6"));
        });
        var parentId = ids.get(0);
        qualityEvaluationService.updateEntireAverageTreeForNode(parentId);

        var loadedParent = new CountDownLatch(2);
        var startDeletes = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            var firstDelete = executor.submit(() -> disconnectChild(parentId, ids.get(1), loadedParent, startDeletes));
            var secondDelete = executor.submit(() -> disconnectChild(parentId, ids.get(2), loadedParent, startDeletes));

            assertTrue(loadedParent.await(5, TimeUnit.SECONDS));
            startDeletes.countDown();

            firstDelete.get(5, TimeUnit.SECONDS);
            secondDelete.get(5, TimeUnit.SECONDS);
        }

        var updatedParent = transactionTemplate.execute(status -> nodeRepository.getByPublicId(parentId));

        assertTrue(updatedParent.getChildQualityEvaluationAverage().isEmpty());
    }

    @Test
    void concurrent_updates_to_same_child_should_load_old_grade_after_waiting_for_lock() throws Exception {
        var ids = transactionTemplate.execute(status -> {
            builder.node(
                    NodeType.TOPIC,
                    node -> node.name("Parent")
                            .publicId("urn:topic:2")
                            .child(
                                    NodeType.RESOURCE,
                                    child -> child.name("Child")
                                            .publicId("urn:resource:2")
                                            .qualityEvaluation(Grade.Three)));

            return List.of(URI.create("urn:topic:2"), URI.create("urn:resource:2"));
        });
        var parentId = ids.get(0);
        var childId = ids.get(1);

        qualityEvaluationService.updateEntireAverageTreeForNode(parentId);

        var firstUpdateLoadedChild = new CountDownLatch(1);
        var continueFirstUpdate = new CountDownLatch(1);
        var secondUpdateReadyForLock = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            var firstUpdate = executor.submit(() -> applyNodeQualityEvaluationUpdate(
                    childId, Grade.Five, null, firstUpdateLoadedChild, continueFirstUpdate));

            assertTrue(firstUpdateLoadedChild.await(5, TimeUnit.SECONDS));

            var secondUpdate = executor.submit(
                    () -> applyNodeQualityEvaluationUpdate(childId, Grade.Four, secondUpdateReadyForLock, null, null));

            assertTrue(secondUpdateReadyForLock.await(5, TimeUnit.SECONDS));
            continueFirstUpdate.countDown();

            firstUpdate.get(5, TimeUnit.SECONDS);
            secondUpdate.get(5, TimeUnit.SECONDS);
        }

        var updatedParent = transactionTemplate.execute(status -> nodeRepository.getByPublicId(parentId));
        var average = updatedParent.getChildQualityEvaluationAverage().orElseThrow();

        assertEquals(1, average.getCount());
        assertEquals(4.0, average.getAverageValue());
    }

    private void applyQualityDelta(
            URI parentId, Grade grade, CountDownLatch loadedParent, CountDownLatch startUpdates) {
        var template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            Node parent = nodeRepository.getByPublicId(parentId);
            loadedParent.countDown();
            await(startUpdates);
            qualityEvaluationService.updateQualityEvaluationOfRecursive(
                    List.of(parent), Optional.empty(), Optional.of(grade));
        });
    }

    private void connectChild(URI parentId, URI childId, CountDownLatch loadedParent, CountDownLatch startConnections) {
        var template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            Node parent = nodeRepository.getByPublicId(parentId);
            Node child = nodeRepository.getByPublicId(childId);
            loadedParent.countDown();
            await(startConnections);
            connectionService.connectParentChild(
                    parent, child, Relevance.CORE, null, Optional.empty(), NodeConnectionType.BRANCH);
        });
    }

    private void disconnectChild(URI parentId, URI childId, CountDownLatch loadedParent, CountDownLatch startDeletes) {
        var template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            Node parent = nodeRepository.getByPublicId(parentId);
            Node child = nodeRepository.getByPublicId(childId);
            loadedParent.countDown();
            await(startDeletes);
            connectionService.disconnectParentChild(parent, child);
        });
    }

    private void applyNodeQualityEvaluationUpdate(URI nodeId, Grade grade) {
        applyNodeQualityEvaluationUpdate(nodeId, grade, null, null, null);
    }

    private void applyNodeQualityEvaluationUpdate(
            URI nodeId,
            Grade grade,
            CountDownLatch readyForLock,
            CountDownLatch loadedNode,
            CountDownLatch continueUpdate) {
        var template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            var command = new NodePostPut();
            command.qualityEvaluation = UpdateOrDelete.Update(new QualityEvaluationDTO(grade, Optional.empty()));

            if (readyForLock != null) {
                readyForLock.countDown();
            }
            qualityEvaluationService.lockQualityEvaluationIfNeeded(command);

            var node = nodeRepository.getByPublicId(nodeId);
            if (loadedNode != null) {
                loadedNode.countDown();
            }
            if (continueUpdate != null) {
                await(continueUpdate);
            }

            var oldGrade = node.getQualityEvaluationGrade();
            command.apply(node);
            qualityEvaluationService.updateQualityEvaluationOfParentsFromFreshlyLoadedNode(node, oldGrade, command);
        });
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
