/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Information about notes to add to drafts")
case class AddMultipleNotesDTO(
    @description("Objects for which notes should be added to which drafts")
    data: List[AddNoteDTO]
)
