/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

import io.circe.generic.semiauto.*
import io.circe.generic.auto.*
import io.circe.*

case class SubjectPageDTO(
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

object SubjectPageDTO {
  implicit def encoder: Encoder[SubjectPageDTO] = deriveEncoder[SubjectPageDTO]
  implicit def decoder: Decoder[SubjectPageDTO] = deriveDecoder[SubjectPageDTO]
}
