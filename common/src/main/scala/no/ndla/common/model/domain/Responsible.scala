/*
 * Part of NDLA common.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import java.time.LocalDateTime

case class Responsible(
    responsibleId: String,
    lastUpdated: LocalDateTime
)
