/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import no.ndla.common.model.domain.draft.Draft
import no.ndla.network.tapir.auth.TokenUser

import scala.util.Try

object SideEffect {
  type IsImported = Boolean
  type SideEffect = (Draft, IsImported, TokenUser) => Try[Draft]

  /** Implicits used to simplify creating a [[SideEffect]] which doesn't need all the parameters */
  object implicits {
    implicit def toSideEffect(func: Draft => Try[Draft]): SideEffect =
      (article: Draft, _: Boolean, _: TokenUser) => {
        func(article)
      }
  }
}
