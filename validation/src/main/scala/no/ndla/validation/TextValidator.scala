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

class TextValidator {
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
      allowedTags: Set[_ <: String],
      requiredToOptional: Map[String, Seq[String]] = Map.empty
  ): Seq[ValidationMessage] = {
    if (allowedTags.isEmpty) {
      validateNoHtmlTags(fieldPath, text).toList
    } else {
      validateAllowedHtmlTags(fieldPath, text, requiredToOptional, new Safelist().addTags(allowedTags.toSeq: _*))
    }
  }

  def validateVisualElement(
      fieldPath: String,
      text: String,
      allowedTags: Set[_ <: String] = HtmlTagRules.allLegalTags,
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
          validateAllowedHtmlTags(fieldPath, text, requiredToOptional, new Safelist().addTags(allowedTags.toSeq: _*))
        }
      case Nil => errorWith("The root html element for visual elements needs to be `embed`.")
      case _   => errorWith("Visual element must be a string containing only a single embed element.")
    }
  }

  private def validateAllowedHtmlTags(
      fieldPath: String,
      text: String,
      requiredToOptional: Map[String, Seq[String]],
      whiteList: Safelist
  ): Seq[ValidationMessage] = {

    HtmlTagRules.allLegalTags
      .filter(tag => HtmlTagRules.legalAttributesForTag(tag).nonEmpty)
      .foreach(tag => whiteList.addAttributes(tag, HtmlTagRules.legalAttributesForTag(tag).toSeq: _*))

    if (text.isEmpty) {
      ValidationMessage(fieldPath, FieldEmpty) :: Nil
    } else {
      val whiteListValidationMessage = ValidationMessage(fieldPath, IllegalContentInBasicText)
      val jsoupValidatorMessages     = Option.when(!Jsoup.isValid(text, whiteList))(whiteListValidationMessage)
      TagValidator.validate(fieldPath, text, requiredToOptional) ++ jsoupValidatorMessages.toSeq
    }
  }

  private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] =
    Option.when(!Jsoup.isValid(text, Safelist.none())) {
      ValidationMessage(fieldPath, IllegalContentInPlainText)
    }
}
