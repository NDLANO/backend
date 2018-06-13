/*
 * Part of NDLA frontpage_api.
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

case class SubjectPageData(id: Long,
                           name: String,
                           displayInTwoColumns: Boolean,
                           twitter: String,
                           facebook: String,
                           banner: BannerImage,
                           about: AboutSubject,
                           topical: String,
                           mostRead: List[String],
                           editorsChoices: List[String],
                           latestContent: List[String],
                           goTo: List[String])

object SubjectPageData {
  implicit def encoder[F[_]: Applicative]: EntityEncoder[F, SubjectPageData] =
    jsonEncoderOf[F, SubjectPageData](EntityEncoder[F, String], Applicative[F], deriveEncoder[SubjectPageData])
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, SubjectPageData] =
    jsonOf[F, SubjectPageData](Sync[F], deriveDecoder[SubjectPageData])
}
