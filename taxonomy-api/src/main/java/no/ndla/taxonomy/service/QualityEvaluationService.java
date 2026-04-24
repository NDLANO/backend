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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * Acquires a pessimistic lock on the node and refreshes it from the database, ensuring
     * that the subsequent getOldGrade/apply/updateParents sequence sees the latest persisted state.
     * Must be called within the same transaction as the update (e.g. from CrudController.updateEntity).
     */
    @Transactional
    public void lockNodeForQualityEvaluationUpdate(Node node, UpdatableDto<?> command) {
        if (getQualityEvaluationCommand(command).isEmpty()) {
            return;
        }

        entityManager.flush();
        lockAndRefresh(node);
    }

    @Transactional
    public void updateQualityEvaluationOfParents(Node node, Optional<Grade> oldGrade, UpdatableDto<?> command) {
        var nodeCommand = getQualityEvaluationCommand(command);
        if (nodeCommand.isEmpty()) {
            return;
        }

        var newGrade = nodeCommand.get().qualityEvaluation.getValue().map(QualityEvaluationDTO::getGrade);

        updateQualityEvaluationOfParents(
                node.getNodeType(), node.getParentNodesForQualityEvaluation(), oldGrade, newGrade);
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

        // Lock the parent tree once upfront to avoid double lock+refresh discarding changes.
        // var allNodes = lockParentTree(List.of(parent));
        // lockAndRefresh(child);

        // Update parents quality evaluation average with the newly linked one.
        updateQualityEvaluationOfParents(
                child.getNodeType(), List.of(parent), Optional.empty(), child.getQualityEvaluationGrade());

        child.getChildQualityEvaluationAverage().ifPresent(childAverage -> {
            addGradeAverageTreeToParents(parent, childAverage);
        });

        // nodeRepository.saveAll(allNodes);
    }

    private void addGradeAverageTreeToParents(Node node, GradeAverage averageToAdd) {
        node.addGradeAverageTreeToAverageCalculation(averageToAdd);
        node.getParentNodesForQualityEvaluation().forEach(parent -> addGradeAverageTreeToParents(parent, averageToAdd));
    }

    private void removeGradeAverageTreeFromParents(Node node, GradeAverage averageToRemove) {
        node.removeGradeAverageTreeFromAverageCalculation(averageToRemove);
        node.getParentNodesForQualityEvaluation()
                .forEach(parent -> removeGradeAverageTreeFromParents(parent, averageToRemove));
    }

    @Transactional
    public void removeQualityEvaluationOfDeletedConnection(NodeConnection connectionToDelete) {
        if (connectionToDelete.getConnectionType() == NodeConnectionType.LINK) return;

        var noChild = connectionToDelete.getChild().isEmpty();
        var noParent = connectionToDelete.getParent().isEmpty();
        if (noChild || noParent) return;

        var child = connectionToDelete.getChild().get();
        var parent = connectionToDelete.getParent().get();

        if (shouldBeIncludedInQualityEvaluationAverage(child.getNodeType())) {
            updateQualityEvaluationOfParents(
                    child.getNodeType(), List.of(parent), child.getQualityEvaluationGrade(), Optional.empty());
            return;
        }

        if (child.getChildQualityEvaluationAverage().isEmpty()) return;
        var childAverage = child.getChildQualityEvaluationAverage().get();

        // var allNodes = lockParentTree(List.of(parent));
        removeGradeAverageTreeFromParents(parent, childAverage);
        // nodeRepository.saveAll(allNodes);
    }

    @Transactional
    protected void updateQualityEvaluationOfParents(
            NodeType nodeType, Collection<Node> parentNodes, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        if (!shouldBeIncludedInQualityEvaluationAverage(nodeType)) {
            return;
        }
        if (oldGrade.isEmpty() && newGrade.isEmpty() || oldGrade.equals(newGrade)) {
            return;
        }

        updateQualityEvaluationOfRecursive(parentNodes, oldGrade, newGrade);
    }

    @Transactional
    public void updateQualityEvaluationOfRecursive(
            Collection<Node> parents, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        // var allNodes = lockParentTree(parents);
        updateQualityEvaluationOfRecursiveUnlocked(parents, oldGrade, newGrade);
        // nodeRepository.saveAll(allNodes);
    }

    private void updateQualityEvaluationOfRecursiveUnlocked(
            Collection<Node> parents, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        parents.forEach(p -> {
            p.updateChildQualityEvaluationAverage(oldGrade, newGrade);
            var parentsParents = p.getParentNodesForQualityEvaluation();
            updateQualityEvaluationOfRecursiveUnlocked(parentsParents, oldGrade, newGrade);
        });
    }

    private Collection<Node> lockParentTree(Collection<Node> parents) {
        // Flush pending changes before locking so that refresh does not discard in-memory mutations
        // made earlier in this transaction (e.g. command.apply on the child node).
        entityManager.flush();
        var allNodes = collectParentTree(parents);
        // Lock in consistent ID order to prevent deadlocks between concurrent transactions.
        allNodes.stream().sorted(Comparator.comparing(Node::getId)).forEach(this::lockAndRefresh);
        return allNodes;
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

    private Collection<Node> collectParentTree(Collection<Node> parents) {
        Map<Integer, Node> nodesById = new HashMap<>();
        collectParentTree(parents, nodesById);
        return nodesById.values();
    }

    private void collectParentTree(Collection<Node> parents, Map<Integer, Node> nodesById) {
        parents.forEach(parent -> {
            if (parent == null || nodesById.containsKey(parent.getId())) {
                return;
            }

            nodesById.put(parent.getId(), parent);
            collectParentTree(parent.getParentNodesForQualityEvaluation(), nodesById);
        });
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
        var nodeStream = nodeRepository.findNodesWithQualityEvaluation();
        nodeStream.forEach(node -> updateQualityEvaluationOfParents(
                node.getNodeType(),
                node.getParentNodesForQualityEvaluation(),
                Optional.empty(),
                node.getQualityEvaluationGrade()));
    }
}
