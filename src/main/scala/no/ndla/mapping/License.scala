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
    LicenseDefinition("by", "Creative Commons Attribution 2.0 Generic", Some("https://creativecommons.org/licenses/by/2.0/")),
    LicenseDefinition("by-sa", "Creative Commons Attribution-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-sa/2.0/")),
    LicenseDefinition("by-nc", "Creative Commons Attribution-NonCommercial 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc/2.0/")),
    LicenseDefinition("by-nd", "Creative Commons Attribution-NoDerivs 2.0 Generic", Some("https://creativecommons.org/licenses/by-nd/2.0/")),
    LicenseDefinition("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/")),
    LicenseDefinition("by-nc-nd", "Creative Commons Attribution-NonCommercial-NoDerivs 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-nd/2.0/")),
    LicenseDefinition("cc0", "Public Domain Dedication", Some("https://creativecommons.org/about/cc0")),
    LicenseDefinition("pd", "Public Domain Mark", Some("https://creativecommons.org/about/pdm")),
    LicenseDefinition("copyrighted", "Copyrighted", None),
    LicenseDefinition("by-4.0", "Creative Commons Attribution 4.0 Generic", Some("https://creativecommons.org/licenses/by/4.0/")),
    LicenseDefinition("by-sa-4.0", "Creative Commons Attribution-ShareAlike 4.0 Generic", Some("https://creativecommons.org/licenses/by-sa/4.0/")),
    LicenseDefinition("by-nc-4.0", "Creative Commons Attribution-NonCommercial 4.0 Generic", Some("https://creativecommons.org/licenses/by-nc/4.0/"))

  )

  private val licenseToLicenseDefinitionsMap = licenseToLicenseDefinitionsSeq.map(x => x.license -> x).toMap

  def getLicense(code: String): Option[LicenseDefinition] = licenseToLicenseDefinitionsMap.get(code)

  def getLicenses: Seq[LicenseDefinition] = licenseToLicenseDefinitionsSeq
}

case class LicenseDefinition(license: String, description: String, url: Option[String])
