/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.domain

import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.{Decoder, Encoder}
import no.ndla.frontpageapi.Props
import no.ndla.language.Language.getSupportedLanguages
import scalikejdbc.{WrappedResultSet, *}

import scala.util.Try

case class SubjectPage(
    id: Option[Long],
    name: String,
    bannerImage: BannerImage,
    about: Seq[AboutSubject],
    metaDescription: Seq[MetaDescription],
    editorsChoices: List[String],
    connectedTo: List[String],
    buildsOn: List[String],
    leadsTo: List[String]
) {

  def supportedLanguages: Seq[String] = getSupportedLanguages(about, metaDescription)
}

object SubjectPage {
  implicit val decoder: Decoder[SubjectPage] = deriveDecoder
  implicit val encoder: Encoder[SubjectPage] = deriveEncoder
  private[domain] def decodeJson(json: String, id: Long): Try[SubjectPage] = {
    parse(json).flatMap(_.as[SubjectPage]).map(_.copy(id = id.some)).toTry
  }
}

trait DBSubjectPage {
  this: Props =>

  object DBSubjectPage extends SQLSyntaxSupport[SubjectPage] {
    override val tableName                  = "subjectpage"
    override val schemaName: Option[String] = props.MetaSchema.some

    def fromDb(lp: SyntaxProvider[SubjectPage])(rs: WrappedResultSet): Try[SubjectPage] =
      fromDb(lp.resultName)(rs)

    private def fromDb(lp: ResultName[SubjectPage])(rs: WrappedResultSet): Try[SubjectPage] = {
      val id       = rs.long(lp.c("id"))
      val document = rs.string(lp.c("document"))

      SubjectPage.decodeJson(document, id)
    }

  }
}
