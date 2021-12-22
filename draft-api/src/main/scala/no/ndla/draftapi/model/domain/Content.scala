/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import java.util.Date
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.model.domain.Language.getSupportedLanguages
import no.ndla.validation.{ValidationException, ValidationMessage}
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.FieldSerializer._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import scalikejdbc._

import scala.util.{Failure, Success, Try}

sealed trait Content {
  def id: Option[Long]
}

case class Article(
    id: Option[Long],
    revision: Option[Int],
    status: Status,
    title: Seq[ArticleTitle],
    content: Seq[ArticleContent],
    copyright: Option[Copyright],
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
    articleType: ArticleType.Value,
    notes: Seq[EditorNote],
    previousVersionsNotes: Seq[EditorNote],
    editorLabels: Seq[String],
    grepCodes: Seq[String],
    conceptIds: Seq[Long],
    availability: Availability.Value = Availability.everyone,
    relatedContent: Seq[RelatedContent]
) extends Content {

  def supportedLanguages: Seq[String] =
    getSupportedLanguages(Seq(title, visualElement, introduction, metaDescription, tags, content, metaImage))
}

object Article extends SQLSyntaxSupport[Article] {

  val jsonEncoder: Formats = DefaultFormats.withLong +
    new EnumNameSerializer(ArticleStatus) +
    new EnumNameSerializer(ArticleType) +
    new EnumNameSerializer(Availability)

  val repositorySerializer = jsonEncoder +
    FieldSerializer[Article](
      ignore("id") orElse
        ignore("revision")
    )

  override val tableName = "articledata"
  override lazy val schemaName = Some(DraftApiProperties.MetaSchema)

  def fromResultSet(lp: SyntaxProvider[Article])(rs: WrappedResultSet): Article = fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[Article])(rs: WrappedResultSet): Article = {
    implicit val formats = jsonEncoder
    val meta = read[Article](rs.string(lp.c("document")))
    meta.copy(
      id = Some(rs.long(lp.c("article_id"))),
      revision = Some(rs.int(lp.c("revision")))
    )
  }
}

object ArticleStatusAction extends Enumeration {
  val UPDATE = Value
}

object ArticleStatus extends Enumeration {

  val IMPORTED, DRAFT, PUBLISHED, PROPOSAL, QUEUED_FOR_PUBLISHING, USER_TEST, AWAITING_QUALITY_ASSURANCE,
  QUEUED_FOR_LANGUAGE, TRANSLATED, QUALITY_ASSURED, QUALITY_ASSURED_DELAYED, QUEUED_FOR_PUBLISHING_DELAYED,
  AWAITING_UNPUBLISHING, UNPUBLISHED, AWAITING_ARCHIVING, ARCHIVED = Value

  def valueOfOrError(s: String): Try[ArticleStatus.Value] =
    valueOf(s) match {
      case Some(st) => Success(st)
      case None =>
        val validStatuses = values.map(_.toString).mkString(", ")
        Failure(
          new ValidationException(
            errors =
              Seq(ValidationMessage("status", s"'$s' is not a valid article status. Must be one of $validStatuses"))))
    }

  def valueOf(s: String): Option[ArticleStatus.Value] = values.find(_.toString == s.toUpperCase)
}

object ArticleType extends Enumeration {
  val Standard = Value("standard")
  val TopicArticle = Value("topic-article")

  def all = ArticleType.values.map(_.toString).toSeq
  def valueOf(s: String): Option[ArticleType.Value] = ArticleType.values.find(_.toString == s)

  def valueOfOrError(s: String): ArticleType.Value =
    valueOf(s).getOrElse(throw new ValidationException(errors = List(
      ValidationMessage("articleType", s"'$s' is not a valid article type. Valid options are ${all.mkString(",")}."))))
}

case class Agreement(
    id: Option[Long],
    title: String,
    content: String,
    copyright: Copyright,
    created: Date,
    updated: Date,
    updatedBy: String
) extends Content

object Agreement extends SQLSyntaxSupport[Agreement] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "agreementdata"
  override val schemaName = Some(DraftApiProperties.MetaSchema)

  def fromResultSet(lp: SyntaxProvider[Agreement])(rs: WrappedResultSet): Agreement = fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[Agreement])(rs: WrappedResultSet): Agreement = {
    val meta = read[Agreement](rs.string(lp.c("document")))
    Agreement(
      id = Some(rs.long(lp.c("id"))),
      title = meta.title,
      content = meta.content,
      copyright = meta.copyright,
      created = meta.created,
      updated = meta.updated,
      updatedBy = meta.updatedBy
    )
  }

  val JSonSerializer = FieldSerializer[Agreement](
    ignore("id")
  )

}

case class UserData(
    id: Option[Long],
    userId: String,
    savedSearches: Option[Seq[String]],
    latestEditedArticles: Option[Seq[String]],
    favoriteSubjects: Option[Seq[String]]
)

object UserData extends SQLSyntaxSupport[UserData] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "userdata"
  lazy override val schemaName = Some(DraftApiProperties.MetaSchema)

  val JSonSerializer = FieldSerializer[UserData](
    ignore("id")
  )

  def fromResultSet(lp: SyntaxProvider[UserData])(rs: WrappedResultSet): UserData =
    fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[UserData])(rs: WrappedResultSet): UserData = {
    val userData = read[UserData](rs.string(lp.c("document")))
    userData.copy(
      id = Some(rs.long(lp.c("id"))),
    )
  }
}
