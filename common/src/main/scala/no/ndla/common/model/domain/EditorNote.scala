/*
 * Part of NDLA common.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain
import no.ndla.common.model.NDLADate

case class EditorNote(note: String, user: String, status: Status, timestamp: NDLADate)
