/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api

import sttp.tapir.Schema.annotations.description

import java.util.UUID

case class FolderSortRequest(
    @description("List of the children ids in sorted order, MUST be all ids") sortedIds: Seq[UUID]
)
