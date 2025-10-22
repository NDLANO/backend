/*
 * Part of NDLA article-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import no.ndla.common.model.domain.article.Article

case class ArticleRow(rowId: Long, revision: Int, articleId: Long, slug: Option[String], article: Option[Article])

object ArticleRow {

  /** Implicit class to add helper methods to `Option[ArticleRow]` */
  implicit class OptionalArticleRow(articleRow: Option[ArticleRow]) {
    def toArticle: Option[Article] = articleRow.flatMap(_.article)

    def mapArticle(func: Article => Article): Option[ArticleRow] = {
      articleRow.map(ar => ar.copy(article = ar.article.map(func)))
    }
  }

  /** Implicit class to add helper methods to `List[ArticleRow]` */
  implicit class ArticleRows(articleRows: Seq[ArticleRow]) {
    def toArticles: Seq[Article] = articleRows.flatMap(_.article)
  }
}
