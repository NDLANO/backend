/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import io.digitallibrary.language.UnitSuite

class LanguageTagTest extends UnitSuite {

  test("that toString outputs language-script when no region") {
    LanguageTag("am-deva").toString should equal ("amh-deva")
  }

  test("that toString outputs language-script-region") {
    LanguageTag("en-latn-gb").toString should equal ("eng-latn-gb")
  }

  test("that toString outputs language and region when no script") {
    LanguageTag("en-gb").toString should equal ("eng-gb")
  }

  test("that to string only outputs language-code when no script and no region") {
    LanguageTag("fra").toString should equal ("fra")
    LanguageTag("fre").toString should equal ("fra")
    LanguageTag("fr").toString should equal ("fra")
  }

  test("that apply throws LanguageNotSupportedException when invalid number of subtags") {
    intercept[LanguageNotSupportedException](
      LanguageTag("eng-latn-gb-invalid")
    ).getMessage should equal ("The language tag 'eng-latn-gb-invalid' is not supported.")
  }

  test("that LanguageSubtagNotSupportedException is thrown when invalid iso639-code") {
    intercept[LanguageSubtagNotSupportedException](
      LanguageTag("abasd")
    ).getMessage should equal ("The language subtag 'abasd' is not supported.")
  }

  test("that ScriptSubtagNotSupportedException is thrown when invalid iso15924-code") {
    intercept[ScriptSubtagNotSupportedException](
      LanguageTag("eng-abcd")
    ).getMessage should equal ("The script subtag 'abcd' is not supported.")
  }

  test("that RegionSubtagNotSupportedException is thrown when invalid iso3166-code") {
    intercept[RegionSubtagNotSupportedException](
      LanguageTag("eng-22")
    ).getMessage should equal ("The region subtag '22' is not supported.")
  }


  test("that displayName only returns 'language' when no script or region subtag"){
    LanguageTag("eng").displayName should equal ("English")
  }

  test("that displayName returns 'language (script)' when no region subtag"){
    LanguageTag("eng-latn").displayName should equal ("English (Latin)")
  }

  test("that displayName returns 'language (region)' when no script subtag"){
    LanguageTag("eng-gb").displayName should equal ("English (United Kingdom)")
  }

  test("that displayName returns 'language (script, region)' when all defined"){
    LanguageTag("eng-latn-gb").displayName should equal ("English (Latin, United Kingdom)")
  }

  test("that localDisplayName returns None for Language that does not have a mapping") {
    LanguageTag("eng").localDisplayName should be (None)
  }

  test("that localDisplayName returns a displayname in the local language when it does have a mapping") {
    LanguageTag("nob").localDisplayName should equal (Some("Norsk (bokmål)"))
  }
}
