/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

case class MovieThemeDTO(name: Seq[MovieThemeNameDTO], movies: Seq[String])

case class MovieThemeNameDTO(name: String, language: String)
