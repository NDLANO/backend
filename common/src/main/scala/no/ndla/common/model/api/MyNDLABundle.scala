/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/** Data to pass between search-api and myndla-api for indexing */
case class MyNDLABundle(
    favorites: Map[String, Map[String, Long]]
) {

  def getFavorites(id: String, resourceType: String): Long = {
    favorites.getOrElse(resourceType, Map.empty).getOrElse(id, 0L)
  }

  def getFavorites(id: String, resourceType: List[String]): Long = {
    val favs = resourceType
      .map(rt => {
        favorites.getOrElse(rt, Map.empty).getOrElse(id, 0L)
      })
    favs.foldLeft(0L) { case (acc, cur) => acc + cur }
  }
}

object MyNDLABundle {
  implicit val encoder: Encoder[MyNDLABundle] = deriveEncoder
  implicit val decoder: Decoder[MyNDLABundle] = deriveDecoder
}

case class FavoriteEntry(
    id: String,
    resourceType: String
)

object FavoriteEntry {
  implicit val encoder: Encoder[FavoriteEntry] = deriveEncoder
  implicit val decoder: Decoder[FavoriteEntry] = deriveDecoder
}
