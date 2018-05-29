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
    val expectedResult = Some(LicenseDefinition("by", "Creative Commons Attribution 2.0 Generic", Some("https://creativecommons.org/licenses/by/2.0/")))
    License.getLicense("by") should equal(expectedResult)
  }

  test("getLicense returns a None if input code is invaid") {
    License.getLicense("invalid") should equal(None)
  }

  test("getLicenses returns a list of all licenses") {
    val byLicense = LicenseDefinition("by", "Creative Commons Attribution 2.0 Generic", Some("https://creativecommons.org/licenses/by/2.0/"))

    License.getLicenses.size should equal(21)
    License.getLicenses.find(_.license == "by").get should equal (byLicense)
  }
}
