/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.common.model.domain.concept.Concept
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Success, Try}

object SideEffect {
  type SideEffect = (Concept, TokenUser) => Try[Concept]
  def none: SideEffect                             = (concept, _) => Success(concept)
  def fromOutput(output: Try[Concept]): SideEffect = (_, _) => output
}
