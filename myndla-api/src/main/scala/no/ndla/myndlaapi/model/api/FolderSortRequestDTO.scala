/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.api

import sttp.tapir.Schema.annotations.description

import java.util.UUID

case class FolderSortRequestDTO(
    @description("List of the children ids in sorted order, MUST be all ids")
    sortedIds: Seq[UUID]
)
