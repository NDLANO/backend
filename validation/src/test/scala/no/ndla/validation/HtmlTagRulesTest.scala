/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import no.ndla.mapping.UnitSuite

class HtmlTagRulesTest extends UnitSuite {
  test("embed tag should be an allowed tag and contain data attributes") {
    HtmlTagRules.isTagValid("embed")
    val dataAttrs =
      TagAttributes.values.map(_.toString).filter(x => x.startsWith("data-") && x != TagAttributes.DataType.toString)
    val legalEmbedAttrs = HtmlTagRules.legalAttributesForTag("embed")
    dataAttrs.foreach(x => legalEmbedAttrs should contain(x))
  }

  test("That isAttributeKeyValid returns false for illegal attributes") {
    HtmlTagRules.isAttributeKeyValid("data-random-junk", "td") should equal(false)
  }

  test("That isAttributeKeyValid returns true for legal attributes") {
    HtmlTagRules.isAttributeKeyValid("align", "td") should equal(true)
  }

  test("That isTagValid returns false for illegal tags") {
    HtmlTagRules.isTagValid("yodawg") should equal(false)
  }

  test("That isTagValid returns true for legal attributes") {
    HtmlTagRules.isTagValid("section") should equal(true)
  }

  test("span tag should be an allowed tag and contain one lang attribute") {
    HtmlTagRules.isTagValid("span")
    val dataAttrs =
      TagAttributes.values.map(_.toString).filter(x => x.startsWith("lang") && x != TagAttributes.DataType.toString)
    val legalEmbedAttrs = HtmlTagRules.legalAttributesForTag("span")
    dataAttrs.foreach(x => legalEmbedAttrs should contain(x))
  }

  test("math should be legal") {
    HtmlTagRules.isTagValid("math") should be(true)
  }

}
