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

class DBArticle(using props: Props) {
  object Article extends SQLSyntaxSupport[Article] {

    override def tableName                  = "contentdata"
    override def schemaName: Option[String] = Some(props.MetaSchema)

    def fromResultSet(a: SyntaxProvider[Article])(rs: WrappedResultSet): ArticleRow = fromResultSet(a.resultName)(rs)

    def fromResultSet(a: ResultName[Article])(rs: WrappedResultSet): ArticleRow = {
      val articleId   = rs.long(a.c("article_id"))
      val externalIds = rs
        .arrayOpt(a.c("external_id"))
        .map(_.getArray.asInstanceOf[Array[String]].toList)
        .getOrElse(List.empty)
      val rowId    = rs.long(a.c("id"))
      val document = rs.stringOpt(a.c("document"))
      val revision = rs.int(a.c("revision"))
      val slug     = rs.stringOpt(a.c("slug"))

      val article = document.map(jsonStr => {
        val meta = CirceUtil.unsafeParseAs[Article](jsonStr)
        meta.copy(id = Some(articleId), revision = Some(revision), slug = slug)
      })

      ArticleRow(
        rowId = rowId,
        externalIds = externalIds,
        revision = revision,
        articleId = articleId,
        slug = slug,
        article = article,
      )
    }
  }
}
