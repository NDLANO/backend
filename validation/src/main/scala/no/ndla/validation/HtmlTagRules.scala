/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import no.ndla.validation.EmbedTagRules._
import org.json4s.native.JsonMethods._
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import scala.io.Source
import scala.language.postfixOps
import scala.jdk.CollectionConverters._

object HtmlTagRules {

  case class HtmlThings(attrsForResource: Map[String, TagRules.TagAttributeRules])

  private[validation] lazy val attributeRules: Map[String, TagRules.TagAttributeRules] = tagRulesToJson
  lazy val allHtmlTagAttributes: Set[TagAttributes.Value] = attributeRules.flatMap {
    case (_, attrRules) => attrRules.all
  } toSet

  private def tagRulesToJson = {
    val attrs = TagRules
      .convertJsonStrToAttributeRules(Source.fromResource("html-rules.json").mkString)

    attrs.map {
      case (tagType, attrRules) =>
        tagType -> attrRules
    }
  }

  def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.select("body").first()
  }

  def jsoupDocumentToString(element: Element): String = {
    element.select("body").html()
  }

  object PermittedHTML {
    val tags: Set[String] = readTags
    lazy val attributes: Map[String, Seq[String]] = readAttributes

    private def convertJsonStr(jsonStr: String): Map[String, Any] = {
      implicit val formats = org.json4s.DefaultFormats
      parse(jsonStr).extract[Map[String, Any]]
    }

    private def htmlRulesJson: Map[String, Any] = convertJsonStr(Source.fromResource("html-rules.json").mkString)

    private def mathMLRulesJson: Map[String, Any] = convertJsonStr(Source.fromResource("mathml-rules.json").mkString)

    private def readTags: Set[String] = {
      val htmlJson: Map[String, Any] = htmlRulesJson
      val mathMlJson: Map[String, Any] = mathMLRulesJson

      val htmlTags = htmlJson.get("tags").map(_.asInstanceOf[Seq[String]].toSet)
      val mathMlTags = mathMlJson.get("tags").map(_.asInstanceOf[Seq[String]].toSet)

      htmlTags.getOrElse(Set.empty) ++ mathMlTags.getOrElse(Set.empty) ++ attributes.keys
    }

    private def readAttributes: Map[String, Seq[String]] = {
      val mathMlJson: Map[String, Any] = mathMLRulesJson

      val htmlAttrs = HtmlTagRules.attributeRules.map {
        case (tagType, attrs) => tagType -> attrs.all.map(_.toString).toSeq
      }
      val mathMlAttrs = mathMlJson.get("attributes").map(_.asInstanceOf[Map[String, Seq[String]]])
      val embedAttrs = EmbedTagRules.allEmbedTagAttributes.map(_.toString).toSeq
      htmlAttrs ++ mathMlAttrs.getOrElse(Map.empty) ++ Map(ResourceHtmlEmbedTag -> embedAttrs)
    }
  }

  def isAttributeKeyValid(attributeKey: String, tagName: String): Boolean = {
    val legalAttrs = legalAttributesForTag(tagName)
    legalAttrs.contains(attributeKey)
  }

  def isTagValid(tagName: String): Boolean = PermittedHTML.tags.contains(tagName)

  def allLegalTags: Set[String] = PermittedHTML.tags

  def attributesForTagType(tagType: String): Seq[String] = PermittedHTML.attributes.getOrElse(tagType, Seq.empty)

  def tagAttributesForTagType(tagType: String): Option[TagRules.TagAttributeRules] = attributeRules.get(tagType)

  def legalAttributesForTag(tagName: String): Set[String] = attributesForTagType(tagName).toSet

  def tagMustContainAtLeastOneOptionalAttribute(tagName: String): Boolean =
    tagAttributesForTagType(tagName).exists(_.mustContainAtLeastOneOptionalAttribute)

  def removeIllegalAttributes(el: Element, legalAttributes: Set[String]): Seq[String] = {
    el.attributes()
      .asScala
      .toList
      .filter(attr => !legalAttributes.contains(attr.getKey))
      .map(illegalAttribute => {
        val keyName = illegalAttribute.getKey
        el.removeAttr(keyName)
        keyName
      })
  }
}
