/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

case class NewOrUpdatedMovieThemeDTO(name: Seq[NewOrUpdatedMovieNameDTO], movies: Seq[String])

case class NewOrUpdatedMovieNameDTO(name: String, language: String)
