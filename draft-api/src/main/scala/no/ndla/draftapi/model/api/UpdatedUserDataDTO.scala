/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description
import no.ndla.common.DeriveHelpers

@description("Information about user data")
case class UpdatedUserDataDTO(
    @description("User's saved searches") savedSearches: Option[Seq[SavedSearchDTO]],
    @description("User's last edited articles") latestEditedArticles: Option[Seq[String]],
    @description("User's last edited concepts") latestEditedConcepts: Option[Seq[String]],
    @description("User's favorite subjects") favoriteSubjects: Option[Seq[String]]
)

object UpdatedUserDataDTO {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}
  import sttp.tapir.generic.auto.*

  implicit val encoder: Encoder[UpdatedUserDataDTO]          = deriveEncoder
  implicit val decoder: Decoder[UpdatedUserDataDTO]          = deriveDecoder
  implicit def schema: sttp.tapir.Schema[UpdatedUserDataDTO] = DeriveHelpers.getSchema
}
