/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

case class NewSubjectFrontPageData(
    name: String,
    externalId: Option[String],
    banner: NewOrUpdateBannerImage,
    about: Seq[NewOrUpdatedAboutSubject],
    metaDescription: Seq[NewOrUpdatedMetaDescription],
    editorsChoices: Option[List[String]],
    connectedTo: Option[List[String]],
    buildsOn: Option[List[String]],
    leadsTo: Option[List[String]]
)
