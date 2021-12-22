/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.articleapi.model

package object domain {

  def emptySomeToNone(lang: Option[String]): Option[String] = {
    lang.filter(_.nonEmpty)
  }

  type RelatedContent = Either[RelatedContentLink, Long]

  case class ArticleIds(articleId: Long, externalId: List[String])
}
