/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

import no.ndla.common.model.api.frontpage.VisualElementDTO

case class AboutFilmSubjectDTO(title: String, description: String, visualElement: VisualElementDTO, language: String)
