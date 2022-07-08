/*
 * Part of NDLA search-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

import java.time.LocalDateTime

case class EditorNote(note: String, user: String, status: Status, timestamp: LocalDateTime)
