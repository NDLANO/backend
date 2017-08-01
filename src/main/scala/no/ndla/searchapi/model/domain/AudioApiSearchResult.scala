/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class AudioApiSearchResult(id: Long,
                                title: String,
                                url: String,
                                license: String,
                                supportedLanguages: Seq[String])
