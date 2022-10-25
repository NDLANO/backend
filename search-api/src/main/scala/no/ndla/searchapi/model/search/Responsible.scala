/*
 * Part of NDLA search-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import java.time.LocalDateTime

case class Responsible(
    responsibleId: String,
    lastUpdated: LocalDateTime
)
