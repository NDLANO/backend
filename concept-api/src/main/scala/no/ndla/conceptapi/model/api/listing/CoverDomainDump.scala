/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api.listing

case class CoverDomainDump(
    totalCount: Long,
    results: List[Cover]
)
