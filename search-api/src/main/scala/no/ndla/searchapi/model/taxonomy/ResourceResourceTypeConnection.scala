/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class ResourceResourceTypeConnection(
    resourceId: String,
    resourceTypeId: String,
    id: String
) extends ResourceTypeConnection
