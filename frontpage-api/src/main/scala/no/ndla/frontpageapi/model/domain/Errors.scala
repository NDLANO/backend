/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

object Errors {
  case class LanguageNotFoundException(message: String, supportedLanguages: Seq[String] = Seq.empty)
      extends RuntimeException(s"$message, supportedLanguages: [${supportedLanguages.mkString(",")}]")
  case class ValidationException(message: String)          extends RuntimeException(message)
  case class OperationNotAllowedException(message: String) extends RuntimeException(message)
  case class MissingIdException()
      extends RuntimeException(s"Could not convert to api.SubjectPageData since domain object did not have an id")
}
