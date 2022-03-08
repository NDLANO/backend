/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import java.time.LocalDateTime

@ApiModel(description = "Information about the editorial notes")
case class RevisionMeta(
    @ApiModelProperty(description = "A date on which the article would need to be revised") revisionDate: LocalDateTime,
    @ApiModelProperty(description = "Notes to keep track of what needs to happen before revision") notes: Seq[String],
    @ApiModelProperty(description = "Status of a revision, either 'revised' or 'needs-revision'") status: String
)
