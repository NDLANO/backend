/*
 * Part of NDLA search
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search.model.domain

case class ReindexResult(name: String, failedIndexed: Int, totalIndexed: Int, millisUsed: Long)
