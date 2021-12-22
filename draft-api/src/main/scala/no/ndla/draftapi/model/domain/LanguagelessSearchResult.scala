/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

case class LanguagelessSearchResult[T](totalCount: Long,
                                       page: Option[Int],
                                       pageSize: Int,
                                       results: Seq[T],
                                       scrollId: Option[String])
