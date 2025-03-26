/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.domain

import cats.implicits.*
import io.circe.generic.semiauto.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.frontpage.{AboutSubject, MovieTheme}
import no.ndla.frontpageapi.Props
import no.ndla.language.Language.getSupportedLanguages
import scalikejdbc.{WrappedResultSet, *}

import scala.util.Try

case class FilmFrontPage(
    name: String,
    about: Seq[AboutSubject],
    movieThemes: Seq[MovieTheme],
    slideShow: Seq[String],
    article: Option[String]
) {

  def supportedLanguages: Seq[String] = getSupportedLanguages(about, movieThemes.flatMap(_.name))
}

object FilmFrontPage {
  implicit val decoder: Decoder[FilmFrontPage] = deriveDecoder
  implicit val encoder: Encoder[FilmFrontPage] = deriveEncoder

  private[domain] def decodeJson(json: String): Try[FilmFrontPage] = {
    parse(json).flatMap(_.as[FilmFrontPage]).toTry
  }
}

trait DBFilmFrontPage {
  this: Props =>

  object DBFilmFrontPageData extends SQLSyntaxSupport[FilmFrontPage] {
    override val tableName                  = "filmfrontpage"
    override val schemaName: Option[String] = props.MetaSchema.some

    def fromDb(lp: SyntaxProvider[FilmFrontPage])(rs: WrappedResultSet): Try[FilmFrontPage] =
      fromDb(lp.resultName)(rs)

    private def fromDb(lp: ResultName[FilmFrontPage])(rs: WrappedResultSet): Try[FilmFrontPage] = {
      val document = rs.string(lp.c("document"))
      FilmFrontPage.decodeJson(document)
    }

  }
}
