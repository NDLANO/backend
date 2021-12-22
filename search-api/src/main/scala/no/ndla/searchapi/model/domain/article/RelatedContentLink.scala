/*
 * Part of NDLA search-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.article

case class RelatedContentLink(title: String, url: String)

object RelatedContentLink {
  type RelatedContent = Either[RelatedContentLink, Long]
}
