/*
 * Part of NDLA search-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep

import enumeratum.*
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import no.ndla.common.implicits.*
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

import scala.util.Try

sealed abstract class GrepStatusDTO(val urlData: String) extends EnumEntry
object GrepStatusDTO                                     extends Enum[GrepStatusDTO] {
  val values: IndexedSeq[GrepStatusDTO] = findValues

  // https://data.udir.no/kl06/v201906/status/
  case object Published  extends GrepStatusDTO("https://data.udir.no/kl06/v201906/status/status_publisert")
  case object InProgress extends GrepStatusDTO("https://data.udir.no/kl06/v201906/status/status_under_arbeid")
  case object ToRevision extends GrepStatusDTO("https://data.udir.no/kl06/v201906/status/status_til_revidering")
  case object Expired    extends GrepStatusDTO("https://data.udir.no/kl06/v201906/status/status_utgaatt")
  case object Invalid    extends GrepStatusDTO("https://data.udir.no/kl06/v201906/status/status_ugyldig")

  implicit val schema: Schema[GrepStatusDTO] = schemaForEnumEntry[GrepStatusDTO]

  def fromUrlData(input: String): Try[GrepStatusDTO] =
    values
      .find(x => input == x.urlData || input == x.entryName)
      .toTry(new IllegalArgumentException(s"Invalid `GrepStatusDTO` input: $input"))

  implicit def decoder: Decoder[GrepStatusDTO] = Decoder.instanceTry {
    _.as[String].toTry.flatMap(str => GrepStatusDTO.fromUrlData(str))
  }

  implicit def encoder(implicit
      config: GrepStatusEncoderConfiguration = GrepStatusEncoderConfiguration.default
  ): Encoder[GrepStatusDTO] = Encoder.instance { x =>
    if (config.encodeToUrl) x.urlData.asJson
    else x.entryName.asJson
  }
}

case class GrepStatusEncoderConfiguration(encodeToUrl: Boolean)

object GrepStatusEncoderConfiguration {
  def default: GrepStatusEncoderConfiguration = GrepStatusEncoderConfiguration(false)
}
