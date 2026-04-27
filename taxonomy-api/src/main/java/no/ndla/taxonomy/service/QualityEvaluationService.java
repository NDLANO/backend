/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class QualityEvaluationService {
    private static final String QUALITY_EVALUATION_ADVISORY_LOCK_QUERY = """
            SELECT pg_advisory_xact_lock(
                hashtext(cast(current_schema() as text)),
                hashtext('taxonomy_quality_evaluation')
            )
            """;
    private static final String NODE_QUALITY_EVALUATION_SNAPSHOT_QUERY = """
            SELECT quality_evaluation,
                   quality_evaluation_comment,
                   child_quality_evaluation_sum,
                   child_quality_evaluation_count
            FROM node
            WHERE id = :nodeId
            """;

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
     * Serializes quality evaluation updates before loading the node, ensuring that the subsequent
     * getOldGrade/apply/updateParents sequence sees the latest persisted state.
     * Must be called within the same transaction as the update (e.g. from CrudController.updateEntity).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockQualityEvaluationIfNeeded(UpdatableDto<?> command) {
        if (getQualityEvaluationCommand(command).isEmpty()) {
            return false;
        }

        acquireQualityEvaluationAdvisoryLock();
        return true;
    }

    /**
     * Acquires the quality evaluation lock before any parent-graph access happens for a connection
     * mutation. Must be called within the same transaction as the mutation, before any code that
     * lazily initializes {@code parentConnections} on the parent or its ancestors. Otherwise the
     * subsequent QE recursion may walk a stale parent tree (TOCTOU between entity load and lock).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockForConnectionChange(NodeConnectionType connectionType) {
        if (connectionType == NodeConnectionType.LINK) {
            return;
        }
        acquireQualityEvaluationAdvisoryLock();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateQualityEvaluationOfParentsFromFreshlyLoadedNode(
            Node node, Optional<Grade> oldGrade, UpdatableDto<?> command) {
        var nodeCommand = getQualityEvaluationCommand(command);
        if (nodeCommand.isEmpty()) {
            return;
        }

        var newGrade = nodeCommand.get().qualityEvaluation.getValue().map(QualityEvaluationDTO::getGrade);

        updateQualityEvaluationOfParents(
                node.getNodeType(), node.getParentNodesForQualityEvaluation(), oldGrade, newGrade, false);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateQualityEvaluationOfNewConnection(NodeConnection connection) {
        if (connection.getConnectionType() == NodeConnectionType.LINK) {
            return;
        }

        var parent = connection.getParent().orElse(null);
        var child = connection.getChild().orElse(null);
        if (parent == null || child == null) {
            return;
        }

        acquireQualityEvaluationAdvisoryLock();
        entityManager.flush();
        var childSnapshot = getNodeQualityEvaluationSnapshot(child);

        // Update parents quality evaluation average with the newly linked one.
        updateQualityEvaluationOfParents(child.getNodeType(), List.of(parent), Optional.empty(), childSnapshot.grade());

        childSnapshot
                .childQualityEvaluationAverage()
                .ifPresent(childAverage -> addGradeAverageTreeToParents(parent, childAverage));
    }

    private void addGradeAverageTreeToParents(Node node, GradeAverage averageToAdd) {
        synchronizeChildQualityEvaluationAverage(node);
        node.addGradeAverageTreeToAverageCalculation(averageToAdd);
        node.getParentNodesForQualityEvaluation().forEach(parent -> addGradeAverageTreeToParents(parent, averageToAdd));
    }

    private void removeGradeAverageTreeFromParents(Node node, GradeAverage averageToRemove) {
        synchronizeChildQualityEvaluationAverage(node);
        node.removeGradeAverageTreeFromAverageCalculation(averageToRemove);
        node.getParentNodesForQualityEvaluation()
                .forEach(parent -> removeGradeAverageTreeFromParents(parent, averageToRemove));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void removeQualityEvaluationOfDeletedConnection(NodeConnection connectionToDelete) {
        if (connectionToDelete.getConnectionType() == NodeConnectionType.LINK) return;

        var noChild = connectionToDelete.getChild().isEmpty();
        var noParent = connectionToDelete.getParent().isEmpty();
        if (noChild || noParent) return;

        var child = connectionToDelete.getChild().get();
        var parent = connectionToDelete.getParent().get();

        acquireQualityEvaluationAdvisoryLock();
        entityManager.flush();
        var childSnapshot = getNodeQualityEvaluationSnapshot(child);

        if (shouldBeIncludedInQualityEvaluationAverage(child.getNodeType())) {
            updateQualityEvaluationOfParents(
                    child.getNodeType(), List.of(parent), childSnapshot.grade(), Optional.empty());
            return;
        }

        childSnapshot
                .childQualityEvaluationAverage()
                .ifPresent(childAverage -> removeGradeAverageTreeFromParents(parent, childAverage));
    }

    protected void updateQualityEvaluationOfParents(
            NodeType nodeType, Collection<Node> parentNodes, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        updateQualityEvaluationOfParents(nodeType, parentNodes, oldGrade, newGrade, true);
    }

    private void updateQualityEvaluationOfParents(
            NodeType nodeType,
            Collection<Node> parentNodes,
            Optional<Grade> oldGrade,
            Optional<Grade> newGrade,
            boolean synchronizeBeforeUpdate) {
        if (!shouldBeIncludedInQualityEvaluationAverage(nodeType)) {
            return;
        }
        if (oldGrade.isEmpty() && newGrade.isEmpty() || oldGrade.equals(newGrade)) {
            return;
        }

        updateQualityEvaluationOfRecursiveUnlocked(parentNodes, oldGrade, newGrade, synchronizeBeforeUpdate);
    }

    @Transactional
    public void updateQualityEvaluationOfRecursive(
            Collection<Node> parents, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        acquireQualityEvaluationAdvisoryLock();
        entityManager.flush();
        updateQualityEvaluationOfRecursiveUnlocked(parents, oldGrade, newGrade, true);
    }

    private void updateQualityEvaluationOfRecursiveUnlocked(
            Collection<Node> parents,
            Optional<Grade> oldGrade,
            Optional<Grade> newGrade,
            boolean synchronizeBeforeUpdate) {
        parents.forEach(parent -> {
            if (synchronizeBeforeUpdate) {
                synchronizeChildQualityEvaluationAverage(parent);
            }
            parent.updateChildQualityEvaluationAverage(oldGrade, newGrade);
            updateQualityEvaluationOfRecursiveUnlocked(
                    parent.getParentNodesForQualityEvaluation(), oldGrade, newGrade, synchronizeBeforeUpdate);
        });
    }

    private void acquireQualityEvaluationAdvisoryLock() {
        entityManager.createNativeQuery(QUALITY_EVALUATION_ADVISORY_LOCK_QUERY).getSingleResult();
    }

    private void synchronizeChildQualityEvaluationAverage(Node node) {
        var snapshot = getNodeQualityEvaluationSnapshot(node);
        var childAverage = snapshot.childQualityEvaluationAverage();
        node.setChildQualityEvaluationAverage(
                childAverage.map(GradeAverage::getAverageSum).orElse(0),
                childAverage.map(GradeAverage::getCount).orElse(0));
    }

    private QualityEvaluationSnapshot getNodeQualityEvaluationSnapshot(Node node) {
        // Flush pending JPA writes so the native query sees in-tx mutations. Required because
        // multi-parent traversals can revisit the same ancestor via different chains, and each
        // visit re-reads the snapshot before applying its delta.
        entityManager.flush();
        var row = (Object[]) entityManager
                .createNativeQuery(NODE_QUALITY_EVALUATION_SNAPSHOT_QUERY)
                .setParameter("nodeId", node.getId())
                .getSingleResult();

        var grade = Optional.ofNullable(row[0])
                .map(Number.class::cast)
                .map(Number::intValue)
                .map(Grade::fromInt);
        var comment = Optional.ofNullable((String) row[1]);
        var sum = Optional.ofNullable(row[2])
                .map(Number.class::cast)
                .map(Number::intValue)
                .orElse(0);
        var count = Optional.ofNullable(row[3])
                .map(Number.class::cast)
                .map(Number::intValue)
                .orElse(0);
        var childAverage =
                count == 0 || sum == 0 ? Optional.<GradeAverage>empty() : Optional.of(new GradeAverage(sum, count));

        return new QualityEvaluationSnapshot(grade, comment, childAverage);
    }

    private record QualityEvaluationSnapshot(
            Optional<Grade> grade, Optional<String> comment, Optional<GradeAverage> childQualityEvaluationAverage) {}

    @Transactional
    public void updateEntireAverageTreeForNode(URI publicId) {
        acquireQualityEvaluationAdvisoryLock();
        var node = nodeRepository
                .findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        node.updateEntireAverageTree();
        nodeRepository.save(node);
    }

    @Transactional
    public void updateQualityEvaluationOfAllNodes() {
        acquireQualityEvaluationAdvisoryLock();
        nodeRepository.wipeQualityEvaluationAverages();
        var nodeStream = nodeRepository.findNodesWithQualityEvaluation();
        nodeStream.forEach(node -> updateQualityEvaluationOfParents(
                node.getNodeType(),
                node.getParentNodesForQualityEvaluation(),
                Optional.empty(),
                node.getQualityEvaluationGrade()));
    }
}
