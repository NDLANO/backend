/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.article

import no.ndla.language.model.WithLanguage

case class ArticleMetaImage(imageId: String, altText: String, language: String) extends WithLanguage
