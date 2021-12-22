/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

import cats.implicits._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.{Decoder, Encoder}
import no.ndla.frontpageapi.FrontpageApiProperties
import scalikejdbc.{WrappedResultSet, _}
import no.ndla.frontpageapi.model.domain.Language.getSupportedLanguages

import scala.util.Try

case class SubjectFrontPageData(id: Option[Long],
                                name: String,
                                filters: Option[List[String]],
                                layout: LayoutType.Value,
                                twitter: Option[String],
                                facebook: Option[String],
                                bannerImage: BannerImage,
                                about: Seq[AboutSubject],
                                metaDescription: Seq[MetaDescription],
                                topical: Option[String],
                                mostRead: List[String],
                                editorsChoices: List[String],
                                latestContent: Option[List[String]],
                                goTo: List[String]) {

  def supportedLanguages: Seq[String] = getSupportedLanguages(Seq(about, metaDescription))
}

object SubjectFrontPageData extends SQLSyntaxSupport[SubjectFrontPageData] {
  override val tableName = "subjectpage"
  override val schemaName: Option[String] = FrontpageApiProperties.MetaSchema.some

  implicit val elementDecoder: Decoder[VisualElementType.Value] = Decoder.decodeEnumeration(VisualElementType)
  implicit val elementEncoder: Encoder[VisualElementType.Value] = Encoder.encodeEnumeration(VisualElementType)
  implicit val layoutDecoder: Decoder[LayoutType.Value] = Decoder.decodeEnumeration(LayoutType)
  implicit val layoutEncoder: Encoder[LayoutType.Value] = Encoder.encodeEnumeration(LayoutType)

  private[domain] def decodeJson(json: String, id: Long): Try[SubjectFrontPageData] = {
    parse(json).flatMap(_.as[SubjectFrontPageData]).map(_.copy(id = id.some)).toTry
  }

  def fromDb(lp: SyntaxProvider[SubjectFrontPageData])(rs: WrappedResultSet): Try[SubjectFrontPageData] =
    fromDb(lp.resultName)(rs)

  private def fromDb(lp: ResultName[SubjectFrontPageData])(rs: WrappedResultSet): Try[SubjectFrontPageData] = {
    val id = rs.long(lp.c("id"))
    val document = rs.string(lp.c("document"))

    decodeJson(document, id)
  }

}
