/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.common.model

package object api {
  // We use the `Deletable` type to make json4s understand the difference between null and undefined/missing fields
  type Deletable[T]   = Either[Null, Option[T]]
  type RelatedContent = Either[api.RelatedContentLinkDTO, Long]
}
