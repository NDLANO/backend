/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api.SavedSearchDTO
import scalikejdbc.*

case class UserData(
    id: Option[Long],
    userId: String,
    savedSearches: Option[Seq[SavedSearchDTO]],
    latestEditedArticles: Option[Seq[String]],
    latestEditedConcepts: Option[Seq[String]],
    latestEditedLearningpaths: Option[Seq[String]],
    favoriteSubjects: Option[Seq[String]],
)

class DBArticle(using props: Props) extends SQLSyntaxSupport[Draft] {
  override def tableName                  = "articledata"
  override def schemaName: Option[String] = Some(props.MetaSchema)

  def fromResultSet(d: SyntaxProvider[Draft])(rs: WrappedResultSet): Draft = fromResultSet(d.resultName)(rs)

  def fromResultSet(d: ResultName[Draft])(rs: WrappedResultSet): Draft = {
    val meta        = CirceUtil.unsafeParseAs[Draft](rs.string(d.c("document")))
    val slug        = rs.stringOpt(d.c("slug"))
    val externalIds = rs
      .arrayOpt(d.c("external_id"))
      .map(_.getArray.asInstanceOf[Array[String]].toList.flatMap(Option(_)))
    meta.copy(
      id = Some(rs.long(d.c("article_id"))),
      revision = Some(rs.int(d.c("revision"))),
      externalIds = externalIds match {
        case Some(Nil) => None
        case _         => externalIds
      },
      slug = slug,
    )
  }
}

object UserData {

  implicit val encoder: Encoder[UserData] = deriveEncoder
  implicit val decoder: Decoder[UserData] = deriveDecoder

  def fromResultSet(u: SyntaxProvider[UserData])(rs: WrappedResultSet): UserData = fromResultSet(u.resultName)(rs)

  def fromResultSet(u: ResultName[UserData])(rs: WrappedResultSet): UserData = {
    val userData = CirceUtil.unsafeParseAs[UserData](rs.string(u.c("document")))
    userData.copy(id = Some(rs.long(u.c("id"))))
  }
}

class DBUserData(using props: Props) extends SQLSyntaxSupport[UserData] {
  override def tableName: String          = "userdata"
  override def schemaName: Option[String] = Some(props.MetaSchema)
}
