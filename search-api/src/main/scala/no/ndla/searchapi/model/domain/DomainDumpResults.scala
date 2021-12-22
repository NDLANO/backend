/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class DomainDumpResults[T](totalCount: Long, page: Int, pageSize: Int, results: Seq[T])
