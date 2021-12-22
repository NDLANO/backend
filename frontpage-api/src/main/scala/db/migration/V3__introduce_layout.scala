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

class V3__introduce_layout extends BaseJavaMigration {

  implicit val decoder: Decoder[V1_DBFrontPageData] = deriveDecoder
  implicit val encoder: Encoder[V1_DBFrontPageData] = deriveEncoder

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)
    db.withinTx { implicit session =>
      subjectPageData.flatMap(convertSubjectpage).map(update)
    }
  }

  private def subjectPageData(implicit session: DBSession): List[DBSubjectPage] = {
    sql"select id, document from subjectpage"
      .map(rs => DBSubjectPage(rs.long("id"), rs.string("document")))
      .list()
  }

  private def convertSubjectpage(subjectPageData: DBSubjectPage): Option[DBSubjectPage] = {
    parse(subjectPageData.document).flatMap(_.as[V2_SubjectFrontPageData]).toTry match {
      case Success(value) =>
        val newSubjectPage = V3_SubjectFrontPageData(
          id = value.id,
          name = value.name,
          filters = value.filters,
          layout = if (value.displayInTwoColumns) "double" else "single",
          twitter = value.twitter,
          facebook = value.facebook,
          bannerImage = value.bannerImage,
          about = value.about,
          topical = value.topical,
          mostRead = value.mostRead,
          editorsChoices = value.editorsChoices,
          latestContent = value.latestContent,
          goTo = value.goTo
        )
        Some(DBSubjectPage(subjectPageData.id, newSubjectPage.asJson.noSpacesDropNull))
      case Failure(_) => None
    }
  }

  private def update(subjectPageData: DBSubjectPage)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(subjectPageData.document)

    sql"update subjectpage set document = $dataObject where id = ${subjectPageData.id}"
      .update()
  }
}

case class DBSubjectPage(id: Long, document: String)
case class V2_SubjectFrontPageData(id: Option[Long],
                                   name: String,
                                   filters: Option[List[String]],
                                   displayInTwoColumns: Boolean,
                                   twitter: Option[String],
                                   facebook: Option[String],
                                   bannerImage: V2_BannerImage,
                                   about: Option[V2_AboutSubject],
                                   topical: Option[String],
                                   mostRead: List[String],
                                   editorsChoices: List[String],
                                   latestContent: Option[List[String]],
                                   goTo: List[String])
case class V2_BannerImage(mobileImageId: Long, desktopImageId: Long)
case class V2_AboutSubject(title: String, description: String, visualElement: V2_VisualElement)
case class V2_VisualElement(`type`: String, id: String, alt: Option[String])

case class V3_SubjectFrontPageData(id: Option[Long],
                                   name: String,
                                   filters: Option[List[String]],
                                   layout: String,
                                   twitter: Option[String],
                                   facebook: Option[String],
                                   bannerImage: V2_BannerImage,
                                   about: Option[V2_AboutSubject],
                                   topical: Option[String],
                                   mostRead: List[String],
                                   editorsChoices: List[String],
                                   latestContent: Option[List[String]],
                                   goTo: List[String])
