/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

case class NewSubjectFrontPageData(
    name: String,
    filters: Option[List[String]],
    externalId: Option[String],
    layout: String,
    twitter: Option[String],
    facebook: Option[String],
    banner: NewOrUpdateBannerImage,
    about: Seq[NewOrUpdatedAboutSubject],
    metaDescription: Seq[NewOrUpdatedMetaDescription],
    topical: Option[String],
    mostRead: Option[List[String]],
    editorsChoices: Option[List[String]],
    latestContent: Option[List[String]],
    goTo: Option[List[String]]
)
