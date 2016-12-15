/*
 * Part of NDLA mapping.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.mapping

class ISO639Test extends UnitSuite {
  test("get6391CodeFor6392CodeMappings returns a map of language mappings") {
    ISO639.get6391CodeFor6392CodeMappings.size should equal (10)
    ISO639.get6391CodeFor6392CodeMappings("nob") should equal ("nb")
    ISO639.get6391CodeFor6392CodeMappings("nno") should equal ("nn")
    ISO639.get6391CodeFor6392CodeMappings("eng") should equal ("en")
  }

  test("get6391CodeFor6392Code returns a language code if input code is valid") {
    ISO639.get6391CodeFor6392Code("sme") should equal (Some("se"))
  }

  test("get6391CodeFor6392Code returns a None if input code is invalid") {
    ISO639.get6391CodeFor6392Code("invalid") should equal (None)
  }
}
