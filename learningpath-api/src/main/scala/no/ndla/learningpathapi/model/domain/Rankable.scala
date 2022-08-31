/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import java.util.UUID

trait Rankable {
  val sortId: UUID
  val sortRank: Option[Int]
}
