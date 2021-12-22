/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

case class EmbedValues(
    id: List[String],
    resource: Option[String],
    language: String
)
