/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Content
import no.ndla.common.model.domain.draft.{Draft, DraftCopyright}
import scalikejdbc.*

case class Agreement(
    id: Option[Long],
    title: String,
    content: String,
    copyright: DraftCopyright,
    created: NDLADate,
    updated: NDLADate,
    updatedBy: String
) extends Content

case class UserData(
    id: Option[Long],
    userId: String,
    savedSearches: Option[Seq[String]],
    latestEditedArticles: Option[Seq[String]],
    latestEditedConcepts: Option[Seq[String]],
    favoriteSubjects: Option[Seq[String]]
)

object DBArticle extends SQLSyntaxSupport[Draft] {
  override val tableName = "articledata"

  def fromResultSet(lp: SyntaxProvider[Draft])(rs: WrappedResultSet): Draft = fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[Draft])(rs: WrappedResultSet): Draft = {
    val meta = CirceUtil.unsafeParseAs[Draft](rs.string(lp.c("document")))
    val slug = rs.stringOpt(lp.c("slug"))
    meta.copy(
      id = Some(rs.long(lp.c("article_id"))),
      revision = Some(rs.int(lp.c("revision"))),
      slug = slug
    )
  }
}

object Agreement extends SQLSyntaxSupport[Agreement] {
  override val tableName                   = "agreementdata"
  implicit val encoder: Encoder[Agreement] = deriveEncoder
  implicit val decoder: Decoder[Agreement] = deriveDecoder

  def fromResultSet(lp: SyntaxProvider[Agreement])(rs: WrappedResultSet): Agreement = fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[Agreement])(rs: WrappedResultSet): Agreement = {
    val meta = CirceUtil.unsafeParseAs[Agreement](rs.string(lp.c("document")))
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

object UserData extends SQLSyntaxSupport[UserData] {
  override val tableName = "userdata"

  implicit val encoder: Encoder[UserData] = deriveEncoder
  implicit val decoder: Decoder[UserData] = deriveDecoder

  def fromResultSet(lp: SyntaxProvider[UserData])(rs: WrappedResultSet): UserData =
    fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[UserData])(rs: WrappedResultSet): UserData = {
    val userData = CirceUtil.unsafeParseAs[UserData](rs.string(lp.c("document")))
    userData.copy(
      id = Some(rs.long(lp.c("id")))
    )
  }
}
