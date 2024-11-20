/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.common.errors.ValidationMessage
import no.ndla.learningpathapi.Props
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

trait TextValidator {
  this: Props =>

  class TextValidator(allowHtml: Boolean) {
    import props.*

    val IllegalContentInBasicText: String =
      s"The content contains illegal html-characters. Allowed characters are ${BasicHtmlTags.mkString(", ")}"

    val IllegalContentInPlainText =
      "The content contains illegal html-characters. No HTML is allowed."
    private val FieldEmpty = "Required field is empty."

    def validate(fieldPath: String, text: String): Option[ValidationMessage] = {
      if (allowHtml) {
        validateOnlyBasicHtmlTags(fieldPath, text)
      } else {
        validateNoHtmlTags(fieldPath, text)
      }
    }

    private def validateOnlyBasicHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
      if (text.isEmpty) {
        Some(ValidationMessage(fieldPath, FieldEmpty))
      } else {
        if (Jsoup.isValid(text, new Safelist().addTags(BasicHtmlTags*))) {
          None
        } else {
          Some(ValidationMessage(fieldPath, IllegalContentInBasicText))
        }
      }
    }

    private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
      if (Jsoup.isValid(text, Safelist.none())) {
        None
      } else {
        Some(ValidationMessage(fieldPath, IllegalContentInPlainText))
      }
    }
  }
}
