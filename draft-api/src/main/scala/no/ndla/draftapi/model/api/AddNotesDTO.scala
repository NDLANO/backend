/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Data to add notes to a draft")
case class AddNotesDTO(
    @description("Notes to add to the draft")
    notes: List[String]
)
