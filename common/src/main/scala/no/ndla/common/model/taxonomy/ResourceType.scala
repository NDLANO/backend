/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.taxonomy

case class ResourceType(
    id: String,
    name: String,
    subtypes: Option[List[ResourceType]],
    translations: List[TaxonomyTranslation],
)
