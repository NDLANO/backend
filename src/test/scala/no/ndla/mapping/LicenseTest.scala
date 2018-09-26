/*
 * Part of NDLA mapping.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.mapping

class LicenseTest extends UnitSuite {
  test("getLicense returns a LicenseDefinition if input code is vaid") {
    val expectedResult = Some(LicenseDefinition("CC-BY-4.0", "Creative Commons Attribution 4.0 International", Some("https://creativecommons.org/licenses/by/4.0/")))
    License.getLicense("CC-BY-4.0") should equal(expectedResult)
  }

  test("getLicense returns a None if input code is invaid") {
    License.getLicense("invalid") should equal(None)
  }

  test("getLicenses returns a list of all licenses") {
    val byLicense = LicenseDefinition("CC-BY-4.0", "Creative Commons Attribution 4.0 International", Some("https://creativecommons.org/licenses/by/4.0/"))

    License.getLicenses.size should equal(9)
    License.getLicenses.find(_.license == "CC-BY-4.0").get should equal (byLicense)
  }
}
