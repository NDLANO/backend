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
      DefaultFormats.withLong + new EnumNameSerializer(Availability) ++ JavaTimeSerializers.all + Json4s.serializer(
        ArticleType
      )
    override val tableName                       = "contentdata"
    override lazy val schemaName: Option[String] = Some(props.MetaSchema)

    def fromResultSet(lp: SyntaxProvider[Article])(rs: WrappedResultSet): Option[Article] =
      fromResultSet(lp.resultName)(rs)

    def fromResultSet(lp: ResultName[Article])(rs: WrappedResultSet): Option[Article] = {
      implicit val formats: Formats = repositorySerializer

      rs.stringOpt(lp.c("document"))
        .map(jsonStr => {
          val meta = read[Article](jsonStr)
          meta.copy(
            id = Some(rs.long(lp.c("article_id"))),
            revision = Some(rs.int(lp.c("revision")))
          )
        })
    }

    val repositorySerializer: Formats = jsonEncoder +
      FieldSerializer[Article](
        ignore("id")
      )
  }

}
