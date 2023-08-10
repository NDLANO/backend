/*
 * Part of NDLA search
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.search.model.domain

case class EmbedValues(
    id: List[String],
    resource: Option[String],
    language: String
)
