/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language

import io.digitallibrary.language.model.{Iso15924Def, Iso3166Def, Iso639Def, LanguageTag}

object TestData {


  val DefaultLanguageTag = LanguageTag("amh-ethi-et")
  val DefaultIso639Def = Iso639Def("amh", Some("amh"), Some("amh"), Some("am"), Some("I"), Some("L"), "Amharic", None)
  val DefaultIso15924Def = Iso15924Def("Ethi", 430, "Ethiopic (Geʻez)", "éthiopien (geʻez, guèze)", Some("Ethiopic"), "2004-10-25")
  val DefaultIso3166Def = Iso3166Def("ET", "Ethiopia")

}
