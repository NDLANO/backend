/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import java.time.LocalDateTime

case class DraftResponsible(
    responsibleId: String,
    lastUpdated: LocalDateTime
)
