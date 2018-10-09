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
    LanguageTag("am-deva").toString should equal ("am-deva")
  }

  test("that toString outputs language-script-region") {
    LanguageTag("en-latn-gb").toString should equal ("en-latn-gb")
  }

  test("that toString outputs language and region when no script") {
    LanguageTag("en-gb").toString should equal ("en-gb")
  }

  test("that to string only outputs language-code when no script and no region") {
    LanguageTag("fra").toString should equal ("fr")
    LanguageTag("fre").toString should equal ("fr")
    LanguageTag("fr").toString should equal ("fr")
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

  test("that two-lettered code is used when it exists") {
    LanguageTag("eng").toString should equal ("en")
  }

  test("that three-lettered code is used when two-lettered code doesn't exist") {
    LanguageTag("hrc").toString should equal ("hrc")
  }

  test("that arabic without script is considered RTL-script") {
    LanguageTag("ar").isRightToLeft should be (true)
  }

  test("that arabic with latin script is not considered RTL-script") {
    LanguageTag("ar-latn").isRightToLeft should be (false)
  }

  test("that norwegian with hebrew script is considered RTL") {
    LanguageTag("nb-hebr").isRightToLeft should be (true)
  }

  test("that hausa as spoken in Ethiopia with arabic script is considered RTL") {
    LanguageTag("ha-arab-et").isRightToLeft should be(true)
  }

  test("that Norwegian is not considered RTL") {
    LanguageTag("nb").isRightToLeft should be (false)
  }


}
