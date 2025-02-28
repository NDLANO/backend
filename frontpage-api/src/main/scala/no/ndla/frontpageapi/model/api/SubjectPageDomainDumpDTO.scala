/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.frontpageapi.model.api

import no.ndla.common.model.domain.frontpage.SubjectPage
import sttp.tapir.Schema.annotations.description

@description("All the subjectpages")
case class SubjectPageDomainDumpDTO(
    @description("The total number of articles in the database") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The search results") results: Seq[SubjectPage]
)
