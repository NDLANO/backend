/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.domain

object Errors {
  case class SubjectPageNotFoundException(id: Long) extends RuntimeException(s"The page with id $id was not found")
  case class LanguageNotFoundException(message: String, supportedLanguages: Seq[String] = Seq.empty)
      extends RuntimeException(message)
}
