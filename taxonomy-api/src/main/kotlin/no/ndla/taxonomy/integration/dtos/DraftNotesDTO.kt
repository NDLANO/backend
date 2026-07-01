/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.integration.dtos

import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class DraftNotesDTO(val draftId: Long, val notes: Collection<String>) {
  companion object {
    fun fromNote(draftId: Long, note: String) = DraftNotesDTO(draftId, listOf(note))
  }
}
