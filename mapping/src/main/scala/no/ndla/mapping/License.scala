/*
 * Part of NDLA mapping
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.mapping
import no.ndla.language.model.LanguageField

object License extends Enumeration {
  val CC0: Value          = Value("CC0-1.0")
  val PublicDomain: Value = Value("PD")
  val Copyrighted: Value  = Value("COPYRIGHTED")
  val CC_BY: Value        = Value("CC-BY-4.0")
  val CC_BY_SA: Value     = Value("CC-BY-SA-4.0")
  val CC_BY_NC: Value     = Value("CC-BY-NC-4.0")
  val CC_BY_ND: Value     = Value("CC-BY-ND-4.0")
  val CC_BY_NC_SA: Value  = Value("CC-BY-NC-SA-4.0")
  val CC_BY_NC_ND: Value  = Value("CC-BY-NC-ND-4.0")
  val NA: Value           = Value("N/A")

  private val licenseToLicenseDefinitionsSeq = Seq(
    LicenseDefinition(
      CC0,
      "Creative Commons Zero",
      Seq(
        LicenseUrl("https://creativecommons.org/publicdomain/zero/1.0", "en"),
        LicenseUrl("https://creativecommons.org/publicdomain/zero/1.0/deed.no", "nb"),
        LicenseUrl("https://creativecommons.org/publicdomain/zero/1.0/deed.no", "nn"),
      ),
    ),
    LicenseDefinition(
      PublicDomain,
      "Public Domain Mark",
      Seq(
        LicenseUrl("https://creativecommons.org/publicdomain/mark/1.0", "en"),
        LicenseUrl("https://creativecommons.org/publicdomain/mark/1.0/deed.no", "nb"),
        LicenseUrl("https://creativecommons.org/publicdomain/mark/1.0/deed.no", "nn"),
      ),
    ),
    LicenseDefinition(
      Copyrighted,
      "Copyrighted",
      Seq(
        LicenseUrl("https://ndla.no/en/article/opphavsrett", "en"),
        LicenseUrl("https://ndla.no/nb/article/opphavsrett", "nb"),
        LicenseUrl("https://ndla.no/nn/article/opphavsrett", "nn"),
      ),
    ),
    LicenseDefinition(
      CC_BY,
      "Creative Commons Attribution 4.0 International",
      Seq(
        LicenseUrl("https://creativecommons.org/licenses/by/4.0", "en"),
        LicenseUrl("https://creativecommons.org/licenses/by/4.0/deed.no", "nb"),
        LicenseUrl("https://creativecommons.org/licenses/by/4.0/deed.no", "nn"),
      ),
    ),
    LicenseDefinition(
      CC_BY_SA,
      "Creative Commons Attribution-ShareAlike 4.0 International",
      Seq(
        LicenseUrl("https://creativecommons.org/licenses/by-sa/4.0", "en"),
        LicenseUrl("https://creativecommons.org/licenses/by-sa/4.0/deed.no", "nb"),
        LicenseUrl("https://creativecommons.org/licenses/by-sa/4.0/deed.no", "nn"),
      ),
    ),
    LicenseDefinition(
      CC_BY_NC,
      "Creative Commons Attribution-NonCommercial 4.0 International",
      Seq(
        LicenseUrl("https://creativecommons.org/licenses/by-nc/4.0", "en"),
        LicenseUrl("https://creativecommons.org/licenses/by-nc/4.0/deed.no", "nb"),
        LicenseUrl("https://creativecommons.org/licenses/by-nc/4.0/deed.no", "nn"),
      ),
    ),
    LicenseDefinition(
      CC_BY_ND,
      "Creative Commons Attribution-NoDerivs 4.0 International",
      Seq(
        LicenseUrl("https://creativecommons.org/licenses/by-nd/4.0", "en"),
        LicenseUrl("https://creativecommons.org/licenses/by-nd/4.0/deed.no", "nb"),
        LicenseUrl("https://creativecommons.org/licenses/by-nd/4.0/deed.no", "nn"),
      ),
    ),
    LicenseDefinition(
      CC_BY_NC_SA,
      "Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International",
      Seq(
        LicenseUrl("https://creativecommons.org/licenses/by-nc-sa/4.0", "en"),
        LicenseUrl("https://creativecommons.org/licenses/by-nc-sa/4.0/deed.no", "nb"),
        LicenseUrl("https://creativecommons.org/licenses/by-nc-sa/4.0/deed.no", "nn"),
      ),
    ),
    LicenseDefinition(
      CC_BY_NC_ND,
      "Creative Commons Attribution-NonCommercial-NoDerivs 4.0 International",
      Seq(
        LicenseUrl("https://creativecommons.org/licenses/by-nc-nd/4.0", "en"),
        LicenseUrl("https://creativecommons.org/licenses/by-nc-nd/4.0/deed.no", "nb"),
        LicenseUrl("https://creativecommons.org/licenses/by-nc-nd/4.0/deed.no", "nn"),
      ),
    ),
    LicenseDefinition(NA, "Not Applicable", Seq.empty),
  )

  private val licenseToLicenseDefinitionsMap = licenseToLicenseDefinitionsSeq.map(x => x.license.toString -> x).toMap

  def getLicense(code: String): Option[LicenseDefinition] = licenseToLicenseDefinitionsMap.get(code)

  def getLicenses: Seq[LicenseDefinition] = licenseToLicenseDefinitionsSeq
}

case class LicenseDefinition(license: License.Value, description: String, url: Seq[LicenseUrl])

case class LicenseUrl(url: String, language: String) extends LanguageField[String] {
  override def value: String    = url
  override def isEmpty: Boolean = url.isEmpty
}
