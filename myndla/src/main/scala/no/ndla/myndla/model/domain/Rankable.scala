/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain

import java.util.UUID

trait Rankable {
  val sortId: UUID
  val sortRank: Option[Int]
}
