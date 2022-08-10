/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import enumeratum._
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.domain.Availability
import no.ndla.draftapi.Props
import no.ndla.language.Language.getSupportedLanguages
import org.json4s.FieldSerializer._
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers, JavaTypesSerializers}
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import scalikejdbc._

import java.time.LocalDateTime
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
    created: LocalDateTime,
    updated: LocalDateTime,
    updatedBy: String,
    published: LocalDateTime,
    articleType: ArticleType,
    notes: Seq[EditorNote],
    previousVersionsNotes: Seq[EditorNote],
    editorLabels: Seq[String],
    grepCodes: Seq[String],
    conceptIds: Seq[Long],
    availability: Availability.Value = Availability.everyone,
    relatedContent: Seq[RelatedContent],
    revisionMeta: Seq[RevisionMeta]
) extends Content {

  def supportedLanguages: Seq[String] =
    getSupportedLanguages(title, visualElement, introduction, metaDescription, tags, content, metaImage)
}

object ArticleStatusAction extends Enumeration {
  val UPDATE: ArticleStatusAction.Value = Value
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
          ValidationException("status", s"'$s' is not a valid article status. Must be one of $validStatuses")
        )
    }

  def valueOf(s: String): Option[ArticleStatus.Value] = values.find(_.toString == s.toUpperCase)
}

sealed abstract class ArticleType(override val entryName: String) extends EnumEntry {
  override def toString: String = super.toString
}

object ArticleType extends Enum[ArticleType] {
  case object Standard     extends ArticleType("standard")
  case object TopicArticle extends ArticleType("topic-article")

  val values: IndexedSeq[ArticleType] = findValues

  def all: Seq[String]                        = ArticleType.values.map(_.entryName)
  def valueOf(s: String): Option[ArticleType] = ArticleType.withNameOption(s)

  def valueOfOrError(s: String): ArticleType =
    valueOf(s).getOrElse(
      throw ValidationException(
        "articleType",
        s"'$s' is not a valid article type. Valid options are ${all.mkString(",")}."
      )
    )
}

case class Agreement(
    id: Option[Long],
    title: String,
    content: String,
    copyright: Copyright,
    created: LocalDateTime,
    updated: LocalDateTime,
    updatedBy: String
) extends Content

case class UserData(
    id: Option[Long],
    userId: String,
    savedSearches: Option[Seq[String]],
    latestEditedArticles: Option[Seq[String]],
    favoriteSubjects: Option[Seq[String]]
)

trait DBArticle {
  this: Props =>

  object DBArticle extends SQLSyntaxSupport[Article] {

    val jsonEncoder: Formats = DefaultFormats.withLong +
      new EnumNameSerializer(ArticleStatus) +
      Json4s.serializer(ArticleType) +
      Json4s.serializer(RevisionStatus) +
      new EnumNameSerializer(Availability) ++
      JavaTimeSerializers.all ++
      JavaTypesSerializers.all

    val repositorySerializer = jsonEncoder +
      FieldSerializer[Article](
        ignore("id") orElse
          ignore("revision")
      )

    override val tableName       = "articledata"
    override lazy val schemaName = Some(props.MetaSchema)

    def fromResultSet(lp: SyntaxProvider[Article])(rs: WrappedResultSet): Article = fromResultSet(lp.resultName)(rs)

    def fromResultSet(lp: ResultName[Article])(rs: WrappedResultSet): Article = {
      implicit val formats = jsonEncoder
      val meta             = read[Article](rs.string(lp.c("document")))
      meta.copy(
        id = Some(rs.long(lp.c("article_id"))),
        revision = Some(rs.int(lp.c("revision")))
      )
    }
  }

  object DBAgreement extends SQLSyntaxSupport[Agreement] {
    val JSonSerializer: Formats = org.json4s.DefaultFormats +
      FieldSerializer[Agreement](ignore("id")) ++
      JavaTimeSerializers.all

    implicit val formats    = JSonSerializer
    override val tableName  = "agreementdata"
    override val schemaName = Some(props.MetaSchema)

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

  }

  object DBUserData extends SQLSyntaxSupport[UserData] {
    implicit val formats         = org.json4s.DefaultFormats
    override val tableName       = "userdata"
    lazy override val schemaName = Some(props.MetaSchema)

    val JSonSerializer = FieldSerializer[UserData](
      ignore("id")
    )

    def fromResultSet(lp: SyntaxProvider[UserData])(rs: WrappedResultSet): UserData =
      fromResultSet(lp.resultName)(rs)

    def fromResultSet(lp: ResultName[UserData])(rs: WrappedResultSet): UserData = {
      val userData = read[UserData](rs.string(lp.c("document")))
      userData.copy(
        id = Some(rs.long(lp.c("id")))
      )
    }
  }
}
