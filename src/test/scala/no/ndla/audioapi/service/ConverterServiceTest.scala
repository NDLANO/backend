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
    val license = License(licenseAbbr, "Creative Commons Attribution-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-sa/2.0/"))
    when(mappingApiClient.getLicenseDefinition(licenseAbbr)).thenReturn(Some(license))

    service.toApiLicence(licenseAbbr) should equal (license)
    verify(mappingApiClient, times(1)).getLicenseDefinition(licenseAbbr)
  }

  test("That toApiLicense returns unknown if the license is invalid") {
    val licenseAbbr = "garbage"
    when(mappingApiClient.getLicenseDefinition(licenseAbbr)).thenReturn(None)

    service.toApiLicence(licenseAbbr) should equal (License("unknown", "", None))
    verify(mappingApiClient, times(1)).getLicenseDefinition(licenseAbbr)
  }
}
