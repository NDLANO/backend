/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import no.ndla.draftapi.model.domain

import scala.util.{Success, Try}
import scala.language.implicitConversions

object SideEffect {
  type SideEffect = (domain.Article, Boolean) => Try[domain.Article]
  def none: SideEffect = (article, isImported) => Success(article)
  def fromOutput(output: Try[domain.Article]): SideEffect = (_, _) => output

  /** Implicits used to simplify creating a [[SideEffect]] which doesn't need all the parameters */
  object implicits {
    implicit def toSideEffect(func: domain.Article => Try[domain.Article]): SideEffect =
      (article: domain.Article, _: Boolean) => {
        func(article)
      }
  }
}
