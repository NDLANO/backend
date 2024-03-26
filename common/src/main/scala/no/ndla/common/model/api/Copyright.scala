/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Description of copyright information")
case class Copyright(
    @description("Describes the license of the article") license: License,
    @description("Reference to where the article is procured") origin: Option[String],
    @description("List of creators") creators: Seq[Author] = Seq.empty,
    @description("List of processors") processors: Seq[Author] = Seq.empty,
    @description("List of rightsholders") rightsholders: Seq[Author] = Seq.empty,
    @description("Date from which the copyright is valid") validFrom: Option[NDLADate],
    @description("Date to which the copyright is valid") validTo: Option[NDLADate],
    @description("Whether or not the content has been processed") processed: Boolean
)

object Copyright {
  implicit val encoder: Encoder[Copyright] = deriveEncoder
  implicit val decoder: Decoder[Copyright] = deriveDecoder
}
