/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import no.ndla.mapping.UnitSuite
import no.ndla.validation.TagRules.Condition

class EmbedTagRulesTest extends UnitSuite {

  test("Rules for all resource types should be defined") {
    val resourceTypesFromConfigFile = EmbedTagRules.attributeRules.keys
    val resourceTypesFromEnumDeclaration = ResourceType.values

    resourceTypesFromEnumDeclaration should equal(resourceTypesFromConfigFile)
  }

  test("data-resource should be required for all resource types") {
    val resourceTypesFromConfigFile = EmbedTagRules.attributeRules.keys

    resourceTypesFromConfigFile.foreach(resType =>
      EmbedTagRules.attributesForResourceType(resType).required should contain(TagAttributes.DataResource))
  }

  test("Every mustBeDirectChildOf -> condition block must be valid") {
    val embedTagValidator = new TagValidator()

    EmbedTagRules.attributeRules.flatMap {
      case (tag, rule) =>
        rule.mustBeDirectChildOf.flatMap(parentRule => {
          parentRule.conditions.map(c => {
            val res = embedTagValidator.checkParentConditions(tag.toString, c, 1)
            res.isRight should be(true)
          })
        })
    }

    val result1 = embedTagValidator.checkParentConditions("test", Condition("apekatt=2"), 3)
    result1 should be(
      Left(
        Seq(
          ValidationMessage(
            "test",
            "Parent condition block is invalid. " +
              "childCount must start with a supported operator (<, >, =) and consist of an integer (Ex: '> 1').")))
    )
  }

}
