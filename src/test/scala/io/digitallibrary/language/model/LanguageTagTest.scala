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
    LanguageTag("amh", Some("latn"), None).toString should equal ("amh-latn")
  }

  test("that toString outputs language-script-region") {
    LanguageTag("amh", Some("latn"), Some("no")).toString should equal ("amh-latn-no")
  }

  test("that toString outputs language and region when no script") {
    LanguageTag("amh", None, Some("no")).toString should equal ("amh-no")
  }

  test("that to string only outputs language when no script and no region") {
    LanguageTag("amh", None, None).toString should equal ("amh")
  }

  test("that apply throws LanguageNotSupportedException when invalid number of subtags") {
    intercept[LanguageNotSupportedException] {
      LanguageTag("abc-def-ghi-jkl-mnn-op")
    }.getMessage.contains("'abc-def-ghi-jkl-mnn-op'") should be (true)
  }

}
