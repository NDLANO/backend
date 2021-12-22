/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

case class AgreementSearchSettings(
    query: Option[String],
    withIdIn: List[Long],
    license: Option[String],
    page: Int,
    pageSize: Int,
    sort: Sort.Value,
    shouldScroll: Boolean
)
