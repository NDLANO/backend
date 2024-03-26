/*
 * Part of NDLA validation
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation

import no.ndla.common.errors.ValidationMessage
import no.ndla.common.model.domain.ArticleType

import scala.util.matching.Regex

object SlugValidator {

  private def articleTypeValidationMessage(articleType: String): Option[ValidationMessage] = {
    Some(
      ValidationMessage(
        "articleType",
        s"articleType needs to be of type $articleType when slug is defined"
      )
    )
  }

  private def slugValidationMessage(articleType: String): Option[ValidationMessage] = {
    Some(
      ValidationMessage(
        "slug",
        s"slug field must be defined when articleType is of type $articleType"
      )
    )
  }

  private def validateSlugString(slug: String): Option[ValidationMessage] = {
    // only a-z,A-Z,0-9 and hyphen(-) allowed in slug paths
    val slugPattern = new Regex("^([a-zA-Z0-9-]+)$")
    if (slugPattern.matches(slug)) None else Some(ValidationMessage("slug", "The string contains invalid characters"))
  }

  private def validateSlugStringAndCheckIfSlugIsUnique(
      slug: String,
      articleId: Option[Long],
      checkIfSlugExistsFunc: (String, Option[Long]) => Boolean
  ): Option[ValidationMessage] = {
    validateSlugString(slug) match {
      case Some(error) => Some(error)
      case None =>
        if (checkIfSlugExistsFunc(slug, articleId))
          Some(ValidationMessage("slug", "This field should be unique. The chosen path exist already."))
        else None
    }
  }

  def validateSlug(
      maybeSlug: Option[String],
      articleType: ArticleType,
      articleId: Option[Long],
      checkIfSlugExistsFunc: (String, Option[Long]) => Boolean
  ): Option[ValidationMessage] = {
    maybeSlug match {
      case Some(_) if articleType != ArticleType.FrontpageArticle =>
        articleTypeValidationMessage(ArticleType.FrontpageArticle.entryName)
      case None if articleType == ArticleType.FrontpageArticle =>
        slugValidationMessage(ArticleType.FrontpageArticle.entryName)
      case None       => None
      case Some(slug) => validateSlugStringAndCheckIfSlugIsUnique(slug, articleId, checkIfSlugExistsFunc)
    }
  }
}
