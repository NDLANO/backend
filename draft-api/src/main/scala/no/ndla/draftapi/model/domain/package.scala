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

  case class ArticleIds(articleId: Long, externalId: List[String], importId: Option[String] = None)
  case class ImportId(importId: Option[String])
}
