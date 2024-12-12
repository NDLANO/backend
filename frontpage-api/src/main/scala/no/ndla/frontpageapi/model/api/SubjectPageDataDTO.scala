/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

import io.circe.generic.semiauto._
import io.circe.generic.auto._
import io.circe._

case class SubjectPageDataDTO(
    id: Long,
    name: String,
    banner: BannerImageDTO,
    about: Option[AboutSubjectDTO],
    metaDescription: Option[String],
    editorsChoices: List[String],
    supportedLanguages: Seq[String],
    connectedTo: List[String],
    buildsOn: List[String],
    leadsTo: List[String]
)

object SubjectPageDataDTO {
  implicit def encoder: Encoder[SubjectPageDataDTO] = deriveEncoder[SubjectPageDataDTO]

  implicit def decoder: Decoder[SubjectPageDataDTO] = deriveDecoder[SubjectPageDataDTO]
}
