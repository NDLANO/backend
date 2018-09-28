/*
 * Part of NDLA mapping.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.mapping

object License extends Enumeration {
  val CC0 = Value("CC0-1.0")
  val PublicDomain = Value("PD")
  val Copyrighted = Value("COPYRIGHTED")
  val CC_BY = Value("CC-BY-4.0")
  val CC_BY_SA = Value("CC-BY-SA-4.0")
  val CC_BY_NC = Value("CC-BY-NC-4.0")
  val CC_BY_ND = Value("CC-BY-ND-4.0")
  val CC_BY_NC_SA = Value("CC-BY-NC-SA-4.0")
  val CC_BY_NC_ND = Value("CC-BY-NC-ND-4.0")

  private val licenseToLicenseDefinitionsSeq = Seq(
    LicenseDefinition(CC0, "Creative Commons Zero", Some("https://creativecommons.org/publicdomain/zero/1.0/legalcode")),
    LicenseDefinition(PublicDomain, "Public Domain Mark", Some("https://creativecommons.org/about/pdm")),
    LicenseDefinition(Copyrighted, "Copyrighted", None),
    LicenseDefinition(CC_BY, "Creative Commons Attribution 4.0 International", Some("https://creativecommons.org/licenses/by/4.0/")),
    LicenseDefinition(CC_BY_SA, "Creative Commons Attribution-ShareAlike 4.0 International", Some("https://creativecommons.org/licenses/by-sa/4.0/")),
    LicenseDefinition(CC_BY_NC, "Creative Commons Attribution-NonCommercial 4.0 International", Some("https://creativecommons.org/licenses/by-nc/4.0/")),
    LicenseDefinition(CC_BY_ND, "Creative Commons Attribution-NoDerivs 4.0 International", Some("https://creativecommons.org/licenses/by-nd/4.0/")),
    LicenseDefinition(CC_BY_NC_SA, "Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International", Some("https://creativecommons.org/licenses/by-nc-sa/4.0/")),
    LicenseDefinition(CC_BY_NC_ND, "Creative Commons Attribution-NonCommercial-NoDerivs 4.0 International", Some("https://creativecommons.org/licenses/by-nc-nd/4.0/"))
  )

  private val licenseToLicenseDefinitionsMap = licenseToLicenseDefinitionsSeq.map(x => x.license.toString -> x).toMap

  def getLicense(code: String): Option[LicenseDefinition] = licenseToLicenseDefinitionsMap.get(code)

  def getLicenses: Seq[LicenseDefinition] = licenseToLicenseDefinitionsSeq
}

case class LicenseDefinition(license: License.Value, description: String, url: Option[String])
