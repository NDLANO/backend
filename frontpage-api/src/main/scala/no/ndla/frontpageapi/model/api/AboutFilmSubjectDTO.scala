/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.api

case class AboutFilmSubjectDTO(
    title: String,
    description: String,
    visualElement: VisualElementDTO,
    language: String
)
