/*
 * Part of NDLA validation
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.validation

import no.ndla.common.configuration.Constants.EmbedTagName

class HtmlTagRulesTest extends UnitSuite {
  test("embed tag should be an allowed tag and contain data attributes") {
    HtmlTagRules.isTagValid("ndlaembed") should equal(true)
    val dataAttrs =
      TagAttribute.values.map(_.toString).filter(x => x.startsWith("data-"))
    val legalEmbedAttrs = HtmlTagRules.legalAttributesForTag(EmbedTagName)
    legalEmbedAttrs.foreach(x => dataAttrs should contain(x))
  }

  test("That isAttributeKeyValid returns false for illegal attributes") {
    HtmlTagRules.isAttributeKeyValid("data-random-junk", "td") should equal(false)
  }

  test("That isAttributeKeyValid returns true for legal attributes") {
    HtmlTagRules.isAttributeKeyValid("data-align", "td") should equal(true)
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
      TagAttribute.values.map(_.toString).filter(x => x.startsWith("lang") && x != TagAttribute.DataType.toString)
    val legalEmbedAttrs = HtmlTagRules.legalAttributesForTag("span")
    dataAttrs.foreach(x => legalEmbedAttrs should contain(x))
  }

  test("math should be legal") {
    HtmlTagRules.isTagValid("math") should be(true)
  }

}
