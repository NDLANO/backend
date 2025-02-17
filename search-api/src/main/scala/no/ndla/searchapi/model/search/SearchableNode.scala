/*
 * Part of NDLA search-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.search.model.SearchableLanguageValues
import no.ndla.searchapi.model.taxonomy.NodeType

case class SearchableNode(
    id: String,
    title: SearchableLanguageValues,
    contentUri: Option[String],
    nodeType: NodeType,
    frontpage: Option[Frontpage]
)

object SearchableNode {
  implicit val encoder: Encoder[SearchableNode] = deriveEncoder
  implicit val decoder: Decoder[SearchableNode] = deriveDecoder
}
