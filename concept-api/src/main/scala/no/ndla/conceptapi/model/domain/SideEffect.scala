/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import scala.language.implicitConversions
import scala.util.{Success, Try}

object SideEffect {
  type SideEffect = (Concept) => Try[Concept]
  def none: SideEffect = (concept) => Success(concept)
  def fromOutput(output: Try[Concept]): SideEffect = (_) => output
}
