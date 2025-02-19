/*
 * Part of NDLA audio-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import no.ndla.audioapi.model.domain
import sttp.tapir.Schema.annotations.description

@description("Information about audio meta dump")
case class AudioMetaDomainDumpDTO(
    @description("The total number of audios in the database") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The search results") results: Seq[domain.AudioMetaInformation]
)
