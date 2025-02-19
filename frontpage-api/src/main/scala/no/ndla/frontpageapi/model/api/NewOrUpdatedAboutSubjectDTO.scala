/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

case class NewOrUpdatedAboutSubjectDTO(
    title: String,
    description: String,
    language: String,
    visualElement: NewOrUpdatedVisualElementDTO
)
