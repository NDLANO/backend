/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.domain

import no.ndla.common.model.api.MyNDLABundleDTO
import no.ndla.common.model.taxonomy.TaxonomyBundle
import no.ndla.searchapi.model.grep.GrepBundle

/** Bundle of data to pass around when indexing */
case class IndexingBundle(
    grepBundle: Option[GrepBundle],
    taxonomyBundle: Option[TaxonomyBundle],
    myndlaBundle: Option[MyNDLABundleDTO],
)
