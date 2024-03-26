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
    import props._

    val IllegalContentInBasicText: String =
      s"The content contains illegal html-characters. Allowed characters are ${BasicHtmlTags.mkString(", ")}"

    val IllegalContentInPlainText =
      "The content contains illegal html-characters. No HTML is allowed."
    val FieldEmpty = "Required field is empty."

    def validate(fieldPath: String, text: String): Option[ValidationMessage] = {
      allowHtml match {
        case true  => validateOnlyBasicHtmlTags(fieldPath, text)
        case false => validateNoHtmlTags(fieldPath, text)
      }
    }

    private def validateOnlyBasicHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
      text.isEmpty match {
        case true => Some(ValidationMessage(fieldPath, FieldEmpty))
        case false => {
          Jsoup.isValid(text, new Safelist().addTags(BasicHtmlTags: _*)) match {
            case true => None
            case false =>
              Some(ValidationMessage(fieldPath, IllegalContentInBasicText))
          }
        }
      }
    }

    private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
      Jsoup.isValid(text, Safelist.none()) match {
        case true => None
        case false =>
          Some(ValidationMessage(fieldPath, IllegalContentInPlainText))
      }
    }
  }
}
