/*
 * Part of NDLA article-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model

package object api {
  type RelatedContent = Either[api.RelatedContentLink, Long];
}
