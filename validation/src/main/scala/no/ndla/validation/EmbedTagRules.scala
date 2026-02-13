/*
 * Part of NDLA validation
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.validation

import no.ndla.common.model.TagAttribute

import scala.io.Source
import scala.language.postfixOps

object EmbedTagRules {
  private[validation] lazy val attributeRules: Map[EmbedType, TagRules.TagAttributeRules] = embedRulesToJson

  lazy val allEmbedTagAttributes: Set[TagAttribute] = attributeRules.flatMap { case (_, attrRules) =>
    attrRules.all
  } toSet

  def attributesForResourceType(resourceType: EmbedType): TagRules.TagAttributeRules = attributeRules(resourceType)

  private def embedRulesToJson: Map[EmbedType, TagRules.TagAttributeRules] = {
    val classLoader = getClass.getClassLoader
    val jsonStr     = Source.fromResource("embed-tag-rules.json", classLoader).mkString
    val attrs       = TagRules.convertJsonStrToAttributeRules(jsonStr)

    def strToResourceType(str: String): EmbedType = EmbedType
      .withNameOption(str)
      .getOrElse(throw new ConfigurationException(s"Missing declaration of resource type '$str' in EmbedType enum"))

    attrs.map { case (resourceType, attrRules) =>
      strToResourceType(resourceType) -> attrRules
    }
  }
}
