/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.domain

trait LanguageField[T] {
  def value: T
  def language: String
  def isEmpty: Boolean
}
