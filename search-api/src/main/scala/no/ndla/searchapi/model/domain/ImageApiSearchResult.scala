/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class ImageTitle(title: String, language: String)
case class ImageAltText(alttext: String, language: String)
case class ImageApiSearchResult(id: String,
                                title: ImageTitle,
                                altText: ImageAltText,
                                previewUrl: String,
                                metaUrl: String,
                                license: String,
                                supportedLanguages: Seq[String])
