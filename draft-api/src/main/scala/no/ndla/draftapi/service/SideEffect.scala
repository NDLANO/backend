/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.auth.UserInfo

import scala.util.{Success, Try}
import scala.language.implicitConversions

object SideEffect {
  type SideEffect = (Draft, Boolean, UserInfo) => Try[Draft]
  def none: SideEffect                           = (article, isImported, _) => Success(article)
  def fromOutput(output: Try[Draft]): SideEffect = (_, _, _) => output

  /** Implicits used to simplify creating a [[SideEffect]] which doesn't need all the parameters */
  object implicits {
    implicit def toSideEffect(func: Draft => Try[Draft]): SideEffect =
      (article: Draft, _: Boolean, _: UserInfo) => {
        func(article)
      }
  }
}
