/*
 * Part of NDLA mapping.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.mapping

object ISO639 {
  private val NORWEGIAN_BOKMAL = "nb"
  private val NORWEGIAN_NYNORSK = "nn"
  private val ENGLISH = "en"
  private val FRENCH = "fr"
  private val GERMAN = "de"
  private val SAMI = "se"
  private val SPANISH = "es"
  private val CHINESE = "zh"
  private val UNKNOWN = "unknown"

  private val iso639Map = Map(
    "nob" -> NORWEGIAN_BOKMAL,
    "eng" -> ENGLISH,
    "fra" -> FRENCH,
    "nno" -> NORWEGIAN_NYNORSK,
    "sme" -> SAMI,
    "sma" -> SAMI,
    "smj" -> SAMI,
    "deu" -> GERMAN,
    "spa" -> SPANISH,
    "zho" -> CHINESE
  )

  private val supportedLanguages = iso639Map.values

  def get6391CodeFor6392CodeMappings: Map[String, String] = iso639Map

  def get6391CodeFor6392Code(code6392: String): Option[String] = {
    iso639Map.get(code6392)
  }
}
