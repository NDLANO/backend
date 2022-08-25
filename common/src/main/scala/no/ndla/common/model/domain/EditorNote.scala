/*
 * Part of NDLA common.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain
import java.time.LocalDateTime

case class EditorNote(note: String, user: String, status: Status, timestamp: LocalDateTime)
