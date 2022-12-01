/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import no.ndla.articleapi.Props
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.domain.{
  ArticleIntroduction,
  Availability,
  RelatedContent,
  RequiredLibrary,
  Tag,
  Title,
  VisualElement
}
import no.ndla.common.model.domain.article.Copyright
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.FieldSerializer._
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.native.Serialization._
import scalikejdbc._

import java.time.LocalDateTime

sealed trait Content {
  def id: Option[Long]
}

case class Article(
    id: Option[Long],
    revision: Option[Int],
    title: Seq[Title],
    content: Seq[ArticleContent],
    copyright: Copyright,
    tags: Seq[Tag],
    requiredLibraries: Seq[RequiredLibrary],
    visualElement: Seq[VisualElement],
    introduction: Seq[ArticleIntroduction],
    metaDescription: Seq[ArticleMetaDescription],
    metaImage: Seq[ArticleMetaImage],
    created: LocalDateTime,
    updated: LocalDateTime,
    updatedBy: String,
    published: LocalDateTime,
    articleType: String,
    grepCodes: Seq[String],
    conceptIds: Seq[Long],
    availability: Availability.Value = Availability.everyone,
    relatedContent: Seq[RelatedContent],
    revisionDate: Option[LocalDateTime]
) extends Content

trait DBArticle {
  this: Props =>
  object Article extends SQLSyntaxSupport[Article] {
    val jsonEncoder: Formats = DefaultFormats.withLong + new EnumNameSerializer(Availability) ++ JavaTimeSerializers.all
    override val tableName   = "contentdata"
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

object ArticleType extends Enumeration {
  val Standard: ArticleType.Value     = Value("standard")
  val TopicArticle: ArticleType.Value = Value("topic-article")

  def all: Seq[String]                              = ArticleType.values.map(_.toString).toSeq
  def valueOf(s: String): Option[ArticleType.Value] = ArticleType.values.find(_.toString == s)

  def valueOfOrError(s: String): ArticleType.Value =
    valueOf(s).getOrElse(
      throw ValidationException(
        "articleType",
        s"'$s' is not a valid article type. Valid options are ${all.mkString(",")}."
      )
    )
}
