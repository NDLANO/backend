/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.db.migration

import io.circe.generic.auto.*
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

class V5__add_meta_description extends BaseJavaMigration {

  implicit val decoder: Decoder[V1_DBFrontPageData] = deriveDecoder
  implicit val encoder: Encoder[V1_DBFrontPageData] = deriveEncoder

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
    parse(subjectPageData.document).flatMap(_.as[V4_SubjectFrontPageData]).toTry match {
      case Success(value) =>
        val newSubjectPage = V5_SubjectFrontPageData(
          id = value.id,
          name = value.name,
          filters = value.filters,
          layout = value.layout,
          twitter = value.twitter,
          facebook = value.facebook,
          bannerImage = value.bannerImage,
          about = value.about,
          metaDescription = Seq(),
          topical = value.topical,
          mostRead = value.mostRead,
          editorsChoices = value.editorsChoices,
          latestContent = value.latestContent,
          goTo = value.goTo
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

case class V5_MetaDescription(metaDescription: String, language: String)
case class V5_SubjectFrontPageData(
    id: Option[Long],
    name: String,
    filters: Option[List[String]],
    layout: String,
    twitter: Option[String],
    facebook: Option[String],
    bannerImage: V2_BannerImage,
    about: Seq[V4_AboutSubject],
    metaDescription: Seq[V5_MetaDescription],
    topical: Option[String],
    mostRead: List[String],
    editorsChoices: List[String],
    latestContent: Option[List[String]],
    goTo: List[String]
)
