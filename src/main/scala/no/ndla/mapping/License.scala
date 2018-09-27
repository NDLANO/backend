/*
 * Part of NDLA mapping.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.mapping

object License {
  private val licenseToLicenseDefinitionsSeq = Seq(
    LicenseDefinition("CC0-1.0", "Creative Commons Zero", Some("https://creativecommons.org/publicdomain/zero/1.0/legalcode")),
    LicenseDefinition("PD", "Public Domain Mark", Some("https://creativecommons.org/about/pdm")),
    LicenseDefinition("COPYRIGHTED", "Copyrighted", None),
    LicenseDefinition("CC-BY-4.0", "Creative Commons Attribution 4.0 International", Some("https://creativecommons.org/licenses/by/4.0/")),
    LicenseDefinition("CC-BY-SA-4.0", "Creative Commons Attribution-ShareAlike 4.0 International", Some("https://creativecommons.org/licenses/by-sa/4.0/")),
    LicenseDefinition("CC-BY-NC-4.0", "Creative Commons Attribution-NonCommercial 4.0 International", Some("https://creativecommons.org/licenses/by-nc/4.0/")),
    LicenseDefinition("CC-BY-ND-4.0", "Creative Commons Attribution-NoDerivs 4.0 International", Some("https://creativecommons.org/licenses/by-nd/4.0/")),
    LicenseDefinition("CC-BY-NC-SA-4.0", "Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International", Some("https://creativecommons.org/licenses/by-nc-sa/4.0/")),
    LicenseDefinition("CC-BY-NC-ND-4.0", "Creative Commons Attribution-NonCommercial-NoDerivs 4.0 International", Some("https://creativecommons.org/licenses/by-nc-nd/4.0/"))
  )

  private val licenseToLicenseDefinitionsMap = licenseToLicenseDefinitionsSeq.map(x => x.license -> x).toMap

  def getLicense(code: String): Option[LicenseDefinition] = licenseToLicenseDefinitionsMap.get(code)

  def getLicenses: Seq[LicenseDefinition] = licenseToLicenseDefinitionsSeq
}

case class LicenseDefinition(license: String, description: String, url: Option[String])
