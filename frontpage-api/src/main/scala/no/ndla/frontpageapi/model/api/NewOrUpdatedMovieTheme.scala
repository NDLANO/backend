/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

case class NewOrUpdatedMovieTheme(name: Seq[NewOrUpdatedMovieName], movies: Seq[String])

case class NewOrUpdatedMovieName(name: String, language: String)
