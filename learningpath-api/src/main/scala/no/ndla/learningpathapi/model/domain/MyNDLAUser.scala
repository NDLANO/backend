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

case class MyNDLAUserDocument(favoriteSubjects: Seq[String]) {
  def toFullUser(
      id: Long,
      feideId: FeideID
  ): MyNDLAUser = {
    MyNDLAUser(
      id = id,
      feideId = feideId,
      favoriteSubjects = favoriteSubjects
    )
  }
}

case class MyNDLAUser(id: Long, feideId: FeideID, favoriteSubjects: Seq[String]) {
  def toDocument: MyNDLAUserDocument = MyNDLAUserDocument(
    favoriteSubjects = favoriteSubjects
  )
}

trait DBMyNDLAUser {
  this: Props =>

  object DBMyNDLAUser extends SQLSyntaxSupport[MyNDLAUser] {
    implicit val jsonEncoder: Formats            = DefaultFormats
    override val tableName                       = "my_ndla_users"
    override lazy val schemaName: Option[String] = Some(props.MetaSchema)

    val repositorySerializer: Formats = jsonEncoder + FieldSerializer[MyNDLAUser](
      ignore("id") orElse
        ignore("feideId")
    )

    def fromResultSet(lp: SyntaxProvider[MyNDLAUser])(rs: WrappedResultSet): MyNDLAUser =
      fromResultSet((s: String) => lp.resultName.c(s))(rs)

    def fromResultSet(rs: WrappedResultSet): MyNDLAUser = fromResultSet((s: String) => s)(rs)

    def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): MyNDLAUser = {
      val metaData = read[MyNDLAUserDocument](rs.string(colNameWrapper("document")))
      val id       = rs.long(colNameWrapper("id"))
      val feideId  = rs.string(colNameWrapper("feide_id"))

      metaData.toFullUser(
        id = id,
        feideId = feideId
      )
    }
  }
}
