/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */
package no.ndla.draftapi.model

package object domain {

  def emptySomeToNone(lang: Option[String]): Option[String] = {
    lang.filter(_.nonEmpty)
  }

  type RelatedContent = Either[RelatedContentLink, Long]

  type IgnoreFunction = (Option[Article], StateTransition) => Boolean
}
