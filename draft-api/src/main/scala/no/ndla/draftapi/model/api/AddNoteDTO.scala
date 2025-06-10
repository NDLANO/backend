/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Information containing new notes and which draft to add them to")
case class AddNoteDTO(
    @description("Id of the draft to add notes to")
    draftId: Long,
    @description("Notes to add to the draft")
    notes: List[String]
)
