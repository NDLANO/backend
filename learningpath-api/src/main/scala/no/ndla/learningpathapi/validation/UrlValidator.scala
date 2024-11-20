/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.common.errors.ValidationMessage
import no.ndla.learningpathapi.Props

trait UrlValidator {
  this: Props & TextValidator =>

  class UrlValidator {
    val noHtmlTextValidator = new TextValidator(allowHtml = false)

    def validate(fieldPath: String, url: String): Seq[ValidationMessage] = {
      nonEmptyText(fieldPath, url) ++
        noHtmlInText(fieldPath, url) ++
        urlIsValid(fieldPath, url)
    }

    private def nonEmptyText(fieldPath: String, url: String): Seq[ValidationMessage] = {
      if (url.isEmpty) {
        return List(ValidationMessage(fieldPath, "Required field is empty."))
      }
      List()
    }

    private def noHtmlInText(fieldPath: String, url: String): Seq[ValidationMessage] = {
      noHtmlTextValidator.validate(fieldPath, url) match {
        case Some(x) => List(x)
        case _       => List()
      }
    }

    private def urlIsValid(fieldPath: String, url: String): Seq[ValidationMessage] = {
      if (url.path.nonEmpty && url.schemeOption.isEmpty && url.hostOption.isEmpty)
        List.empty
      else if (!url.startsWith("https"))
        List(ValidationMessage(fieldPath, "Illegal Url. All Urls must start with https."))
      else
        List.empty
    }
  }
}
