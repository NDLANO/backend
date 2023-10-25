/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

import enumeratum._
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.search.SearchableTaxonomyResourceType

sealed trait NodeType extends EnumEntry {}
object NodeType extends Enum[NodeType] {
  case object NODE      extends NodeType
  case object SUBJECT   extends NodeType
  case object TOPIC     extends NodeType
  case object RESOURCE  extends NodeType
  case object PROGRAMME extends NodeType

  val values: IndexedSeq[NodeType] = findValues
}

case class Node(
    id: String,
    name: String,
    contentUri: Option[String],
    path: Option[String],
    metadata: Option[Metadata],
    translations: List[TaxonomyTranslation],
    nodeType: NodeType,
    var contexts: List[TaxonomyContext]
)

case class TaxonomyContext(
    publicId: String,
    rootId: String,
    root: SearchableLanguageValues,
    path: String,
    breadcrumbs: SearchableLanguageList,
    contextType: Option[String],
    relevanceId: String,
    relevance: SearchableLanguageValues,
    resourceTypes: List[SearchableTaxonomyResourceType],
    parentIds: List[String],
    isPrimary: Boolean,
    contextId: String,
    isVisible: Boolean,
    isActive: Boolean
)

case class TaxonomyTranslation(name: String, language: String)
