/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about user data")
case class UpdatedUserData(
    @(ApiModelProperty @field)(description = "User's saved searches") savedSearches: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "User's last edited articles") latestEditedArticles: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "User's favorite subjects") favoriteSubjects: Option[Seq[String]])
