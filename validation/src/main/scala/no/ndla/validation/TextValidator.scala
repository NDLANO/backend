/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationMessage
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

import scala.jdk.CollectionConverters.IteratorHasAsScala

class TextValidator(allowHtml: Boolean) {
  private def IllegalContentInBasicText =
    s"The content contains illegal tags and/or attributes. Allowed HTML tags are: ${HtmlTagRules.allLegalTags.mkString(", ")}"
  private val IllegalContentInPlainText = "The content contains illegal html-characters. No HTML is allowed"
  private val FieldEmpty                = "Required field is empty"
  private val TagValidator              = new TagValidator

  /** Validates text Will validate legal html tags if html is allowed.
    *
    * @param fieldPath
    *   Path to return in the [[ValidationMessage]]'s if there are any
    * @param text
    *   Text to validate
    * @param requiredToOptional
    *   Map from resource-type to Seq of embed tag attributes to treat as optional rather than required for this
    *   validation. Example Map("image" -> Seq("data-caption")) to make data-caption optional for "image" on this
    *   validation.
    * @return
    *   Seq of [[ValidationMessage]]'s describing issues with validation
    */
  def validate(
      fieldPath: String,
      text: String,
      requiredToOptional: Map[String, Seq[String]] = Map.empty
  ): Seq[ValidationMessage] = {
    allowHtml match {
      case true  => validateOnlyBasicHtmlTags(fieldPath, text, requiredToOptional)
      case false => validateNoHtmlTags(fieldPath, text).toList
    }
  }

  def validateVisualElement(
      fieldPath: String,
      text: String,
      requiredToOptional: Map[String, Seq[String]] = Map.empty
  ): Seq[ValidationMessage] = {

    val errorWith = (msg: String) => Seq(ValidationMessage(fieldPath, msg))

    val body     = HtmlTagRules.stringToJsoupDocument(text)
    val elemList = body.children().iterator().asScala.toList

    elemList match {
      case onlyElement :: Nil =>
        if (onlyElement.tagName() != EmbedTagName) {
          errorWith("The root html element for visual elements needs to be `embed`.")
        } else {
          validateOnlyBasicHtmlTags(fieldPath, text, requiredToOptional)
        }
      case Nil => errorWith("The root html element for visual elements needs to be `embed`.")
      case _   => errorWith("Visual element must be a string containing only a single embed element.")
    }
  }

  private def validateOnlyBasicHtmlTags(
      fieldPath: String,
      text: String,
      requiredToOptional: Map[String, Seq[String]]
  ): Seq[ValidationMessage] = {
    val whiteList = new Safelist().addTags(HtmlTagRules.allLegalTags.toSeq: _*)

    HtmlTagRules.allLegalTags
      .filter(tag => HtmlTagRules.legalAttributesForTag(tag).nonEmpty)
      .foreach(tag => whiteList.addAttributes(tag, HtmlTagRules.legalAttributesForTag(tag).toSeq: _*))

    text.isEmpty match {
      case true => ValidationMessage(fieldPath, FieldEmpty) :: Nil
      case false => {
        val jsoupValidatorMessages = Jsoup.isValid(text, whiteList) match {
          case true  => None
          case false => Some(ValidationMessage(fieldPath, IllegalContentInBasicText))
        }
        TagValidator.validate(fieldPath, text, requiredToOptional) ++ jsoupValidatorMessages.toSeq
      }

    }
  }

  private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
    Jsoup.isValid(text, Safelist.none()) match {
      case true  => None
      case false => Some(ValidationMessage(fieldPath, IllegalContentInPlainText))
    }
  }
}
