/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import enumeratum.Json4s
import no.ndla.articleapi.Props
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.{ArticleType, Availability}
import no.ndla.common.model.domain.article.Article
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.FieldSerializer._
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.native.Serialization._
import scalikejdbc._

trait DBArticle {
  this: Props =>
  object Article extends SQLSyntaxSupport[Article] {
    val jsonEncoder: Formats =
      DefaultFormats.withLong +
        new EnumNameSerializer(Availability) ++
        JavaTimeSerializers.all +
        Json4s.serializer(ArticleType) +
        NDLADate.Json4sSerializer

    override val tableName                       = "contentdata"
    override lazy val schemaName: Option[String] = Some(props.MetaSchema)

    def fromResultSet(lp: SyntaxProvider[Article])(rs: WrappedResultSet): ArticleRow =
      fromResultSet(lp.resultName)(rs)

    val repositorySerializer: Formats = jsonEncoder +
      FieldSerializer[Article](ignore("id") orElse ignore("slug"))

    def fromResultSet(lp: ResultName[Article])(rs: WrappedResultSet): ArticleRow = {
      implicit val formats: Formats = repositorySerializer

      val articleId = rs.long(lp.c("article_id"))
      val rowId     = rs.long(lp.c("id"))
      val document  = rs.stringOpt(lp.c("document"))
      val revision  = rs.int(lp.c("revision"))
      val slug      = rs.stringOpt(lp.c("slug"))

      val article = document.map(jsonStr => {
        val meta = read[Article](jsonStr)
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
