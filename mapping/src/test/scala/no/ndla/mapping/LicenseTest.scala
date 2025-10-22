/*
 * Part of NDLA mapping
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.mapping

class LicenseTest extends UnitSuite {
  test("getLicense returns a LicenseDefinition if input code is valid") {
    val expectedResult = Some(
      LicenseDefinition(
        License.CC_BY,
        "Creative Commons Attribution 4.0 International",
        Some("https://creativecommons.org/licenses/by/4.0/"),
      )
    )
    License.getLicense("CC-BY-4.0") should equal(expectedResult)
  }

  test("getLicense returns a None if input code is invalid") {
    License.getLicense("invalid") should equal(None)
  }

  test("getLicenses returns a list of all licenses") {
    val byLicense = LicenseDefinition(
      License.CC_BY,
      "Creative Commons Attribution 4.0 International",
      Some("https://creativecommons.org/licenses/by/4.0/"),
    )

    License.getLicenses.size should equal(10)
    License.getLicenses.find(_.license.toString == "CC-BY-4.0").get should equal(byLicense)
  }

  test("getLicense returns a NA license") {
    val expectedResult = Some(LicenseDefinition(License.NA, "Not Applicable", None))
    License.getLicense("N/A") should equal(expectedResult)
  }
}
