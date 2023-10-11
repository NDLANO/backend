/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

case class UpdatedSubjectFrontPageData(
    name: Option[String],
    externalId: Option[String],
    banner: Option[NewOrUpdateBannerImage],
    about: Option[Seq[NewOrUpdatedAboutSubject]],
    metaDescription: Option[Seq[NewOrUpdatedMetaDescription]],
    editorsChoices: Option[List[String]],
    connectedTo: Option[List[String]],
    buildsOn: Option[List[String]],
    leadsTo: Option[List[String]]
)
