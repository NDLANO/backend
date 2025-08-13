/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}

case class UpdatedSubjectPageDTO(
    name: Option[String],
    externalId: Option[String],
    banner: Option[NewOrUpdateBannerImageDTO],
    about: Option[Seq[NewOrUpdatedAboutSubjectDTO]],
    metaDescription: Option[Seq[NewOrUpdatedMetaDescriptionDTO]],
    editorsChoices: Option[List[String]],
    connectedTo: Option[List[String]],
    buildsOn: Option[List[String]],
    leadsTo: Option[List[String]]
)
object UpdatedSubjectPageDTO {
  implicit val encoder: Encoder[UpdatedSubjectPageDTO] = deriveEncoder
  implicit val decoder: Decoder[UpdatedSubjectPageDTO] = deriveDecoder
}
