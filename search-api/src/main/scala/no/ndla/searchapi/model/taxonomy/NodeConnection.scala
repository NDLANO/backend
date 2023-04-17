/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class NodeConnection(
    parentId: String,
    childId: String,
    id: String,
    primary: Boolean,
    rank: Int,
    relevanceId: Option[String],
    metadata: Option[Metadata]
)
