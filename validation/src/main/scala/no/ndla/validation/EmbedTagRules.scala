/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import scala.language.postfixOps
import scala.io.Source

object EmbedTagRules {
  val ResourceHtmlEmbedTag = "embed"

  case class EmbedThings(attrsForResource: Map[ResourceType.Value, TagRules.TagAttributeRules])

  private[validation] lazy val attributeRules: Map[ResourceType.Value, TagRules.TagAttributeRules] = embedRulesToJson

  lazy val allEmbedTagAttributes: Set[TagAttributes.Value] = attributeRules.flatMap {
    case (_, attrRules) => attrRules.all
  } toSet

  def attributesForResourceType(resourceType: ResourceType.Value): TagRules.TagAttributeRules =
    attributeRules(resourceType)

  private def embedRulesToJson = {
    val attrs = TagRules.convertJsonStrToAttributeRules(Source.fromResource("embed-tag-rules.json").mkString)

    def strToResourceType(str: String): ResourceType.Value =
      ResourceType
        .valueOf(str)
        .getOrElse(
          throw new ConfigurationException(s"Missing declaration of resource type '$str' in ResourceType enum"))

    attrs.map {
      case (resourceType, attrRules) => strToResourceType(resourceType) -> attrRules
    }
  }
}

object ResourceType extends Enumeration {
  val Error = Value("error")
  val Image = Value("image")
  val Audio = Value("audio")
  val H5P = Value("h5p")
  val Brightcove = Value("brightcove")
  val ContentLink = Value("content-link")
  val ExternalContent = Value("external")
  val IframeContent = Value("iframe")
  val NRKContent = Value("nrk")
  val ConceptLink = Value("concept")
  val FootNote = Value("footnote")
  val CodeBlock = Value("code-block")
  val RelatedContent = Value("related-content")
  val File = Value("file")

  def all: Set[String] = ResourceType.values.map(_.toString)

  def valueOf(s: String): Option[ResourceType.Value] = {
    ResourceType.values.find(_.toString == s)
  }
}
