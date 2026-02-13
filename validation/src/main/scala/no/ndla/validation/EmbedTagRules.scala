/*
 * Part of NDLA validation
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.validation

import enumeratum.*
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

sealed abstract class EmbedType(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}

object EmbedType extends Enum[EmbedType] {
  val values: IndexedSeq[EmbedType] = findValues

  case object Audio           extends EmbedType("audio")
  case object Brightcove      extends EmbedType("brightcove")
  case object CampaignBlock   extends EmbedType("campaign-block")
  case object CodeBlock       extends EmbedType("code-block")
  case object Comment         extends EmbedType("comment")
  case object Concept         extends EmbedType("concept")
  case object ContactBlock    extends EmbedType("contact-block")
  case object ContentLink     extends EmbedType("content-link")
  case object Copyright       extends EmbedType("copyright")
  case object Error           extends EmbedType("error")
  case object ExternalContent extends EmbedType("external")
  case object File            extends EmbedType("file")
  case object FootNote        extends EmbedType("footnote")
  case object H5P             extends EmbedType("h5p")
  case object IframeContent   extends EmbedType("iframe")
  case object Image           extends EmbedType("image")
  case object KeyFigure       extends EmbedType("key-figure")
  case object LinkBlock       extends EmbedType("link-block")
  case object NRKContent      extends EmbedType("nrk")
  case object Pitch           extends EmbedType("pitch")
  case object RelatedContent  extends EmbedType("related-content")
  case object Symbol          extends EmbedType("symbol")
  case object UuDisclaimer    extends EmbedType("uu-disclaimer")

}
