/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import no.ndla.articleapi.Props
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.article.Article
import scalikejdbc.*

trait DBArticle {
  this: Props =>
  object Article extends SQLSyntaxSupport[Article] {

    override val tableName                       = "contentdata"
    override lazy val schemaName: Option[String] = Some(props.MetaSchema)

    def fromResultSet(lp: SyntaxProvider[Article])(rs: WrappedResultSet): ArticleRow =
      fromResultSet(lp.resultName)(rs)

    def fromResultSet(lp: ResultName[Article])(rs: WrappedResultSet): ArticleRow = {
      val articleId = rs.long(lp.c("article_id"))
      val rowId     = rs.long(lp.c("id"))
      val document  = rs.stringOpt(lp.c("document"))
      val revision  = rs.int(lp.c("revision"))
      val slug      = rs.stringOpt(lp.c("slug"))

      val article = document.map(jsonStr => {
        val meta = CirceUtil.unsafeParseAs[Article](jsonStr)
        meta.copy(
          id = Some(articleId),
          revision = Some(revision),
          slug = slug
        )
      })

      ArticleRow(
        rowId = rowId,
        revision = revision,
        articleId = articleId,
        slug = slug,
        article = article
      )
    }
  }

}
