/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class QueryResourceResult(
    contentUri: String,
    id: String,
    name: String,
    path: String,
    resourceTypes: Seq[ResourceType]
)
