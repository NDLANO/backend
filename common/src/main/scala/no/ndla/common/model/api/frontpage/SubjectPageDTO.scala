/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.frontpage

import io.circe.*
import io.circe.generic.semiauto.*

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
