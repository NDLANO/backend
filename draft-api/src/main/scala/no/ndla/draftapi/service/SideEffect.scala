/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import no.ndla.common.model.domain.draft.Draft

import scala.util.{Success, Try}
import scala.language.implicitConversions

object SideEffect {
  type SideEffect = (Draft, Boolean) => Try[Draft]
  def none: SideEffect                           = (article, isImported) => Success(article)
  def fromOutput(output: Try[Draft]): SideEffect = (_, _) => output

  /** Implicits used to simplify creating a [[SideEffect]] which doesn't need all the parameters */
  object implicits {
    implicit def toSideEffect(func: Draft => Try[Draft]): SideEffect =
      (article: Draft, _: Boolean) => {
        func(article)
      }
  }
}
