/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.Props
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import scalikejdbc._

case class FeideUserDocument(favoriteSubjects: Seq[String]) {
  def toFullUser(
      id: Long,
      feideId: FeideID
  ): FeideUser = {
    FeideUser(
      id = id,
      feideId = feideId,
      favoriteSubjects = favoriteSubjects
    )
  }
}

case class FeideUser(id: Long, feideId: FeideID, favoriteSubjects: Seq[String]) {
  def toDocument: FeideUserDocument = FeideUserDocument(
    favoriteSubjects = favoriteSubjects
  )
}

trait DBFeideUser {
  this: Props =>

  object DBFeideUser extends SQLSyntaxSupport[FeideUser] {
    implicit val jsonEncoder: Formats            = DefaultFormats
    override val tableName                       = "feide_users"
    override lazy val schemaName: Option[String] = Some(props.MetaSchema)

    val repositorySerializer: Formats = jsonEncoder + FieldSerializer[FeideUser](
      ignore("id") orElse
        ignore("feideId")
    )

    def fromResultSet(lp: SyntaxProvider[FeideUser])(rs: WrappedResultSet): FeideUser =
      fromResultSet((s: String) => lp.resultName.c(s))(rs)

    def fromResultSet(rs: WrappedResultSet): FeideUser = fromResultSet((s: String) => s)(rs)

    def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): FeideUser = {
      val metaData = read[FeideUserDocument](rs.string(colNameWrapper("document")))
      val id       = rs.long(colNameWrapper("id"))
      val feideId  = rs.string(colNameWrapper("feide_id"))

      metaData.toFullUser(
        id = id,
        feideId = feideId
      )
    }
  }
}
