/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.domain

case class ImportId(importId: Option[String])

object ImportId {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}
  import sttp.tapir.generic.auto.*
  import sttp.tapir.Schema

  implicit val encoder: Encoder[ImportId] = deriveEncoder
  implicit val decoder: Decoder[ImportId] = deriveDecoder
  implicit def schema: Schema[ImportId]   = Schema.derivedSchema
}
