/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.commands.NodePostPut;
import no.ndla.taxonomy.service.dtos.QualityEvaluationDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class QualityEvaluationService {
    private final NodeRepository nodeRepository;
    private final EntityManager entityManager;

    public QualityEvaluationService(NodeRepository nodeRepository, EntityManager entityManager) {
        this.nodeRepository = nodeRepository;
        this.entityManager = entityManager;
    }

    private boolean shouldBeIncludedInQualityEvaluationAverage(NodeType nodeType) {
        return nodeType == NodeType.RESOURCE;
    }

    private Optional<NodePostPut> getQualityEvaluationCommand(UpdatableDto<?> command) {
        if (command instanceof NodePostPut nodeCommand && nodeCommand.qualityEvaluation.isChanged()) {
            return Optional.of(nodeCommand);
        }

        return Optional.empty();
    }

    /**
     * Returns the old grade to use for a quality-evaluation update. If the command changes quality
     * evaluation, the node is pessimistically locked and refreshed first so the grade is based on the
     * latest committed row.
     */
    @Transactional
    public Optional<Grade> getOldGradeForQualityEvaluationUpdate(Node node, UpdatableDto<?> command) {
        if (getQualityEvaluationCommand(command).isPresent()) {
            entityManager.flush();
            lockAndRefresh(node);
        }

        return node.getQualityEvaluationGrade();
    }

    /**
     * Note: this clears the Hibernate persistence context as a side effect of the bulk SQL update.
     * Any entities loaded earlier in this transaction become detached.
     */
    @Transactional
    public void updateQualityEvaluationOfParents(Node node, Optional<Grade> oldGrade, UpdatableDto<?> command) {
        var nodeCommand = getQualityEvaluationCommand(command);
        if (nodeCommand.isEmpty() || !shouldBeIncludedInQualityEvaluationAverage(node.getNodeType())) {
            return;
        }

        var newGrade = nodeCommand.get().qualityEvaluation.getValue().map(QualityEvaluationDTO::getGrade);
        applyGradeDeltaToAncestors(node.getParentNodesForQualityEvaluation(), oldGrade, newGrade, true);
    }

    /**
     * Applies the (oldGrade -> newGrade) delta to every non-LINK ancestor in a single atomic SQL
     * statement. When clearPersistenceContext is true, managed entities are detached after the
     * update so subsequent reads see DB state. Pass false when the caller still needs entities
     * loaded earlier in the transaction (e.g. the freshly refreshed child in a connect/disconnect
     * flow).
     */
    private void applyGradeDeltaToAncestors(
            Collection<Node> directParents,
            Optional<Grade> oldGrade,
            Optional<Grade> newGrade,
            boolean clearPersistenceContext) {
        if (directParents.isEmpty() || (oldGrade.isEmpty() && newGrade.isEmpty()) || oldGrade.equals(newGrade)) {
            return;
        }

        var oldGradeInt = oldGrade.map(Grade::toInt).orElse(null);
        var newGradeInt = newGrade.map(Grade::toInt).orElse(null);
        int countDelta = (newGrade.isPresent() ? 1 : 0) - (oldGrade.isPresent() ? 1 : 0);
        int sumDelta = (newGradeInt == null ? 0 : newGradeInt) - (oldGradeInt == null ? 0 : oldGradeInt);

        var startIds = directParents.stream().map(Node::getId).toList();
        if (clearPersistenceContext) {
            nodeRepository.applyQualityEvaluationDeltaToAncestors(
                    startIds, oldGradeInt, newGradeInt, sumDelta, countDelta);
        } else {
            nodeRepository.applyQualityEvaluationDeltaToAncestorsWithoutClearing(
                    startIds, oldGradeInt, newGradeInt, sumDelta, countDelta);
        }
    }

    private void applyGradeAverageDeltaToAncestors(
            Collection<Node> directParents, GradeAverage gradeAverage, boolean shouldAdd) {
        if (directParents.isEmpty() || gradeAverage.getCount() == 0 || gradeAverage.getAverageSum() == 0) {
            return;
        }

        var direction = shouldAdd ? 1 : -1;
        nodeRepository.applyQualityEvaluationAverageDeltaToAncestors(
                directParents.stream().map(Node::getId).toList(),
                gradeAverage.getAverageSum() * direction,
                gradeAverage.getCount() * direction);
    }

    @Transactional
    public void updateQualityEvaluationOfNewConnection(NodeConnection connection) {
        if (connection.getConnectionType() == NodeConnectionType.LINK) {
            return;
        }

        var parent = connection.getParent().orElse(null);
        var child = connection.getChild().orElse(null);
        if (parent == null || child == null) {
            return;
        }

        entityManager.flush();
        entityManager.refresh(child);

        if (shouldBeIncludedInQualityEvaluationAverage(child.getNodeType())) {
            applyGradeDeltaToAncestors(List.of(parent), Optional.empty(), child.getQualityEvaluationGrade(), false);
        }

        child.getChildQualityEvaluationAverage().ifPresent(childAverage -> {
            applyGradeAverageDeltaToAncestors(List.of(parent), childAverage, true);
        });
    }

    @Transactional
    public void removeQualityEvaluationOfDeletedConnection(NodeConnection connectionToDelete) {
        if (connectionToDelete.getConnectionType() == NodeConnectionType.LINK) return;

        var noChild = connectionToDelete.getChild().isEmpty();
        var noParent = connectionToDelete.getParent().isEmpty();
        if (noChild || noParent) return;

        var child = connectionToDelete.getChild().get();
        var parent = connectionToDelete.getParent().get();

        entityManager.flush();
        entityManager.refresh(child);

        if (shouldBeIncludedInQualityEvaluationAverage(child.getNodeType())) {
            applyGradeDeltaToAncestors(List.of(parent), child.getQualityEvaluationGrade(), Optional.empty(), false);
            return;
        }

        if (child.getChildQualityEvaluationAverage().isEmpty()) return;
        var childAverage = child.getChildQualityEvaluationAverage().get();
        applyGradeAverageDeltaToAncestors(List.of(parent), childAverage, false);
    }

    /**
     * Note: this clears the Hibernate persistence context as a side effect of the bulk SQL update.
     * Any entities loaded earlier in this transaction become detached.
     */
    @Transactional
    public void updateQualityEvaluationOfRecursive(
            Collection<Node> parents, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        applyGradeDeltaToAncestors(parents, oldGrade, newGrade, true);
    }

    /**
     * Acquires a pessimistic write lock and refreshes the entity from the database.
     * WARNING: refresh overwrites any in-memory changes not yet flushed — call flush() first
     * if the entity (or related entities) may have been modified in this transaction.
     */
    private void lockAndRefresh(Node node) {
        entityManager.lock(node, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(node);
    }

    @Transactional
    public void updateEntireAverageTreeForNode(URI publicId) {
        var node = nodeRepository
                .findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        node.updateEntireAverageTree();
        nodeRepository.save(node);
    }

    @Transactional
    public void updateQualityEvaluationOfAllNodes() {
        nodeRepository.wipeQualityEvaluationAverages();
        try (var nodeStream = nodeRepository.findNodesWithQualityEvaluation()) {
            nodeStream.forEach(node -> {
                if (shouldBeIncludedInQualityEvaluationAverage(node.getNodeType())) {
                    applyGradeDeltaToAncestors(
                            node.getParentNodesForQualityEvaluation(),
                            Optional.empty(),
                            node.getQualityEvaluationGrade(),
                            false);
                }
            });
        }
    }
}
