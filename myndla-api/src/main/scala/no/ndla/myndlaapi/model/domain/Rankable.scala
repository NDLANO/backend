/*
 * Part of NDLA myndla-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import java.util.UUID

trait Rankable {
  val sortId: UUID
  val sortRank: Option[Int]
}
