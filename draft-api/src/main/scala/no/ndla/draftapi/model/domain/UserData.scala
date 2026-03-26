/*
 * Part of NDLA draft-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
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

object UserData {
  implicit val encoder: Encoder[UserData] = deriveEncoder
  implicit val decoder: Decoder[UserData] = deriveDecoder

  def fromResultSet(ud: SyntaxProvider[UserData])(rs: WrappedResultSet): UserData = fromResultSet(ud.resultName)(rs)

  def fromResultSet(ud: ResultName[UserData])(rs: WrappedResultSet): UserData = {
    val userData = CirceUtil.unsafeParseAs[UserData](rs.string(ud.c("document")))
    userData.copy(id = Some(rs.long(ud.c("id"))))
  }
}

class DBUserData(using props: Props) extends SQLSyntaxSupport[UserData] {
  override def tableName: String          = "userdata"
  override def schemaName: Option[String] = Some(props.MetaSchema)
}
