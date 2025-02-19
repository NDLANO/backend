/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

case class UpdatedSubjectFrontPageDataDTO(
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
