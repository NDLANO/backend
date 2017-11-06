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
  private def IllegalContentInBasicText = s"The content contains illegal tags and/or attributes. Allowed HTML tags are: ${HtmlRules.allLegalTags.mkString(",")}"
  private val IllegalContentInPlainText = "The content contains illegal html-characters. No HTML is allowed"
  private val FieldEmpty = "Required field is empty"
  private val EmbedTagValidator = new EmbedTagValidator

  def validate(fieldPath: String, text: String): Seq[ValidationMessage] = {
    allowHtml match {
      case true => validateOnlyBasicHtmlTags(fieldPath, text)
      case false => validateNoHtmlTags(fieldPath, text).toList
    }
  }

  private def validateOnlyBasicHtmlTags(fieldPath: String, text: String): Seq[ValidationMessage] = {
    val whiteList = new Whitelist().addTags(HtmlRules.allLegalTags.toSeq: _*)
    HtmlRules.allLegalTags
      .filter(tag => HtmlRules.legalAttributesForTag(tag).nonEmpty)
      .foreach(tag => whiteList.addAttributes(tag, HtmlRules.legalAttributesForTag(tag).toSeq: _*))

    text.isEmpty match {
      case true => ValidationMessage(fieldPath, FieldEmpty) :: Nil
      case false => {
        val jsoupValidatorMessages = Jsoup.isValid(text, whiteList) match {
          case true => None
          case false => Some(ValidationMessage(fieldPath, IllegalContentInBasicText))
        }
        val embedTagValidatorMessages = EmbedTagValidator.validate(fieldPath, text)
        jsoupValidatorMessages.toList ++ embedTagValidatorMessages
      }

    }
  }

  private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
    Jsoup.isValid(text, Whitelist.none()) match {
      case true => None
      case false => Some(ValidationMessage(fieldPath, IllegalContentInPlainText))
    }
  }
}
