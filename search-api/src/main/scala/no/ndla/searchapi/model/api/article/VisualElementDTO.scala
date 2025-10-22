/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.article

import no.ndla.language.model.WithLanguage
import sttp.tapir.Schema.annotations.description

@description("Description of a visual element")
case class VisualElementDTO(
    @description("Html containing the visual element. May contain any legal html element, including the embed-tag")
    visualElement: String,
    @description("The ISO 639-1 language code describing which article translation this visual element belongs to")
    language: String,
) extends WithLanguage
