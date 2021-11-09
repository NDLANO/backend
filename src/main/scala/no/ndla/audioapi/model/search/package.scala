/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model

import no.ndla.audioapi.model.domain.WithLanguage

package object search {

  case class LanguageValue[T](lang: String, value: T) extends WithLanguage {
    override def language: String = lang
  }
}
