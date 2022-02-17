/*
 * Part of NDLA audio-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field
import no.ndla.audioapi.model.domain

@ApiModel(description = "Information about audio meta dump")
case class AudioMetaDomainDump(
    @(ApiModelProperty @field)(description = "The total number of audios in the database") totalCount: Long,
    @(ApiModelProperty @field)(description = "For which page results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[domain.AudioMetaInformation]
)
