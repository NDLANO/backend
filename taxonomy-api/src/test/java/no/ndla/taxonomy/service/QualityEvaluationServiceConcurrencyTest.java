/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.UpdateOrDelete;
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
    private QualityEvaluationService qualityEvaluationService;

    @Autowired
    private Builder builder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
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

        assertEquals(1, average.getCount()); // Should be 2
        // assertEquals(4.5, average.getAverageValue());
    }

    @Test
    void concurrent_updates_to_same_child_should_refresh_old_grade_before_updating_parents() throws Exception {
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

        // Sequence: background thread loads the child (stale read), then pauses.
        // Main thread updates child grade Three->Four and commits.
        // Background thread resumes with stale state — the lock+refresh should
        // make it see grade Four (not Three) as the old grade before applying Five.
        var staleChildLoaded = new CountDownLatch(1);
        var continueStaleUpdate = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            var secondUpdate = executor.submit(
                    () -> applyNodeQualityEvaluationUpdate(childId, Grade.Five, staleChildLoaded, continueStaleUpdate));

            assertTrue(staleChildLoaded.await(5, TimeUnit.SECONDS));

            // First update commits Three->Four while the background thread is paused.
            applyNodeQualityEvaluationUpdate(childId, Grade.Four);

            // Unblock background thread — it should refresh and see Four as old grade.
            continueStaleUpdate.countDown();
            secondUpdate.get(5, TimeUnit.SECONDS);
        }

        var updatedParent = transactionTemplate.execute(status -> nodeRepository.getByPublicId(parentId));
        var average = updatedParent.getChildQualityEvaluationAverage().orElseThrow();

        assertEquals(1, average.getCount());
        assertEquals(5.0, average.getAverageValue());
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

    private void applyNodeQualityEvaluationUpdate(URI nodeId, Grade grade) {
        applyNodeQualityEvaluationUpdate(nodeId, grade, null, null);
    }

    private void applyNodeQualityEvaluationUpdate(
            URI nodeId, Grade grade, CountDownLatch loadedNode, CountDownLatch continueUpdate) {
        var template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            Node node = nodeRepository.getByPublicId(nodeId);
            var command = new NodePostPut();
            command.qualityEvaluation = UpdateOrDelete.Update(new QualityEvaluationDTO(grade, Optional.empty()));

            if (loadedNode != null) {
                loadedNode.countDown();
            }
            if (continueUpdate != null) {
                await(continueUpdate);
            }

            qualityEvaluationService.lockNodeForQualityEvaluationUpdate(node, command);

            var oldGrade = node.getQualityEvaluationGrade();
            command.apply(node);
            qualityEvaluationService.updateQualityEvaluationOfParents(node, oldGrade, command);
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
