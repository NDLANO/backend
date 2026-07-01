/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.integration

import java.net.URI
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull
import no.ndla.taxonomy.domain.Node
import no.ndla.taxonomy.domain.NodeConnection
import no.ndla.taxonomy.domain.NodeType
import no.ndla.taxonomy.domain.Relevance
import no.ndla.taxonomy.integration.dtos.DraftNotesDTO
import no.ndla.taxonomy.integration.dtos.UpdateNotesDTO
import no.ndla.taxonomy.service.VersionContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

private data class IdAndType(val id: Long, val type: String)

private data class ConnectionContext(
    val parent: Node,
    val child: Node,
    val parentId: IdAndType,
    val childId: IdAndType,
)

private fun getId(contentUri: URI?): IdAndType? {
  val splits = contentUri?.toString()?.split(":") ?: return null
  if (splits.size != 3) return null
  val (_, type, id) = splits
  return IdAndType(id.toLongOrNull() ?: return null, type)
}

@Component
class DraftApiClient(
    @param:Value("\${DRAFT_API_HOST:draft-api}") private val draftApiHost: String,
    @param:Value("\${spring.datasource.hikari.schema:taxonomy_api}")
    private val defaultSchema: String,
) {
  private val restClient: RestClient =
      RestClient.builder()
          .baseUrl("http://$draftApiHost/draft-api")
          .requestInitializer { AuthorizationRequestInitializer() }
          .build()

  private fun notBaseSchema() = VersionContext.getCurrentVersion() != defaultSchema

  private fun NodeConnection.resolve(errorMsg: String): ConnectionContext? {
    if (notBaseSchema()) return null
    val parent = parent.getOrNull()
    val child = child.getOrNull()
    val parentId = parent?.let { getId(it.contentUri) }
    val childId = child?.let { getId(it.contentUri) }

    if (parent == null || child == null || parentId == null || childId == null) {
      logger.error(errorMsg)
      return null
    }

    return ConnectionContext(parent, child, parentId, childId)
  }

  fun updateNotesWithNewConnection(newConnection: NodeConnection) {
    val errorMsg =
        "Attempted to update draft with new connection, but parent or child was missing. This is a bug somewhere."
    val (parent, child, parentId, childId) = newConnection.resolve(errorMsg) ?: return

    val notes = buildList {
      if (parentId.type == "article") {
        val relevanceNotePart = newConnection.relevance.getOrNull()?.getTranslatedName() ?: "barn"
        val noteString =
            "Taksonomi: ${child.nodeType.displayName} med id '${childId.id}' lagt til som $relevanceNotePart"
        add(DraftNotesDTO.fromNote(parentId.id, noteString))
        if (newConnection.isPrimary.getOrDefault(false)) {
          val primaryNoteString =
              "Taksonomi: ${child.nodeType.displayName} med id '${childId.id}' lagt til som primærkobling"
          add(DraftNotesDTO.fromNote(parentId.id, primaryNoteString))
        }
      }
      if (childId.type == "article") {
        val relevanceNotePart =
            newConnection.relevance.getOrNull()?.getTranslatedName()?.let { " som $it" } ?: ""
        val noteId = if (parentId.type == "frontpage") parent.publicId else parentId.id
        val noteString =
            "Taksonomi: lagt til i ${parent.nodeType.displayName} med id '$noteId'$relevanceNotePart"
        add(DraftNotesDTO.fromNote(childId.id, noteString))
      }
    }
    updateNotes(notes)
  }

  fun updateNotesWithDeletedConnection(deletedConnection: NodeConnection) {
    val errorMsg =
        "Attempted to update draft with deleted connection, but parent or child was missing. This is a bug somewhere."
    val (parent, child, parentId, childId) = deletedConnection.resolve(errorMsg) ?: return

    val notes = buildList {
      if (childId.type == "article") {
        val noteString =
            "Taksonomi: fjernet fra ${parent.nodeType.displayName} med id '${parentId.id}'"
        add(DraftNotesDTO.fromNote(childId.id, noteString))
      }
      if (parentId.type == "article") {
        val noteString = "Taksonomi: ${child.nodeType.displayName} med id '${childId.id}' fjernet"
        add(DraftNotesDTO.fromNote(parentId.id, noteString))
      }
    }
    updateNotes(notes)
  }

  fun updateRelevanceNotesWithUpdatedConnection(
      nodeConnection: NodeConnection,
      newRelevance: Relevance,
  ) {
    val errorMsg =
        "Attempted to update draft with updated connection, but parent or child was missing. This is a bug somewhere."
    val (parent, child, parentId, childId) = nodeConnection.resolve(errorMsg) ?: return

    val notes = buildList {
      if (nodeConnection.relevance.getOrNull() != newRelevance) {
        if (childId.type == "article") {
          val note =
              "Taksonomi: satt som ${newRelevance.getTranslatedName()} for ${parent.nodeType.displayName} med id '${parentId.id}'"
          add(DraftNotesDTO.fromNote(childId.id, note))
        }
        if (parentId.type == "article") {
          val note =
              "Taksonomi: ${child.nodeType.displayName} med id '${childId.id}' satt som ${newRelevance.getTranslatedName()}"
          add(DraftNotesDTO.fromNote(parentId.id, note))
        }
      }
    }
    updateNotes(notes)
  }

  fun updatePrimaryNotesWithUpdatedConnection(
      nodeConnection: NodeConnection,
      newIsPrimary: Boolean?,
  ) {
    val errorMsg =
        "Attempted to update draft with updated connection, but parent or child was missing. This is a bug somewhere."
    val (parent, _, parentId, childId) = nodeConnection.resolve(errorMsg) ?: return

    val oldPrimary = nodeConnection.isPrimary.getOrNull()
    val newPrimary = newIsPrimary ?: false
    if (oldPrimary != newPrimary && parentId.type == "article") {
      val action = if (newPrimary) "satt" else "fjernet"
      val note =
          "Taksonomi: $action som primærkobling på ${parent.nodeType.displayName} med id '${parentId.id}'"
      updateNotes(listOf(DraftNotesDTO.fromNote(childId.id, note)))
    }
  }

  private val NodeType.displayName
    get() =
        when (this) {
          NodeType.NODE -> "node"
          NodeType.TOPIC -> "emne"
          NodeType.CASE -> "case"
          NodeType.RESOURCE -> "ressurs"
          NodeType.SUBJECT -> "fag"
          NodeType.PROGRAMME -> "utdanningsprogram"
        }

  private fun updateNotes(notes: List<DraftNotesDTO>) {
    if (notes.isEmpty()) return
    val maxRetries = 3
    var attempt = 0
    val body = UpdateNotesDTO(notes)

    while (attempt < maxRetries) {
      val response =
          restClient
              .post()
              .uri("/v1/drafts/notes")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .onStatus({ it == HttpStatus.CONFLICT }) { _, _ ->
                // Don't throw exception, will retry in the loop
              }
              .toBodilessEntity()

      if (response.statusCode.is2xxSuccessful) {
        return
      } else if (response.statusCode == HttpStatus.CONFLICT) {
        attempt++
      } else {
        logger.error("Got status code '${response.statusCode.value()}' when updating notes")
        break
      }
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(DraftApiClient::class.java)
  }
}
