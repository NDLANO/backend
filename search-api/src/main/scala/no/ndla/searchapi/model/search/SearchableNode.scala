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
import no.ndla.common.model.api.search.SearchableLanguageValues
import no.ndla.common.model.taxonomy.NodeType

case class SearchableNode(
    nodeId: String,
    title: SearchableLanguageValues,
    contentUri: Option[String],
    url: Option[String],
    nodeType: NodeType,
    subjectPage: Option[SearchableSubjectPage],
    context: Option[SearchableTaxonomyContext],
    contexts: List[SearchableTaxonomyContext],
    grepContexts: List[SearchableGrepContext],
    typeName: List[String],
)

object SearchableNode {
  implicit val encoder: Encoder[SearchableNode] = deriveEncoder
  implicit val decoder: Decoder[SearchableNode] = deriveDecoder
}
