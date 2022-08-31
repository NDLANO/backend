/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations.ApiModelProperty

import java.util.UUID
import scala.annotation.meta.field

case class FolderSortRequest(
    @(ApiModelProperty @field)(
      description = "List of the children ids in sorted order, MUST be all ids"
    ) sortedIds: Seq[UUID]
)
