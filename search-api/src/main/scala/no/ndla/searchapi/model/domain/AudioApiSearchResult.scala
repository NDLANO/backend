/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class AudioApiTitle(title: String, language: String)
case class AudioApiSearchResult(id: Long,
                                title: AudioApiTitle,
                                url: String,
                                license: String,
                                supportedLanguages: Seq[String])
