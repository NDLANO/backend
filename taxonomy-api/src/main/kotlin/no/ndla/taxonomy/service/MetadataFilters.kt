/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

data class MetadataFilters(
    val key: String? = null,
    val value: String? = null,
    val visible: Boolean? = null,
) {
  val likeQueryValue = value?.let { "%\"$it\"%" }
  val hasFilters = key != null || value != null || visible != null
}
