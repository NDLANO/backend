/*
 * Part of NDLA validation
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

class ValidationRulesTest extends UnitSuite {

  test("That HtmlRulesFile is deserialized without failing") {
    ValidationRules.htmlRulesJson
  }

  test("That MathMLRulesFile is deserialized without failing") {
    ValidationRules.mathMLRulesJson
  }

}
