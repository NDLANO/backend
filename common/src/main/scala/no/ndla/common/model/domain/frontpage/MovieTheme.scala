/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.frontpage

case class MovieTheme(name: Seq[MovieThemeName], movies: Seq[String])
case class MovieThemeName(name: String, language: String)
