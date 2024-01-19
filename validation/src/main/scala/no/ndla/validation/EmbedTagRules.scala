/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import enumeratum._

import scala.io.Source
import scala.language.postfixOps

object EmbedTagRules {
  private[validation] lazy val attributeRules: Map[ResourceType, TagRules.TagAttributeRules] = embedRulesToJson

  lazy val allEmbedTagAttributes: Set[TagAttribute] = attributeRules.flatMap { case (_, attrRules) =>
    attrRules.all
  } toSet

  def attributesForResourceType(resourceType: ResourceType): TagRules.TagAttributeRules =
    attributeRules(resourceType)

  private def embedRulesToJson: Map[ResourceType, TagRules.TagAttributeRules] = {
    val classLoader = getClass.getClassLoader
    val attrs =
      TagRules.convertJsonStrToAttributeRules(Source.fromResource("embed-tag-rules.json", classLoader).mkString)

    def strToResourceType(str: String): ResourceType =
      ResourceType
        .withNameOption(str)
        .getOrElse(
          throw new ConfigurationException(s"Missing declaration of resource type '$str' in ResourceType enum")
        )

    attrs.map { case (resourceType, attrRules) =>
      strToResourceType(resourceType) -> attrRules
    }
  }
}

sealed abstract class ResourceType(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}

object ResourceType extends Enum[ResourceType] {
  val values: IndexedSeq[ResourceType] = findValues

  case object Error           extends ResourceType("error")
  case object Image           extends ResourceType("image")
  case object Audio           extends ResourceType("audio")
  case object H5P             extends ResourceType("h5p")
  case object Brightcove      extends ResourceType("brightcove")
  case object ContentLink     extends ResourceType("content-link")
  case object ExternalContent extends ResourceType("external")
  case object IframeContent   extends ResourceType("iframe")
  case object NRKContent      extends ResourceType("nrk")
  case object Concept         extends ResourceType("concept")
  case object ConceptList     extends ResourceType("concept-list")
  case object FootNote        extends ResourceType("footnote")
  case object CodeBlock       extends ResourceType("code-block")
  case object RelatedContent  extends ResourceType("related-content")
  case object File            extends ResourceType("file")
  case object ContactBlock    extends ResourceType("contact-block")
  case object BlogPost        extends ResourceType("blog-post")
  case object KeyFigure       extends ResourceType("key-figure")
  case object CampaignBlock   extends ResourceType("campaign-block")
  case object LinkBlock       extends ResourceType("link-block")
  case object UuDisclaimer    extends ResourceType("uu-disclaimer")

}
