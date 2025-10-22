/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.db.migration

import io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import no.ndla.frontpageapi.repository.*
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc.*

import scala.util.{Failure, Success}

class V3__introduce_layout extends BaseJavaMigration {

  implicit val decoder: Decoder[V1_DBFrontPageData]        = deriveDecoder
  implicit val encoder: Encoder[V1_DBFrontPageData]        = deriveEncoder
  implicit val v2decoder: Decoder[V2_SubjectFrontPageData] = deriveDecoder
  implicit val v2encoder: Encoder[V2_SubjectFrontPageData] = deriveEncoder
  implicit val v3decoder: Decoder[V3_SubjectFrontPageData] = deriveDecoder
  implicit val v3encoder: Encoder[V3_SubjectFrontPageData] = deriveEncoder

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      subjectPageData.flatMap(convertSubjectpage).foreach(update)
    }

  private def subjectPageData(implicit session: DBSession): List[V2_DBSubjectPage] = {
    sql"select id, document from subjectpage".map(rs => V2_DBSubjectPage(rs.long("id"), rs.string("document"))).list()
  }

  private def convertSubjectpage(subjectPageData: V2_DBSubjectPage): Option[V2_DBSubjectPage] = {
    parse(subjectPageData.document).flatMap(_.as[V2_SubjectFrontPageData]).toTry match {
      case Success(value) =>
        val newSubjectPage = V3_SubjectFrontPageData(
          id = value.id,
          name = value.name,
          filters = value.filters,
          layout =
            if (value.displayInTwoColumns) "double"
            else "single",
          twitter = value.twitter,
          facebook = value.facebook,
          bannerImage = value.bannerImage,
          about = value.about,
          topical = value.topical,
          mostRead = value.mostRead,
          editorsChoices = value.editorsChoices,
          latestContent = value.latestContent,
          goTo = value.goTo,
        )
        Some(V2_DBSubjectPage(subjectPageData.id, newSubjectPage.asJson.noSpacesDropNull))
      case Failure(_) => None
    }
  }

  private def update(subjectPageData: V2_DBSubjectPage)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(subjectPageData.document)

    sql"update subjectpage set document = $dataObject where id = ${subjectPageData.id}".update()
  }
}

case class V2_DBSubjectPage(id: Long, document: String)
object V2_DBSubjectPage {
  implicit val encoder: Encoder[V2_DBSubjectPage] = deriveEncoder
  implicit val decoder: Decoder[V2_DBSubjectPage] = deriveDecoder
}
case class V2_SubjectFrontPageData(
    id: Option[Long],
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
    goTo: List[String],
)
case class V2_BannerImage(mobileImageId: Long, desktopImageId: Long)
object V2_BannerImage {
  implicit val encoder: Encoder[V2_BannerImage] = deriveEncoder
  implicit val decoder: Decoder[V2_BannerImage] = deriveDecoder
}
case class V2_AboutSubject(title: String, description: String, visualElement: V2_VisualElement)
object V2_AboutSubject {
  implicit val encoder: Encoder[V2_AboutSubject] = deriveEncoder
  implicit val decoder: Decoder[V2_AboutSubject] = deriveDecoder
}
case class V2_VisualElement(`type`: String, id: String, alt: Option[String])
object V2_VisualElement {
  implicit val encoder: Encoder[V2_VisualElement] = deriveEncoder
  implicit val decoder: Decoder[V2_VisualElement] = deriveDecoder
}

case class V3_SubjectFrontPageData(
    id: Option[Long],
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
    goTo: List[String],
)
object V3_SubjectFrontPageData {
  implicit val encoder: Encoder[V3_SubjectFrontPageData] = deriveEncoder
  implicit val decoder: Decoder[V3_SubjectFrontPageData] = deriveDecoder
}
