/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import no.ndla.network.UnitSuite

class NonEmptyStringTest extends UnitSuite {

  test("That calling the constructor directly does not work") {
    "new NonEmptyString(\"\")" shouldNot compile
  }

  test("That constructing from string works if the string is non-empty") {
    val fromStr = NonEmptyString.fromString("hei")
    fromStr.isDefined should be(true)
    fromStr.get.underlying should be("hei")
  }

  test("That constructing from empty string results in None") {
    val fromStr = NonEmptyString.fromString("")
    fromStr should be(None)
  }

  test("That constructing from optional string works if the string is non-empty") {
    val fromStr = NonEmptyString.fromOptString(Some("hei"))
    fromStr.isDefined should be(true)
    fromStr.get.underlying should be("hei")
  }

  test("That constructing from empty optional string results in None") {
    NonEmptyString.fromOptString(None) should be(None)
    NonEmptyString.fromOptString(Some("")) should be(None)
  }

  test("That decoding a json-string returns `None` if empty string is passed") {
    import io.circe.generic.auto._
    case class SomeObject(cantbeempty: Option[NonEmptyString])

    val jsonString           = """{"cantbeempty": ""}"""
    val Right(Right(result)) = io.circe.parser.parse(jsonString).map(_.as[SomeObject])
    result.cantbeempty should be(None)
  }

  test("That decoding a json-string returns Some(str) if valid string is passed") {
    import io.circe.generic.auto._
    case class SomeObject(cantbeempty: Option[NonEmptyString])

    val jsonString           = """{"cantbeempty": "spirrevipp"}"""
    val Right(Right(result)) = io.circe.parser.parse(jsonString).map(_.as[SomeObject])
    result.cantbeempty.get.underlying should be("spirrevipp")
  }

  test("That decoding missing field returns None for optionals") {
    import io.circe.generic.auto._
    case class SomeObject(cantbeempty: Option[NonEmptyString])

    val jsonString           = """{}"""
    val Right(Right(result)) = io.circe.parser.parse(jsonString).map(_.as[SomeObject])
    result.cantbeempty should be(None)
  }

  test("That encoding json simply makes a normal string :^)") {
    import io.circe.generic.auto._
    import io.circe.syntax._
    case class SomeObject(cantbeempty: Option[NonEmptyString], yolo: NonEmptyString)

    val expectedString = """{"cantbeempty":"spirrevipp","yolo":"heisann"}"""

    val toConvert = SomeObject(
      cantbeempty = NonEmptyString.fromString("spirrevipp"),
      yolo = NonEmptyString.fromString("heisann").get
    )

    val jsonString = toConvert.asJson.noSpaces
    jsonString should be(expectedString)
  }

  test("That decoding a json-string returns str if valid string is passed and fails if invalid") {
    import io.circe.generic.auto._
    case class SomeObject(cantbeempty: NonEmptyString)

    val jsonString1           = """{"cantbeempty": "spirrevipp"}"""
    val Right(Right(result1)) = io.circe.parser.parse(jsonString1).map(_.as[SomeObject])
    result1.cantbeempty.underlying should be("spirrevipp")

    val jsonString           = """{"cantbeempty": ""}"""
    val Right(Left(failure)) = io.circe.parser.parse(jsonString).map(_.as[SomeObject])
    failure.message should be(NonEmptyString.parseErrorMessage)
  }

  test("That comparisons works as expected") {
    val a = NonEmptyString.fromString("hei").get
    val b = NonEmptyString.fromString("hei").get
    val c = NonEmptyString.fromString("hallo").get

    a == b should be(true)
    a == c should be(false)
    a == "hei" should be(true)
    a == "hallo" should be(false)
  }

  test("That only whitespace strings are not parsed as NonEmptyString") {
    NonEmptyString.fromString(" ") should be(None)
    NonEmptyString.fromString("  ") should be(None)
    NonEmptyString.fromString("   ") should be(None)
  }

}
