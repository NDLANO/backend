/*
 * Part of NDLA validation
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

object TextValidator {
  private val IllegalContentInBasicText = "The content contains illegal tags and/or attributes. Allowed HTML tags are:"
  private val IllegalContentInPlainText = "The content contains illegal html-characters. No HTML is allowed"

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
      allowedTags: Set[String],
      requiredToOptional: Map[String, Seq[String]] = Map.empty
  ): Seq[ValidationMessage] = {
    if (allowedTags.isEmpty) {
      validateNoHtmlTags(fieldPath, text).toList
    } else {
      validateAllowedHtmlTags(fieldPath, text, requiredToOptional, allowedTags)
    }
  }

  def validateVisualElement(
      fieldPath: String,
      text: String,
      allowedTags: Set[String] = HtmlTagRules.allLegalTags,
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
          validateAllowedHtmlTags(fieldPath, text, requiredToOptional, allowedTags)
        }
      case Nil => errorWith("The root html element for visual elements needs to be `embed`.")
      case _   => errorWith("Visual element must be a string containing only a single embed element.")
    }
  }

  private def validateAllowedHtmlTags(
      fieldPath: String,
      text: String,
      requiredToOptional: Map[String, Seq[String]],
      allowedTags: Set[String]
  ): Seq[ValidationMessage] = {

    val whiteList = new Safelist().addTags(allowedTags.toSeq: _*)
    HtmlTagRules.allLegalTags
      .filter(tag => HtmlTagRules.legalAttributesForTag(tag).nonEmpty)
      .foreach(tag => whiteList.addAttributes(tag, HtmlTagRules.legalAttributesForTag(tag).toSeq: _*))

    if (text.isEmpty) {
      Seq.empty
    } else {
      val whiteListValidationMessage =
        ValidationMessage(fieldPath, s"$IllegalContentInBasicText ${allowedTags.mkString(",")}")
      val jsoupValidatorMessages = Option.when(!Jsoup.isValid(text, whiteList))(whiteListValidationMessage)
      TagValidator.validate(fieldPath, text, requiredToOptional) ++ jsoupValidatorMessages.toSeq
    }
  }

  private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] =
    Option.when(!Jsoup.isValid(text, Safelist.none())) {
      ValidationMessage(fieldPath, IllegalContentInPlainText)
    }
}
