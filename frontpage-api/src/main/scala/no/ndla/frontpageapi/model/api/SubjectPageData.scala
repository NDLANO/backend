/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import cats.Applicative
import cats.effect.Sync
import org.http4s.circe.{jsonOf, jsonEncoderWithPrinterOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import io.circe.generic.semiauto._
import io.circe.generic.auto._
import io.circe._

case class SubjectPageData(
    id: Long,
    name: String,
    filters: Option[List[String]],
    layout: String,
    twitter: Option[String],
    facebook: Option[String],
    banner: BannerImage,
    about: Option[AboutSubject],
    metaDescription: Option[String],
    topical: Option[String],
    mostRead: List[String],
    editorsChoices: List[String],
    latestContent: Option[List[String]],
    goTo: List[String],
    supportedLanguages: Seq[String]
)

object SubjectPageData {
  val indentDropNull = Printer.spaces2.copy(dropNullValues = true) // TODO: Do we need to include this in encoder?

  implicit def encoder: Encoder[SubjectPageData] = deriveEncoder[SubjectPageData]

  implicit def decoder: Decoder[SubjectPageData] = deriveDecoder[SubjectPageData]
}
