/*
 * Part of NDLA validation.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

class TextValidator(allowHtml: Boolean) {
  private def IllegalContentInBasicText =
    s"The content contains illegal tags and/or attributes. Allowed HTML tags are: ${HtmlTagRules.allLegalTags.mkString(",")}"
  private val IllegalContentInPlainText = "The content contains illegal html-characters. No HTML is allowed"
  private val FieldEmpty = "Required field is empty"
  private val TagValidator = new TagValidator

  /**
    * Validates text
    * Will validate legal html tags if html is allowed.
    *
    * @param fieldPath Path to return in the [[ValidationMessage]]'s if there are any
    * @param text Text to validate
    * @param validateEmbedTagParent Whether to validate parents of embed tags where those are required.
    * @param requiredToOptional Map from resource-type to Seq of embed tag attributes to treat as optional rather than required for this validation.
    *                           Example Map("image" -> Seq("data-caption")) to make data-caption optional for "image" on this validation.
    * @return Seq of [[ValidationMessage]]'s describing issues with validation
    */
  def validate(
      fieldPath: String,
      text: String,
      validateEmbedTagParent: Boolean = true,
      requiredToOptional: Map[String, Seq[String]] = Map.empty
  ): Seq[ValidationMessage] = {
    allowHtml match {
      case true  => validateOnlyBasicHtmlTags(fieldPath, text, validateEmbedTagParent, requiredToOptional)
      case false => validateNoHtmlTags(fieldPath, text).toList
    }
  }

  private def validateOnlyBasicHtmlTags(
      fieldPath: String,
      text: String,
      validateParent: Boolean,
      requiredToOptional: Map[String, Seq[String]]
  ): Seq[ValidationMessage] = {
    val whiteList = new Whitelist().addTags(HtmlTagRules.allLegalTags.toSeq: _*)

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
        TagValidator.validate(fieldPath, text, validateParent, requiredToOptional) ++ jsoupValidatorMessages.toSeq
      }

    }
  }

  private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
    Jsoup.isValid(text, Whitelist.none()) match {
      case true  => None
      case false => Some(ValidationMessage(fieldPath, IllegalContentInPlainText))
    }
  }
}
