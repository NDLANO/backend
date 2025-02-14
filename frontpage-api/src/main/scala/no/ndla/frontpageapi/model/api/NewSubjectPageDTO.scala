/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

case class NewSubjectPageDTO(
    name: String,
    externalId: Option[String],
    banner: NewOrUpdateBannerImageDTO,
    about: Seq[NewOrUpdatedAboutSubjectDTO],
    metaDescription: Seq[NewOrUpdatedMetaDescriptionDTO],
    editorsChoices: Option[List[String]],
    connectedTo: Option[List[String]],
    buildsOn: Option[List[String]],
    leadsTo: Option[List[String]]
)
