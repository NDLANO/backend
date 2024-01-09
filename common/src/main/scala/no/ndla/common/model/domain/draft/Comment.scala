/*
 * Part of NDLA common.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import no.ndla.common.model.NDLADate

import java.util.UUID

case class Comment(
    id: UUID,
    created: NDLADate,
    updated: NDLADate,
    content: String,
    isOpen: Boolean,
    solved: Boolean
)
