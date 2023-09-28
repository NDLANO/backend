/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationMessage
import no.ndla.mapping.UnitSuite
import no.ndla.validation.TagRules.Condition

class EmbedTagRulesTest extends UnitSuite {

  test("Rules for all resource types should be defined") {
    val resourceTypesFromConfigFile      = EmbedTagRules.attributeRules.keys
    val resourceTypesFromEnumDeclaration = ResourceType.values

    resourceTypesFromEnumDeclaration should equal(resourceTypesFromConfigFile)
  }

  test("data-resource should be required for all resource types") {
    val resourceTypesFromConfigFile = EmbedTagRules.attributeRules.keys

    resourceTypesFromConfigFile.foreach(resType =>
      EmbedTagRules.attributesForResourceType(resType).required.map(f => f.name) should contain(TagAttribute.DataResource)
    )
  }

  test("Every mustBeDirectChildOf -> condition block must be valid") {
    val embedTagValidator = new TagValidator()

    EmbedTagRules.attributeRules.flatMap { case (tag, rule) =>
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
              "childCount must start with a supported operator (<, >, =) and consist of an integer (Ex: '> 1')."
          )
        )
      )
    )
  }

  test("RequiredNonEmpty fields should not be allowed to be empty-strings") {
    val embedString =
      s"""<$EmbedTagName
        | data-resource="image"
        | data-resource_id=""
        | data-size=""
        | data-align=""
        | data-alt=""
        | data-caption=""
        |/>""".stripMargin
    val embedTagValidator = new TagValidator()

    val result = embedTagValidator.validate("test", embedString)
    result should be(
      Seq(
        ValidationMessage(
          "test",
          s"An $EmbedTagName HTML tag with data-resource=image must contain non-empty attributes: data-resource_id."
        )
      )
    )
  }

  test("Optional standalone fields without coExisting is OK") {
    val embedString =
      s"""<$EmbedTagName
         | data-resource="external"
         | data-url="https://youtube.com"
         | data-type="external"
         | data-title="Youtube-video"
         |/>""".stripMargin
    val embedTagValidator = new TagValidator()

    val result = embedTagValidator.validate("test", embedString)
    result should be(
      Seq.empty
    )
  }

  test("Optional fields dependent on others is !OK") {
    val embedString =
      s"""<$EmbedTagName
         | data-resource="external"
         | data-url="https://youtube.com"
         | data-type="external"
         | data-title="Youtube-video"
         | data-imageid="123"
         |/>""".stripMargin
    val embedTagValidator = new TagValidator()

    val result = embedTagValidator.validate("test", embedString)
    result should be(
      Seq(
        ValidationMessage(
          "test",
          s"An $EmbedTagName HTML tag with data-resource=external must contain all or none of the attributes in the optional attribute group: (data-caption (Missing: data-caption))"
        )
      )
    )
  }

}
