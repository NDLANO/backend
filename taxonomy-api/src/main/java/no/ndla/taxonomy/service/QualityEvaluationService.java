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
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class QualityEvaluationService {
    private final NodeRepository nodeRepository;
    private final EntityManager entityManager;

    private enum PersistenceContextMode {
        CLEAR,
        KEEP
    }

    private enum DeltaDirection {
        ADD(1),
        REMOVE(-1);

        private final int multiplier;

        DeltaDirection(int multiplier) {
            this.multiplier = multiplier;
        }

        private int applyTo(int value) {
            return value * multiplier;
        }
    }

    public record QualityEvaluationUpdateState(NodeType oldNodeType, Optional<Grade> oldGrade) {}

    public QualityEvaluationService(NodeRepository nodeRepository, EntityManager entityManager) {
        this.nodeRepository = nodeRepository;
        this.entityManager = entityManager;
    }

    private boolean shouldBeIncludedInQualityEvaluationAverage(NodeType nodeType) {
        return nodeType == NodeType.RESOURCE;
    }

    private boolean shouldRefreshBeforeQualityEvaluationUpdate(UpdatableDto<?> command) {
        return command instanceof NodePostPut nodeCommand
                && (nodeCommand.qualityEvaluation.isChanged() || nodeCommand.nodeType != null);
    }

    /**
     * Returns the old quality-evaluation state to use for an update. If the command can affect
     * quality evaluation, the node is pessimistically locked and refreshed first so the state is
     * based on the latest committed row.
     */
    @Transactional
    public QualityEvaluationUpdateState getQualityEvaluationUpdateState(Node node, UpdatableDto<?> command) {
        if (shouldRefreshBeforeQualityEvaluationUpdate(command)) {
            entityManager.flush();
            lockAndRefresh(node);
        }

        return new QualityEvaluationUpdateState(node.getNodeType(), node.getQualityEvaluationGrade());
    }

    public QualityEvaluationUpdateState getCurrentQualityEvaluationUpdateState(Node node) {
        return new QualityEvaluationUpdateState(node.getNodeType(), node.getQualityEvaluationGrade());
    }

    private Optional<Grade> gradeIncludedInAverage(NodeType nodeType, Optional<Grade> grade) {
        return shouldBeIncludedInQualityEvaluationAverage(nodeType) ? grade : Optional.empty();
    }

    /**
     * Note: this clears the Hibernate persistence context as a side effect of the bulk SQL update.
     * Any entities loaded earlier in this transaction become detached.
     */
    @Transactional
    public void updateQualityEvaluationOfParents(
            Node node, QualityEvaluationUpdateState oldState, UpdatableDto<?> command) {
        if (!(command instanceof NodePostPut nodeCommand)) {
            return;
        }

        var qualityEvaluationChanged = nodeCommand.qualityEvaluation.isChanged();
        var nodeTypeChanged = nodeCommand.nodeType != null && nodeCommand.nodeType != oldState.oldNodeType();
        if (!qualityEvaluationChanged && !nodeTypeChanged) {
            return;
        }

        var oldGrade = gradeIncludedInAverage(oldState.oldNodeType(), oldState.oldGrade());
        var newGrade = gradeIncludedInAverage(node.getNodeType(), node.getQualityEvaluationGrade());
        propagateResourceGradeDeltaToAncestors(
                node.getParentNodesForQualityEvaluation(), oldGrade, newGrade, PersistenceContextMode.CLEAR);
    }

    /**
     * Applies the (oldGrade -> newGrade) delta to every non-LINK ancestor in a single atomic SQL
     * statement. Use CLEAR when managed entities should be detached after the update so subsequent
     * reads see DB state. Use KEEP when the caller still needs entities loaded earlier in the
     * transaction, e.g. the freshly refreshed child in a connect/disconnect flow.
     */
    private void propagateResourceGradeDeltaToAncestors(
            Collection<Node> directParents,
            Optional<Grade> oldGrade,
            Optional<Grade> newGrade,
            PersistenceContextMode persistenceContextMode) {
        if (directParents.isEmpty() || (oldGrade.isEmpty() && newGrade.isEmpty()) || oldGrade.equals(newGrade)) {
            return;
        }

        var oldGradeInt = oldGrade.map(Grade::toInt).orElse(null);
        var newGradeInt = newGrade.map(Grade::toInt).orElse(null);
        int countDelta = (newGrade.isPresent() ? 1 : 0) - (oldGrade.isPresent() ? 1 : 0);
        int sumDelta = (newGradeInt == null ? 0 : newGradeInt) - (oldGradeInt == null ? 0 : oldGradeInt);

        var startIds = directParents.stream().map(Node::getId).toList();
        if (persistenceContextMode == PersistenceContextMode.CLEAR) {
            nodeRepository.applyQualityEvaluationDeltaToAncestors(
                    startIds, oldGradeInt, newGradeInt, sumDelta, countDelta);
        } else {
            nodeRepository.applyQualityEvaluationDeltaToAncestorsWithoutClearing(
                    startIds, oldGradeInt, newGradeInt, sumDelta, countDelta);
        }
    }

    private void propagateSubtreeAverageDeltaToAncestors(
            Collection<Node> directParents, GradeAverage gradeAverage, DeltaDirection direction) {
        if (directParents.isEmpty() || gradeAverage.getCount() == 0 || gradeAverage.getAverageSum() == 0) {
            return;
        }

        nodeRepository.applyQualityEvaluationAverageDeltaToAncestors(
                directParents.stream().map(Node::getId).toList(),
                direction.applyTo(gradeAverage.getAverageSum()),
                direction.applyTo(gradeAverage.getCount()));
    }

    @Transactional
    public void propagateQualityEvaluationForAddedConnection(NodeConnection connection) {
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

        propagateConnectionDelta(parent, child, DeltaDirection.ADD);
    }

    @Transactional
    public void propagateQualityEvaluationForRemovedConnection(NodeConnection connectionToDelete) {
        if (connectionToDelete.getConnectionType() == NodeConnectionType.LINK) return;

        var noChild = connectionToDelete.getChild().isEmpty();
        var noParent = connectionToDelete.getParent().isEmpty();
        if (noChild || noParent) return;

        var child = connectionToDelete.getChild().get();
        var parent = connectionToDelete.getParent().get();

        entityManager.flush();
        entityManager.refresh(child);

        propagateConnectionDelta(parent, child, DeltaDirection.REMOVE);
    }

    /**
     * Note: leaves {@code parent} with stale child_quality_evaluation_* values in memory. The DB
     * row is updated, but the managed entity is not refreshed (KEEP mode preserves child/parent
     * for the rest of the transaction). Callers that observe parent's quality fields after this
     * call must re-read or refresh. Current call sites only return the connection.
     */
    private void propagateConnectionDelta(Node parent, Node child, DeltaDirection direction) {
        if (shouldBeIncludedInQualityEvaluationAverage(child.getNodeType())) {
            propagateResourceConnectionDelta(parent, child, direction);
            if (direction == DeltaDirection.REMOVE) return;
        }

        child.getChildQualityEvaluationAverage()
                .ifPresent(childAverage ->
                        propagateSubtreeAverageDeltaToAncestors(List.of(parent), childAverage, direction));
    }

    private void propagateResourceConnectionDelta(Node parent, Node child, DeltaDirection direction) {
        var grade = child.getQualityEvaluationGrade();
        var previousGrade = direction == DeltaDirection.ADD ? Optional.<Grade>empty() : grade;
        var newGrade = direction == DeltaDirection.ADD ? grade : Optional.<Grade>empty();

        propagateResourceGradeDeltaToAncestors(List.of(parent), previousGrade, newGrade, PersistenceContextMode.KEEP);
    }

    /**
     * Note: this clears the Hibernate persistence context as a side effect of the bulk SQL update.
     * Any entities loaded earlier in this transaction become detached.
     */
    @Transactional
    public void updateQualityEvaluationOfRecursive(
            Collection<Node> parents, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        propagateResourceGradeDeltaToAncestors(parents, oldGrade, newGrade, PersistenceContextMode.CLEAR);
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

    /**
     * Recovery/repair entry point. Walks descendants in-memory rather than ancestors via SQL —
     * recomputes from scratch in the opposite direction from the bulk paths above.
     */
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
                    propagateResourceGradeDeltaToAncestors(
                            node.getParentNodesForQualityEvaluation(),
                            Optional.empty(),
                            node.getQualityEvaluationGrade(),
                            PersistenceContextMode.KEEP);
                }
            });
        }
    }
}
