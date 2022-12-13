/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model

case class RelatedContentLink(title: String, url: String)

package object domain {
  type RelatedContent = Either[RelatedContentLink, Long]
}
