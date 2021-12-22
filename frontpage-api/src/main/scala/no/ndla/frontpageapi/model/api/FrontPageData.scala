/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.Applicative
import cats.effect.Sync
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import scala.language.higherKinds
import io.circe.generic.semiauto._
import io.circe.generic.auto._
import io.circe._

case class FrontPageData(topical: List[String], categories: List[SubjectCollection])

object FrontPageData {
  implicit def encoder[F[_]: Applicative]: EntityEncoder[F, FrontPageData] = {
    val encoder = deriveEncoder[FrontPageData]
    jsonEncoderOf[F, FrontPageData](encoder)
  }

  implicit def decoder[F[_]: Sync]: EntityDecoder[F, FrontPageData] = {
    val decoder = deriveDecoder[no.ndla.frontpageapi.model.api.FrontPageData]
    jsonOf[F, FrontPageData](Sync[F], decoder)
  }
}
