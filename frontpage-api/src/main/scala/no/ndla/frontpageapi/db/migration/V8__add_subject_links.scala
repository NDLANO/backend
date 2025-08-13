/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.db.migration

import io.circe.generic.semiauto.*
import io.circe.parser.parse
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import no.ndla.frontpageapi.repository.*
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession}
import scalikejdbc.*

import scala.util.{Failure, Success}

class V8__add_subject_links extends BaseJavaMigration {
  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      subjectPageData.flatMap(convertSubjectpage).foreach(update)
    }

  private def subjectPageData(implicit session: DBSession): List[V2_DBSubjectPage] = {
    sql"select id, document from subjectpage"
      .map(rs => V2_DBSubjectPage(rs.long("id"), rs.string("document")))
      .list()
  }

  def convertSubjectpage(subjectPageData: V2_DBSubjectPage): Option[V2_DBSubjectPage] = {
    parse(subjectPageData.document).flatMap(_.as[V5_SubjectFrontPageData]).toTry match {
      case Success(value) =>
        val newSubjectPage = V8_SubjectFrontPageData(
          id = value.id,
          name = value.name,
          bannerImage = value.bannerImage,
          about = value.about,
          metaDescription = value.metaDescription,
          editorsChoices = value.editorsChoices,
          connectedTo = List(),
          buildsOn = List(),
          leadsTo = List()
        )
        Some(V2_DBSubjectPage(subjectPageData.id, newSubjectPage.asJson.noSpacesDropNull))
      case Failure(_) => None
    }
  }

  private def update(subjectPageData: V2_DBSubjectPage)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(subjectPageData.document)

    sql"update subjectpage set document = $dataObject where id = ${subjectPageData.id}"
      .update()
  }
}

case class V8_SubjectFrontPageData(
    id: Option[Long],
    name: String,
    bannerImage: V2_BannerImage,
    about: Seq[V4_AboutSubject],
    metaDescription: Seq[V5_MetaDescription],
    editorsChoices: List[String],
    connectedTo: List[String],
    buildsOn: List[String],
    leadsTo: List[String]
)
object V8_SubjectFrontPageData {
  implicit val encoder: Encoder[V8_SubjectFrontPageData] = deriveEncoder
  implicit val decoder: Decoder[V8_SubjectFrontPageData] = deriveDecoder
}
