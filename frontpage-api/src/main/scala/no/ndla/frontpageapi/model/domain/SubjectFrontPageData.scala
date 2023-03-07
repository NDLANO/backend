/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

import cats.implicits._
import io.circe.generic.auto._
import io.circe.generic.semiauto
import io.circe.parser._
import io.circe.{Decoder, Encoder}
import no.ndla.frontpageapi.Props
import no.ndla.language.Language.getSupportedLanguages
import scalikejdbc.{WrappedResultSet, _}

import scala.util.Try

case class SubjectFrontPageData(
    id: Option[Long],
    name: String,
    filters: Option[List[String]],
    layout: LayoutType,
    twitter: Option[String],
    facebook: Option[String],
    bannerImage: BannerImage,
    about: Seq[AboutSubject],
    metaDescription: Seq[MetaDescription],
    topical: Option[String],
    mostRead: List[String],
    editorsChoices: List[String],
    latestContent: Option[List[String]],
    goTo: List[String]
) {

  def supportedLanguages: Seq[String] = getSupportedLanguages(about, metaDescription)
}

object SubjectFrontPageData {
  implicit val layoutDecoder: Decoder[LayoutType] = semiauto.deriveDecoder
  implicit val layoutEncoder: Encoder[LayoutType] = semiauto.deriveEncoder

  private[domain] def decodeJson(json: String, id: Long): Try[SubjectFrontPageData] = {
    parse(json).flatMap(_.as[SubjectFrontPageData]).map(_.copy(id = id.some)).toTry
  }
}

trait DBSubjectFrontPageData {
  this: Props =>

  object DBSubjectFrontPageData extends SQLSyntaxSupport[SubjectFrontPageData] {
    override val tableName                  = "subjectpage"
    override val schemaName: Option[String] = props.MetaSchema.some

    def fromDb(lp: SyntaxProvider[SubjectFrontPageData])(rs: WrappedResultSet): Try[SubjectFrontPageData] =
      fromDb(lp.resultName)(rs)

    private def fromDb(lp: ResultName[SubjectFrontPageData])(rs: WrappedResultSet): Try[SubjectFrontPageData] = {
      val id       = rs.long(lp.c("id"))
      val document = rs.string(lp.c("document"))

      SubjectFrontPageData.decodeJson(document, id)
    }

  }
}
