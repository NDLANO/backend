/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import enumeratum._
import no.ndla.common.model.domain.{ArticleType, Availability, Content}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus, Copyright, RevisionStatus}
import no.ndla.draftapi.Props
import org.json4s.FieldSerializer._
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers, JavaTypesSerializers}
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import scalikejdbc._

import java.time.LocalDateTime

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

  object DBArticle extends SQLSyntaxSupport[Draft] {

    val jsonEncoder: Formats = DefaultFormats.withLong +
      new EnumNameSerializer(DraftStatus) +
      Json4s.serializer(ArticleType) +
      Json4s.serializer(RevisionStatus) +
      new EnumNameSerializer(Availability) ++
      JavaTimeSerializers.all ++
      JavaTypesSerializers.all

    val repositorySerializer: Formats = jsonEncoder +
      FieldSerializer[Draft](
        ignore("id") orElse
          ignore("revision") orElse
          ignore("slug")
      )

    override val tableName       = "articledata"
    override lazy val schemaName: Some[String] = Some(props.MetaSchema)

    def fromResultSet(lp: SyntaxProvider[Draft])(rs: WrappedResultSet): Draft = fromResultSet(lp.resultName)(rs)

    def fromResultSet(lp: ResultName[Draft])(rs: WrappedResultSet): Draft = {
      implicit val formats = jsonEncoder
      val meta             = read[Draft](rs.string(lp.c("document")))
      val slug             = rs.stringOpt(lp.c("slug"))
      meta.copy(
        id = Some(rs.long(lp.c("article_id"))),
        revision = Some(rs.int(lp.c("revision"))),
        slug = slug
      )
    }
  }

  object DBAgreement extends SQLSyntaxSupport[Agreement] {
    val JSonSerializer: Formats = org.json4s.DefaultFormats +
      FieldSerializer[Agreement](ignore("id")) ++
      JavaTimeSerializers.all

    implicit val formats: Formats    = JSonSerializer
    override val tableName  = "agreementdata"
    override val schemaName: Some[String] = Some(props.MetaSchema)

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
    implicit val formats: DefaultFormats.type         = org.json4s.DefaultFormats
    override val tableName       = "userdata"
    lazy override val schemaName: Some[String] = Some(props.MetaSchema)

    val JSonSerializer: FieldSerializer[UserData] = FieldSerializer[UserData](
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
