/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import java.util.Date

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.validation.{ValidationException, ValidationMessage}
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.FieldSerializer._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import scalikejdbc._

sealed trait Content {
  def id: Option[Long]
}

case class Article(id: Option[Long],
                   revision: Option[Int],
                   title: Seq[ArticleTitle],
                   content: Seq[ArticleContent],
                   copyright: Copyright,
                   tags: Seq[ArticleTag],
                   requiredLibraries: Seq[RequiredLibrary],
                   visualElement: Seq[VisualElement],
                   introduction: Seq[ArticleIntroduction],
                   metaDescription: Seq[ArticleMetaDescription],
                   metaImage: Seq[ArticleMetaImage],
                   created: Date,
                   updated: Date,
                   updatedBy: String,
                   published: Date,
                   articleType: String,
                   grepCodes: Seq[String],
                   conceptIds: Seq[Long],
                   availability: Availability.Value = Availability.everyone,
                   relatedContent: Seq[RelatedContent])
    extends Content

object Article extends SQLSyntaxSupport[Article] {

  val jsonEncoder: Formats = DefaultFormats.withLong + new EnumNameSerializer(Availability)
  override val tableName = "contentdata"
  override lazy val schemaName: Option[String] = Some(ArticleApiProperties.MetaSchema)

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

object ArticleType extends Enumeration {
  val Standard: ArticleType.Value = Value("standard")
  val TopicArticle: ArticleType.Value = Value("topic-article")

  def all: Seq[String] = ArticleType.values.map(_.toString).toSeq
  def valueOf(s: String): Option[ArticleType.Value] = ArticleType.values.find(_.toString == s)

  def valueOfOrError(s: String): ArticleType.Value =
    valueOf(s).getOrElse(throw new ValidationException(errors = List(
      ValidationMessage("articleType", s"'$s' is not a valid article type. Valid options are ${all.mkString(",")}."))))
}
