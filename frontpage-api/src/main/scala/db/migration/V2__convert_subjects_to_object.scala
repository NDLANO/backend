/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration
import java.sql.Connection

import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import no.ndla.frontpageapi.repository._
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Failure, Success}

class V2__convert_subjects_to_object extends BaseJavaMigration {

  implicit val decoder: Decoder[V1_DBFrontPageData] = deriveDecoder
  implicit val encoder: Encoder[V1_DBFrontPageData] = deriveEncoder

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)
    db.withinTx { implicit session =>
      frontPageData.flatMap(convertSubjects).map(update)
    }
  }

  def frontPageData(implicit session: DBSession): Option[DBFrontPage] = {
    sql"select id, document from mainfrontpage"
      .map(rs => DBFrontPage(rs.long("id"), rs.string("document")))
      .single()
  }

  def convertSubjects(frontPage: DBFrontPage): Option[V2_FrontPageData] = {
    parse(frontPage.document).flatMap(_.as[V1_DBFrontPageData]).toTry match {
      case Success(value) =>
        Some(V2_FrontPageData(value.topical, toDomainCategories(value.categories)))
      case Failure(_) => None
    }
  }

  private def toDomainCategories(dbCategories: List[V1_DBSubjectCollection]): List[V2_SubjectCollection] = {
    dbCategories.map(sc => V2_SubjectCollection(sc.name, sc.subjects.map(s => V2_SubjectFilters(s, List()))))
  }

  private def update(frontPageData: V2_FrontPageData)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(frontPageData.asJson.noSpacesDropNull)

    sql"update mainfrontpage set document = $dataObject"
      .update()
  }
}

case class V2_FrontPageData(topical: List[String], categories: List[V2_SubjectCollection])
case class V2_SubjectCollection(name: String, subjects: List[V2_SubjectFilters])
case class V2_SubjectFilters(id: String, filters: List[String])

case class DBFrontPage(id: Long, document: String)
case class V1_DBFrontPageData(topical: List[String], categories: List[V1_DBSubjectCollection])
case class V1_DBSubjectCollection(name: String, subjects: List[String])
