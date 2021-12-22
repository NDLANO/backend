/*
 * Part of NDLA search_api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.search

case class EmbedValues(
    id: List[String],
    resource: Option[String],
    language: String
)
