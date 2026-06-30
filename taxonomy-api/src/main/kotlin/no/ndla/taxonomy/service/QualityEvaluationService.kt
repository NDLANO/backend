/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import jakarta.persistence.EntityManager
import java.net.URI
import kotlin.jvm.optionals.getOrNull
import no.ndla.taxonomy.domain.Grade
import no.ndla.taxonomy.domain.GradeAverage
import no.ndla.taxonomy.domain.Node
import no.ndla.taxonomy.domain.NodeConnection
import no.ndla.taxonomy.domain.NodeConnectionType
import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.domain.UpdateOrDelete
import no.ndla.taxonomy.repositories.NodeRepository
import no.ndla.taxonomy.rest.v1.commands.NodePostPut
import no.ndla.taxonomy.service.dtos.QualityEvaluationDTO
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class QualityEvaluationService(
    private val nodeRepository: NodeRepository,
    private val entityManager: EntityManager,
) {

  private fun shouldBeIncludedInQualityEvaluationAverage(nodeType: NodeType) =
      nodeType == NodeType.RESOURCE

  private fun getQualityEvaluationCommand(command: UpdatableDto<*>): NodePostPut? =
      (command as? NodePostPut)?.takeIf { it.qualityEvaluation !is UpdateOrDelete.Default }

  /**
   * Serializes quality evaluation updates before loading the node, ensuring that the subsequent
   * getOldGrade/apply/updateParents sequence sees the latest persisted state. Must be called within
   * the same transaction as the update (e.g. from CrudController.updateEntity).
   */
  @Transactional(propagation = Propagation.MANDATORY)
  fun lockQualityEvaluationIfNeeded(command: UpdatableDto<*>): Boolean {
    if (getQualityEvaluationCommand(command) == null) {
      return false
    }
    acquireQualityEvaluationAdvisoryLock()
    return true
  }

  /**
   * Acquires the quality evaluation lock before any parent-graph access happens for a connection
   * mutation. Must be called within the same transaction as the mutation, before any code that
   * lazily initializes {@code parentConnections} on the parent or its ancestors. Otherwise the
   * subsequent QE recursion may walk a stale parent tree (TOCTOU between entity load and lock).
   */
  @Transactional(propagation = Propagation.MANDATORY)
  fun lockForConnectionChange(connectionType: NodeConnectionType) {
    if (connectionType == NodeConnectionType.LINK) {
      return
    }
    acquireQualityEvaluationAdvisoryLock()
  }

  @Transactional(propagation = Propagation.MANDATORY)
  fun updateQualityEvaluationOfParentsFromFreshlyLoadedNode(
      node: Node,
      oldGrade: Grade?,
      command: UpdatableDto<*>,
  ) {
    val nodeCommand = getQualityEvaluationCommand(command) ?: return

    val newGrade =
        (nodeCommand.qualityEvaluation as? UpdateOrDelete.Update<QualityEvaluationDTO>)
            ?.value
            ?.grade

    updateQualityEvaluationOfParents(
        node.nodeType,
        node.parentNodesForQualityEvaluation,
        oldGrade,
        newGrade,
        false,
    )
  }

  @Transactional(propagation = Propagation.MANDATORY)
  fun updateQualityEvaluationOfNewConnection(connection: NodeConnection) {
    if (connection.connectionType == NodeConnectionType.LINK) return

    val parent = connection.parent.getOrNull() ?: return
    val child = connection.child.getOrNull() ?: return

    acquireQualityEvaluationAdvisoryLock()
    entityManager.flush()
    val childSnapshot = getNodeQualityEvaluationSnapshot(child)

    // Update parents quality evaluation average with the newly linked one.
    updateQualityEvaluationOfParents(child.nodeType, listOf(parent), null, childSnapshot.grade)

    childSnapshot.childQualityEvaluationAverage?.let { addGradeAverageTreeToParents(parent, it) }
  }

  private fun addGradeAverageTreeToParents(node: Node, averageToAdd: GradeAverage) {
    synchronizeChildQualityEvaluationAverage(node)
    node.addGradeAverageTreeToAverageCalculation(averageToAdd)
    node.parentNodesForQualityEvaluation.forEach { addGradeAverageTreeToParents(it, averageToAdd) }
  }

  private fun removeGradeAverageTreeFromParents(node: Node, averageToRemove: GradeAverage) {
    synchronizeChildQualityEvaluationAverage(node)
    node.removeGradeAverageTreeFromAverageCalculation(averageToRemove)
    node.parentNodesForQualityEvaluation.forEach {
      removeGradeAverageTreeFromParents(it, averageToRemove)
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  fun removeQualityEvaluationOfDeletedConnection(connectionToDelete: NodeConnection) {
    if (connectionToDelete.connectionType == NodeConnectionType.LINK) return
    val child = connectionToDelete.child.getOrNull() ?: return
    val parent = connectionToDelete.parent.getOrNull() ?: return

    acquireQualityEvaluationAdvisoryLock()
    entityManager.flush()
    val childSnapshot = getNodeQualityEvaluationSnapshot(child)

    if (shouldBeIncludedInQualityEvaluationAverage(child.nodeType)) {
      updateQualityEvaluationOfParents(child.nodeType, listOf(parent), childSnapshot.grade, null)
      return
    }

    childSnapshot.childQualityEvaluationAverage?.let {
      removeGradeAverageTreeFromParents(parent, it)
    }
  }

  protected fun updateQualityEvaluationOfParents(
      nodeType: NodeType,
      parentNodes: Collection<Node>,
      oldGrade: Grade?,
      newGrade: Grade?,
  ) {
    updateQualityEvaluationOfParents(nodeType, parentNodes, oldGrade, newGrade, true)
  }

  private fun updateQualityEvaluationOfParents(
      nodeType: NodeType,
      parentNodes: Collection<Node>,
      oldGrade: Grade?,
      newGrade: Grade?,
      synchronizeBeforeUpdate: Boolean,
  ) {
    if (!shouldBeIncludedInQualityEvaluationAverage(nodeType)) return
    if (oldGrade == null && newGrade == null || oldGrade == newGrade) return

    updateQualityEvaluationOfRecursiveUnlocked(
        parentNodes,
        oldGrade,
        newGrade,
        synchronizeBeforeUpdate,
    )
  }

  @Transactional
  fun updateQualityEvaluationOfRecursive(
      parents: Collection<Node>,
      oldGrade: Grade?,
      newGrade: Grade?,
  ) {
    acquireQualityEvaluationAdvisoryLock()
    entityManager.flush()
    updateQualityEvaluationOfRecursiveUnlocked(parents, oldGrade, newGrade, true)
  }

  private fun updateQualityEvaluationOfRecursiveUnlocked(
      parents: Collection<Node>,
      oldGrade: Grade?,
      newGrade: Grade?,
      synchronizeBeforeUpdate: Boolean,
  ) {
    parents.forEach { parent ->
      if (synchronizeBeforeUpdate) {
        synchronizeChildQualityEvaluationAverage(parent)
      }
      parent.updateChildQualityEvaluationAverage(oldGrade, newGrade)
      updateQualityEvaluationOfRecursiveUnlocked(
          parent.parentNodesForQualityEvaluation,
          oldGrade,
          newGrade,
          synchronizeBeforeUpdate,
      )
    }
  }

  private fun acquireQualityEvaluationAdvisoryLock() {
    entityManager.createNativeQuery(QUALITY_EVALUATION_ADVISORY_LOCK_QUERY).singleResult
  }

  private fun synchronizeChildQualityEvaluationAverage(node: Node) {
    val snapshot = getNodeQualityEvaluationSnapshot(node)
    val childAverage = snapshot.childQualityEvaluationAverage
    node.setChildQualityEvaluationAverage(childAverage?.averageSum ?: 0, childAverage?.count ?: 0)
  }

  private fun asInt(value: Any?) = (value as? Number)?.toInt()

  private fun getNodeQualityEvaluationSnapshot(node: Node): QualityEvaluationSnapshot {
    // Flush pending JPA writes so the native query sees in-tx mutations. Required because
    // multi-parent traversals can revisit the same ancestor via different chains, and each
    // visit re-reads the snapshot before applying its delta.
    entityManager.flush()
    val row =
        entityManager
            .createNativeQuery(NODE_QUALITY_EVALUATION_SNAPSHOT_QUERY)
            .setParameter("nodeId", node.id)
            .singleResult as Array<*>

    val grade = asInt(row[0])?.let { Grade.fromInt(it) }
    val comment = row[1] as? String
    val sum = asInt(row[2]) ?: 0
    val count = asInt(row[3]) ?: 0
    val childAverage = if (count == 0 || sum == 0) null else GradeAverage(sum, count)

    return QualityEvaluationSnapshot(grade, comment, childAverage)
  }

  data class QualityEvaluationSnapshot(
      val grade: Grade?,
      val comment: String?,
      val childQualityEvaluationAverage: GradeAverage?,
  )

  @Transactional
  fun updateEntireAverageTreeForNode(publicId: URI) {
    acquireQualityEvaluationAdvisoryLock()
    val node =
        nodeRepository.findFirstByPublicId(publicId).orElseThrow {
          NotFoundServiceException("Node was not found")
        }

    node.updateEntireAverageTree()
    nodeRepository.save(node)
  }

  @Transactional
  fun updateQualityEvaluationOfAllNodes() {
    acquireQualityEvaluationAdvisoryLock()
    nodeRepository.wipeQualityEvaluationAverages()
    nodeRepository.findNodesWithQualityEvaluation().use { nodeStream ->
      nodeStream.forEach { node ->
        updateQualityEvaluationOfParents(
            node.nodeType,
            node.parentNodesForQualityEvaluation,
            null,
            node.qualityEvaluationGrade.getOrNull(),
        )
      }
    }
  }

  companion object {
    private val QUALITY_EVALUATION_ADVISORY_LOCK_QUERY =
        """
        SELECT pg_advisory_xact_lock(
            hashtext(cast(current_schema() as text)),
            hashtext('taxonomy_quality_evaluation')
        )
        """
            .trimIndent()

    private val NODE_QUALITY_EVALUATION_SNAPSHOT_QUERY =
        """
        SELECT quality_evaluation,
                   quality_evaluation_comment,
                   child_quality_evaluation_sum,
                   child_quality_evaluation_count
            FROM node
            WHERE id = :nodeId
        """
            .trimIndent()
  }
}
