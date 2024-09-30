/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

import enumeratum.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.search.SearchableTaxonomyResourceType

sealed trait NodeType extends EnumEntry {}
object NodeType extends Enum[NodeType] with CirceEnum[NodeType] {
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

object Node {
  implicit val encoder: Encoder[Node] = deriveEncoder
  implicit val decoder: Decoder[Node] = Decoder.instance(c => {
    for {
      id           <- c.downField("id").as[String]
      name         <- c.downField("name").as[Option[String]].map(_.getOrElse(""))
      contentUri   <- c.downField("contentUri").as[Option[String]]
      path         <- c.downField("path").as[Option[String]]
      metadata     <- c.downField("metadata").as[Option[Metadata]]
      translations <- c.downField("translations").as[List[TaxonomyTranslation]]
      nodeType     <- c.downField("nodeType").as[NodeType]
      contexts     <- c.downField("contexts").as[List[TaxonomyContext]]
    } yield Node(id, name, contentUri, path, metadata, translations, nodeType, contexts)

  })
}

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
    isActive: Boolean,
    url: String
)

object TaxonomyContext {
  implicit val encoder: Encoder[TaxonomyContext] = deriveEncoder
  implicit val decoder: Decoder[TaxonomyContext] = deriveDecoder
}

case class TaxonomyTranslation(name: String, language: String)

object TaxonomyTranslation {
  implicit val encoder: Encoder[TaxonomyTranslation] = deriveEncoder
  implicit val decoder: Decoder[TaxonomyTranslation] = Decoder.instance(c => {
    for {
      name     <- c.downField("name").as[Option[String]].map(_.getOrElse(""))
      language <- c.downField("language").as[String]
    } yield TaxonomyTranslation(name, language)
  })
}
