/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.ResourceType

/** Data to pass between search-api and myndla-api for indexing */
case class MyNDLABundleDTO(
    favorites: Map[String, Map[String, Long]]
) {

  def getFavorites(id: String, resourceType: ResourceType): Long = {
    favorites.getOrElse(resourceType.entryName, Map.empty).getOrElse(id, 0L)
  }

  def getFavorites(id: String, resourceType: List[ResourceType]): Long = {
    resourceType
      .map(rt => favorites.getOrElse(rt.entryName, Map.empty).getOrElse(id, 0L))
      .foldLeft(0L) { case (acc, cur) => acc + cur }
  }
}

object MyNDLABundleDTO {
  implicit val encoder: Encoder[MyNDLABundleDTO] = deriveEncoder
  implicit val decoder: Decoder[MyNDLABundleDTO] = deriveDecoder
}

case class FavoriteEntryDTO(
    id: String,
    resourceType: String
)

object FavoriteEntryDTO {
  implicit val encoder: Encoder[FavoriteEntryDTO] = deriveEncoder
  implicit val decoder: Decoder[FavoriteEntryDTO] = deriveDecoder
}
