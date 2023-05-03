/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import scala.io.Source
import scala.language.postfixOps

object EmbedTagRules {
  case class EmbedThings(attrsForResource: Map[ResourceType.Value, TagRules.TagAttributeRules])

  private[validation] lazy val attributeRules: Map[ResourceType.Value, TagRules.TagAttributeRules] = embedRulesToJson

  lazy val allEmbedTagAttributes: Set[TagAttributes.Value] = attributeRules.flatMap { case (_, attrRules) =>
    attrRules.all
  } toSet

  def attributesForResourceType(resourceType: ResourceType.Value): TagRules.TagAttributeRules =
    attributeRules(resourceType)

  private def embedRulesToJson: Map[ResourceType.Value, TagRules.TagAttributeRules] = {
    val attrs = TagRules.convertJsonStrToAttributeRules(Source.fromResource("embed-tag-rules.json").mkString)

    def strToResourceType(str: String): ResourceType.Value =
      ResourceType
        .valueOf(str)
        .getOrElse(
          throw new ConfigurationException(s"Missing declaration of resource type '$str' in ResourceType enum")
        )

    attrs.map { case (resourceType, attrRules) =>
      strToResourceType(resourceType) -> attrRules
    }
  }
}

object ResourceType extends Enumeration {
  val Error: ResourceType.Value                   = Value("error")
  val Image: ResourceType.Value                   = Value("image")
  val Audio: ResourceType.Value                   = Value("audio")
  val H5P: ResourceType.Value                     = Value("h5p")
  val Brightcove: ResourceType.Value              = Value("brightcove")
  val ContentLink: ResourceType.Value             = Value("content-link")
  val ExternalContent: ResourceType.Value         = Value("external")
  val IframeContent: ResourceType.Value           = Value("iframe")
  val NRKContent: ResourceType.Value              = Value("nrk")
  val Concept: ResourceType.Value                 = Value("concept")
  val ConceptList: ResourceType.Value             = Value("concept-list")
  val FootNote: ResourceType.Value                = Value("footnote")
  val CodeBlock: ResourceType.Value               = Value("code-block")
  val RelatedContent: ResourceType.Value          = Value("related-content")
  val File: ResourceType.Value                    = Value("file")
  val ContactBlock: ResourceType.Value            = Value("contact-block")
  val BlogPost: ResourceType.Value                = Value("blog-post")
  val KeyPerformanceIndicator: ResourceType.Value = Value("key-performance-indicator")

  def all: Set[String] = ResourceType.values.map(_.toString)

  def valueOf(s: String): Option[ResourceType.Value] = {
    ResourceType.values.find(_.toString == s)
  }
}
