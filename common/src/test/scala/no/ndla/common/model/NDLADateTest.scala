/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model

import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate.fromTimestampSeconds
import no.ndla.scalatestsuite.UnitTestSuite

import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.util.Success

class NDLADateTest extends UnitTestSuite {

  test("That parsing from string works as expected") {
    val timestamp = 1691042491L
    val expected  = fromTimestampSeconds(timestamp)

    val validFormats = List(
      "2023-08-03T06:01:31Z",
      "2023-08-03T06:01:31.000Z",
      "2023-08-03T06:01:31.000000000Z",
      "2023-08-03T06:01:31",
      "2023-08-03T06:01:31.000",
      "2023-08-03T06:01:31.000000000"
    )

    for (x <- validFormats) {
      val result = NDLADate.fromString(x)
      result should be(Success(expected))
    }
  }

  test("That parsing invalid dates fails") {
    val result = NDLADate.fromString("2023-08-03T06:61:31Z")
    result.isFailure should be(true)
  }

  test("That parsing and serializing dates in json works as expected") {
    import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
    import io.circe.parser.parse
    import io.circe.syntax._

    case class ObjectWithDate(date: NDLADate, unrelatedField: String)
    implicit val decoder: Decoder[ObjectWithDate] = deriveDecoder[ObjectWithDate]
    implicit val encoder: Encoder[ObjectWithDate] = deriveEncoder[ObjectWithDate]

    val dateString       = "2023-08-03T06:01:31.000Z"
    val objectJsonString = s"""{"date":"$dateString","unrelatedField":"test"}"""
    val parsed           = parse(objectJsonString).toTry.get

    val result         = parsed.as[ObjectWithDate].toTry.get
    val timestamp      = 1691042491L
    val expectedDate   = new NDLADate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()))
    val expectedObject = ObjectWithDate(expectedDate, "test")

    result should be(expectedObject)

    val jsonStringResult = result.asJson.noSpaces

    jsonStringResult should be(objectJsonString)
  }

  test("That sorting dates works as expected") {
    val a = fromTimestampSeconds(1691042491L)
    val b = fromTimestampSeconds(1691042492L)
    val c = fromTimestampSeconds(1691042494L)

    val dates = List(b, a, c)
    dates.sorted should be(List(a, b, c))

  }

}
