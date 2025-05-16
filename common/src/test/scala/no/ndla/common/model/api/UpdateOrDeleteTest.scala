/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import no.ndla.testbase.UnitTestSuiteBase

class UpdateOrDeleteTest extends UnitTestSuiteBase {

  test("That encoding UpdateOrDelete works as expected") {
    import io.circe.syntax.*
    import io.circe.Encoder
    import io.circe.generic.semiauto.deriveEncoder
    case class ApiObject(
        normalField: String,
        deletableField: UpdateOrDelete[String]
    )

    object ApiObject {
      implicit val encoder: Encoder[ApiObject] = UpdateOrDelete.filterMarkers(deriveEncoder)
    }

    val x = ApiObject(
      "hei",
      Missing
    )

    val y = ApiObject(
      "hei",
      Delete
    )

    val z = ApiObject(
      "hei",
      UpdateWith("næmmen")
    )

    val res1 = x.asJson.noSpaces
    val res2 = y.asJson.noSpaces
    val res3 = z.asJson.noSpaces

    res1 should be("""{"normalField":"hei"}""")
    res2 should be("""{"normalField":"hei","deletableField":null}""")
    res3 should be("""{"normalField":"hei","deletableField":"næmmen"}""")
  }

  test("That decoding UpdateOrDelete works as expected") {
    import io.circe.Decoder
    import io.circe.generic.semiauto.deriveDecoder
    import io.circe.parser.parse

    case class ApiObject(
        normalField: String,
        deletableField: UpdateOrDelete[String]
    )

    object ApiObject {
      implicit val decoder: Decoder[ApiObject] = deriveDecoder
    }

    val res1 = parse("""{"normalField":"hei"}""").flatMap(_.as[ApiObject]).toTry.get
    val res2 = parse("""{"normalField":"hei","deletableField":null}""").flatMap(_.as[ApiObject]).toTry.get
    val res3 = parse("""{"normalField":"hei","deletableField":"næmmen"}""").flatMap(_.as[ApiObject]).toTry.get

    res1 should be(
      ApiObject(
        "hei",
        Missing
      )
    )
    res2 should be(
      ApiObject(
        "hei",
        Delete
      )
    )
    res3 should be(
      ApiObject(
        "hei",
        UpdateWith("næmmen")
      )
    )
  }

}
