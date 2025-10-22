/*
 * Part of NDLA search-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep;

import io.circe.generic.semiauto.deriveDecoder
import io.circe.Decoder
import io.circe.syntax.EncoderOps
import no.ndla.common.CirceUtil
import no.ndla.searchapi.{TestEnvironment, UnitSuite}

class GrepStatusDTOTest extends UnitSuite with TestEnvironment {
  test("That serialization works as expected by default") {
    implicit val config: GrepStatusEncoderConfiguration = GrepStatusEncoderConfiguration(encodeToUrl = true)
    val status: GrepStatusDTO                           = GrepStatusDTO.Published
    val json                                            = status.asJson.noSpaces
    json should be("\"https://data.udir.no/kl06/v201906/status/status_publisert\"")
  }

  test("That serialization works as expected with custom serializer") {
    val status: GrepStatusDTO = GrepStatusDTO.Published
    val json                  = status.asJson.noSpaces
    json should be("\"Published\"")
  }

  test("That deserialization works as expected with both values") {
    val uriStr  = "\"https://data.udir.no/kl06/v201906/status/status_publisert\""
    val enumStr = "\"Published\""

    CirceUtil.tryParseAs[GrepStatusDTO](uriStr).get should be(GrepStatusDTO.Published)
    CirceUtil.tryParseAs[GrepStatusDTO](enumStr).get should be(GrepStatusDTO.Published)
  }

  test("GrepStatusDTO decoding as inner class works as expected") {
    case class WithStatus(status: GrepStatusDTO)
    implicit val decoder: Decoder[WithStatus] = deriveDecoder[WithStatus]
    val json                                  = """{"status":"https://data.udir.no/kl06/v201906/status/status_publisert"}"""
    val element                               = CirceUtil.tryParseAs[WithStatus](json).get
    element.status should be(GrepStatusDTO.Published)

    val json2    = """{"status":"Published"}"""
    val element2 = CirceUtil.tryParseAs[WithStatus](json2).get
    element2.status should be(GrepStatusDTO.Published)
  }
}
