/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.domain

import cats.implicits._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.{Decoder, Encoder}
import no.ndla.frontpageapi.Props
import scalikejdbc.{WrappedResultSet, _}

import scala.util.Try

case class FilmFrontPageData(
    name: String,
    about: Seq[AboutSubject],
    movieThemes: Seq[MovieTheme],
    slideShow: Seq[String],
    article: Option[String]
)

object FilmFrontPageData {
  implicit val decoder: Decoder[FilmFrontPageData] = deriveDecoder
  implicit val encoder: Encoder[FilmFrontPageData] = deriveEncoder

  private[domain] def decodeJson(json: String): Try[FilmFrontPageData] = {
    parse(json).flatMap(_.as[FilmFrontPageData]).toTry
  }
}

trait DBFilmFrontPageData {
  this: Props =>

  object DBFilmFrontPageData extends SQLSyntaxSupport[FilmFrontPageData] {
    override val tableName                  = "filmfrontpage"
    override val schemaName: Option[String] = props.MetaSchema.some

    def fromDb(lp: SyntaxProvider[FilmFrontPageData])(rs: WrappedResultSet): Try[FilmFrontPageData] =
      fromDb(lp.resultName)(rs)

    private def fromDb(lp: ResultName[FilmFrontPageData])(rs: WrappedResultSet): Try[FilmFrontPageData] = {
      val document = rs.string(lp.c("document"))
      FilmFrontPageData.decodeJson(document)
    }

  }
}
