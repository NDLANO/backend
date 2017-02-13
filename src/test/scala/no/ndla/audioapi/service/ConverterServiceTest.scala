/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import no.ndla.audioapi.model.api.License
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class ConverterServiceTest extends UnitSuite with TestEnvironment {
  val service = new ConverterService

  test("That toApiLicense invokes mapping api to retrieve license information") {
    val licenseAbbr = "by-sa"
    val license = License(licenseAbbr, Some("Creative Commons Attribution-ShareAlike 2.0 Generic"), Some("https://creativecommons.org/licenses/by-sa/2.0/"))

    service.toApiLicence(licenseAbbr) should equal (license)
  }

  test("That toApiLicense returns unknown if the license is invalid") {
    val licenseAbbr = "garbage"

    service.toApiLicence(licenseAbbr) should equal (License("unknown", None, None))
  }
}
