/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

case class FilmFrontPageDTO(
    name: String,
    about: Seq[AboutFilmSubjectDTO],
    movieThemes: Seq[MovieThemeDTO],
    slideShow: Seq[String],
    article: Option[String]
)
