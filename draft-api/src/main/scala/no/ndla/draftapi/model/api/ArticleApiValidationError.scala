/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.errors.ValidationMessage

case class ArticleApiValidationError(
    code: String,
    description: String,
    messages: Seq[ValidationMessage]
)

object ArticleApiValidationError {
  implicit val encoder: Encoder[ArticleApiValidationError] = deriveEncoder
  implicit val decoder: Decoder[ArticleApiValidationError] = deriveDecoder
}
