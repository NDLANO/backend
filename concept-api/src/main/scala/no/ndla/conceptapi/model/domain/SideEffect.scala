/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.conceptapi.auth.UserInfo

import scala.util.{Success, Try}

object SideEffect {
  type SideEffect = (Concept, UserInfo) => Try[Concept]
  def none: SideEffect                             = (concept, _) => Success(concept)
  def fromOutput(output: Try[Concept]): SideEffect = (_, _) => output
}
