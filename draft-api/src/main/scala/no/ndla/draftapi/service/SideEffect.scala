/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service

import no.ndla.common.model.domain.draft.Draft
import no.ndla.network.tapir.auth.TokenUser

import scala.util.Try

case class SideEffect(name: String, function: (Draft, TokenUser) => Try[Draft]) {
  def run(article: Draft, user: TokenUser): Try[Draft] = function(article, user)
}

object SideEffect {
  def withDraft(name: String)(func: Draft => Try[Draft]): SideEffect = {
    SideEffect(
      name = name,
      function = (article: Draft, _: TokenUser) => {
        func(article)
      },
    )
  }

  def withDraftAndUser(name: String)(func: (Draft, TokenUser) => Try[Draft]): SideEffect = {
    SideEffect(
      name = name,
      function = (article: Draft, tokenUser: TokenUser) => {
        func(article, tokenUser)
      },
    )
  }
}
