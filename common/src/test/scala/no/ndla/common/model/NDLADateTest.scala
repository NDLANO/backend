/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model

import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate.fromTimestampSeconds
import no.ndla.common.model.domain.draft.NestedOptionalDate
import no.ndla.scalatestsuite.UnitTestSuite

import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.annotation.unused
import scala.util.{Failure, Success, Try}

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

    implicit val decoder: Decoder[TestObjectWithDate] = deriveDecoder[TestObjectWithDate]
    implicit val encoder: Encoder[TestObjectWithDate] = deriveEncoder[TestObjectWithDate]

    val dateString       = "2023-08-03T06:01:31.000Z"
    val objectJsonString = s"""{"date":"$dateString","unrelatedField":"test"}"""
    val parsed           = parse(objectJsonString).toTry.get

    val result         = parsed.as[TestObjectWithDate].toTry.get
    val timestamp      = 1691042491L
    val expectedDate   = new NDLADate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()))
    val expectedObject = TestObjectWithDate(expectedDate, "test")

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

  test("That json4s parsing works like before") {
    import org.json4s.native.Serialization.{read, write}
    val formats = org.json4s.DefaultFormats + NDLADate.Json4sSerializer

    val dateString       = "2023-08-03T06:01:31.000Z"
    val objectJsonString = s"""{"date":"$dateString","unrelatedField":"test"}"""

    val timestamp    = 1691042491L
    val expectedDate = new NDLADate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()))

    val result = read[TestObjectWithDate](objectJsonString)(formats, implicitly[Manifest[TestObjectWithDate]])
    result.date should be(expectedDate)
    result.unrelatedField should be("test")

    val jsonStringResult = write(result)(formats)
    jsonStringResult should be(objectJsonString)
  }

  test("That json4s parsing returns some sensible errors") {
    import org.json4s.native.Serialization.read
    val formats = org.json4s.DefaultFormats + NDLADate.Json4sSerializer

    val dateString       = ""
    val objectJsonString = s"""{"date":"$dateString","unrelatedField":"test"}"""

    val result = Try(read[TestObjectWithDate](objectJsonString)(formats, implicitly[Manifest[TestObjectWithDate]]))
    val Failure(ex: org.json4s.MappingException) = result
    ex.msg should be("No usable value for date\nWas unable to parse '' as a date.")
  }

  test("That json4s weird optional parsing returns valid") {
    import org.json4s.native.Serialization.read
    val formats = org.json4s.DefaultFormats + NDLADate.Json4sSerializer

    val dateString       = ""
    val objectJsonString = s"""{"optDate":"$dateString"}"""

    val result =
      read[TestObjectWithOptionalDate](objectJsonString)(formats, implicitly[Manifest[TestObjectWithOptionalDate]])
    result should be(TestObjectWithOptionalDate(None))
  }

  test("That circe parses empty string as `None` for optional NDLADates") {
    import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
    import io.circe.parser.parse
    import io.circe.syntax._

    implicit val decoder: Decoder[TestObjectWithOptionalDate] = deriveDecoder[TestObjectWithOptionalDate]
    implicit val encoder: Encoder[TestObjectWithOptionalDate] = deriveEncoder[TestObjectWithOptionalDate]

    {
      val objectJsonString = s"""{"optDate":""}"""
      val parsed           = parse(objectJsonString).toTry.get

      val result         = parsed.as[TestObjectWithOptionalDate].toTry.get
      val expectedObject = TestObjectWithOptionalDate(None)

      result should be(expectedObject)

      val jsonStringResult   = result.asJson.dropNullValues.noSpaces
      val expectedJsonString = s"""{}"""
      jsonStringResult should be(expectedJsonString)
    }
  }

  test("That nested circe empty string parsing works as expected") {
    import io.circe.generic.semiauto.deriveDecoder
    import io.circe.parser.parse
    @unused
    implicit val subDecoder: Decoder[TestObjectWithOptionalDate] = deriveDecoder[TestObjectWithOptionalDate]
    implicit val decoder: Decoder[NestedOptionalDate]            = deriveDecoder[NestedOptionalDate]

    val objectJsonString2 = s"""{"subfield":{}}"""
    val parsed            = parse(objectJsonString2).toTry.get
    val result            = parsed.as[NestedOptionalDate].toTry.get
    val expectedObject    = NestedOptionalDate(Some(TestObjectWithOptionalDate(None)))
    result should be(expectedObject)
  }

}
