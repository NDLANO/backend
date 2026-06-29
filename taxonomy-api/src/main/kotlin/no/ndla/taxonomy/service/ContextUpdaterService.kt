/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull
import no.ndla.taxonomy.config.Constants
import no.ndla.taxonomy.domain.LanguageField
import no.ndla.taxonomy.domain.Node
import no.ndla.taxonomy.domain.NodeConnection
import no.ndla.taxonomy.domain.NodeConnectionType
import no.ndla.taxonomy.domain.Relevance
import no.ndla.taxonomy.domain.TaxonomyContext
import no.ndla.taxonomy.util.HashUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ContextUpdaterService {

  private fun createContext(
      node: Node,
      parentContext: TaxonomyContext,
      parent: Node,
      parentConnection: NodeConnection,
      activeContext: Boolean,
  ): TaxonomyContext {
    val breadcrumbs =
        LanguageField.listFromLists(parentContext.breadcrumbs, LanguageField.fromNode(parent))

    val parentIds = (parentContext.parentIds + parent.publicId.toString()).toMutableList()
    val parentContextIds =
        (parentContext.parentContextIds + parentContext.contextId).toMutableList()
    val contextId = HashUtil.mediumHash(parentContext.contextId + parentConnection.publicId)
    return TaxonomyContext(
        node.publicId.toString(),
        LanguageField.fromNode(node),
        node.nodeType,
        parentContext.rootId,
        parentContext.rootName,
        parentContext.path + node.pathPart,
        breadcrumbs,
        node.contextType.getOrNull(),
        parentIds,
        parentContextIds,
        parentContext.isVisible && node.isVisible(),
        parentContext.isActive && activeContext,
        parentConnection.isPrimary.getOrElse { false },
        parentContext.isArchived,
        parentConnection.relevance.getOrElse { Relevance.CORE }.id.toString(),
        contextId,
        parentConnection.rank,
        parentConnection.publicId.toString(),
        mutableListOf(),
    )
  }

  private fun createContexts(node: Node): Set<TaxonomyContext> {
    val fields = node.getCustomFields()
    val activeContext =
        (fields[Constants.SubjectCategory] ?: Constants.Active) in ACTIVE_SUBJECT_CATEGORIES
    val isArchived = fields[Constants.SubjectType] == Constants.ArchiveSubject

    return hashSetOf<TaxonomyContext>().apply {
      // This entity can be root path
      if (node.isContext) {
        val contextId = HashUtil.semiHash(node.publicId)
        add(
            TaxonomyContext(
                node.publicId.toString(),
                LanguageField.fromNode(node),
                node.nodeType,
                node.publicId.toString(),
                LanguageField.fromNode(node),
                node.pathPart,
                LanguageField(),
                node.contextType.getOrNull(),
                mutableListOf(),
                mutableListOf(),
                node.isVisible(),
                activeContext,
                true,
                isArchived,
                Relevance.CORE.id.toString(),
                contextId,
                0,
                "",
                mutableListOf(),
            ))
      }
      node.parentConnections.forEach { pc ->
        if (pc.connectionType != NodeConnectionType.BRANCH) return@forEach
        val parent = pc.parent.getOrNull() ?: return@forEach
        createContexts(parent).mapTo(this) { createContext(node, it, parent, pc, activeContext) }
      }
    }
  }

  /*
   * Method recursively re-creates all Contexts entries for the entity by removing old entities and creating new ones
   */
  @Transactional(propagation = Propagation.MANDATORY)
  fun updateContexts(entity: Node) {
    entity.childConnections.toSet().forEach {
      it.child.getOrNull()?.let { child -> updateContexts(child) }
    }

    clearContexts(entity)

    val contexts = createContexts(entity)
    entity.contexts = contexts
    entity.addContextIds(contexts.mapTo(mutableSetOf()) { it.contextId })
  }

  @Transactional(propagation = Propagation.MANDATORY)
  fun clearContexts(entity: Node) {
    entity.contexts = hashSetOf()
  }

  companion object {
    private val ACTIVE_SUBJECT_CATEGORIES =
        listOf(Constants.Active, Constants.Beta, Constants.OtherResources)
  }
}
