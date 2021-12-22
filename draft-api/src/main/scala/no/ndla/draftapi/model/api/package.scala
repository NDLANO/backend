/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model

package object api {
  type Deletable[T] = Either[Null, Option[T]] // We use this type to make json4s understand the difference between null and missing fields
  type RelatedContent = Either[api.RelatedContentLink, Long];
}
