/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.common.model.NDLADate

case class EditorNote(note: String, user: String, status: Status, timestamp: NDLADate)
