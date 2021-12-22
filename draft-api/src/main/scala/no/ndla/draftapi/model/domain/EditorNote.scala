/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain
import java.util.Date

case class EditorNote(note: String, user: String, status: Status, timestamp: Date)
