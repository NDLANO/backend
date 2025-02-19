/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

case class NewOrUpdatedFilmFrontPageDataDTO(
    name: String,
    about: Seq[NewOrUpdatedAboutSubjectDTO],
    movieThemes: Seq[NewOrUpdatedMovieThemeDTO],
    slideShow: Seq[String],
    article: Option[String]
)
