/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Information about user data")
case class UserData(
    @ApiModelProperty(description = "The auth0 id of the user") userId: String,
    @ApiModelProperty(description = "User's saved searches") savedSearches: Option[Seq[String]],
    @ApiModelProperty(description = "User's last edited articles") latestEditedArticles: Option[Seq[String]],
    @ApiModelProperty(description = "User's last edited concepts") latestEditedConcepts: Option[Seq[String]],
    @ApiModelProperty(description = "User's favorite subjects") favoriteSubjects: Option[Seq[String]]
)
