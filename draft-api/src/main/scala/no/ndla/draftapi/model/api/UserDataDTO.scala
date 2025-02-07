/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Information about user data")
case class UserDataDTO(
    @description("The auth0 id of the user") userId: String,
    @description("User's saved searches") savedSearches: Option[Seq[SavedSearchDTO]],
    @description("User's last edited articles") latestEditedArticles: Option[Seq[String]],
    @description("User's last edited concepts") latestEditedConcepts: Option[Seq[String]],
    @description("User's favorite subjects") favoriteSubjects: Option[Seq[String]]
)
