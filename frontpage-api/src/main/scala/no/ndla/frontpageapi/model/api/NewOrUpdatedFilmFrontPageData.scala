/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.effect.Sync
import io.circe.Decoder
import io.circe.generic.semiauto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import io.circe.generic.auto._

case class NewOrUpdatedFilmFrontPageData(
    name: String,
    about: Seq[NewOrUpdatedAboutSubject],
    movieThemes: Seq[NewOrUpdatedMovieTheme],
    slideShow: Seq[String]
)

object NewOrUpdatedFilmFrontPageData {

  implicit def decoder: Decoder[NewOrUpdatedFilmFrontPageData] =
    deriveDecoder[NewOrUpdatedFilmFrontPageData]
}
