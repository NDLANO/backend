/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

case class MovieTheme(name: Seq[MovieThemeName], movies: Seq[String])

case class MovieThemeName(name: String, language: String)
