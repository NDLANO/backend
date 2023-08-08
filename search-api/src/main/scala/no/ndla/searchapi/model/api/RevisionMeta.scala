/*
 * Part of NDLA draft-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import no.ndla.common.model.NDLADate
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

// format: off
@ApiModel(description = "Information about the editorial notes")
case class RevisionMeta(
    @ApiModelProperty(description = "A date on which the article would need to be revised") revisionDate: NDLADate,
    @ApiModelProperty(description = "Notes to keep track of what needs to happen before revision") note: String,
    @ApiModelProperty(description = "Status of a revision, either 'revised' or 'needs-revision'", allowableValues = "revised,needs-revision") status: String
)
