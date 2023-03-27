/*
 * Part of NDLA common.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import java.time.LocalDateTime
import java.util.UUID

case class Comment(
    id: UUID,
    created: LocalDateTime,
    updated: LocalDateTime,
    content: String,
    isOpen: Boolean
)
