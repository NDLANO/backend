/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

case class NewOrUpdatedFilmFrontPageData(
    name: String,
    about: Seq[NewOrUpdatedAboutSubject],
    movieThemes: Seq[NewOrUpdatedMovieTheme],
    slideShow: Seq[String],
    article: Option[String]
)
