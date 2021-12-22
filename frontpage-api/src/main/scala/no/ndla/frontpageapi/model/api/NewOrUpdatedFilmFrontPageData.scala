/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.effect.Sync
import io.circe.generic.semiauto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import io.circe.generic.auto._

import scala.language.higherKinds

case class NewOrUpdatedFilmFrontPageData(name: String,
                                         about: Seq[NewOrUpdatedAboutSubject],
                                         movieThemes: Seq[NewOrUpdatedMovieTheme],
                                         slideShow: Seq[String])

object NewOrUpdatedFilmFrontPageData {

  implicit def decoder[F[_]: Sync]: EntityDecoder[F, NewOrUpdatedFilmFrontPageData] = {
    val decoder = deriveDecoder[NewOrUpdatedFilmFrontPageData]
    jsonOf[F, NewOrUpdatedFilmFrontPageData](Sync[F], decoder)
  }
}
