/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class ReindexResult(name: String, failedIndexed: Int, totalIndexed: Int, millisUsed: Long)
