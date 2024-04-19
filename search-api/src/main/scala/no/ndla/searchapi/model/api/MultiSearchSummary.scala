/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.draft.Comment
import no.ndla.searchapi.model.search.SearchType
import sttp.tapir.Schema.annotations.description

@description("Object describing matched field with matching words emphasized")
case class HighlightedField(
    @description("Field that matched") field: String,
    @description("List of segments that matched in `field`") matches: Seq[String]
)

object HighlightedField {
  implicit val encoder: Encoder[HighlightedField] = deriveEncoder
  implicit val decoder: Decoder[HighlightedField] = deriveDecoder
}

// format: off
@description("Short summary of information about the resource")
case class MultiSearchSummary(
    @description("The unique id of the resource") id: Long,
    @description("The title of the resource") title: Title,
    @description("The meta description of the resource") metaDescription: MetaDescription,
    @description("The meta image for the resource") metaImage: Option[MetaImage],
    @description("Url pointing to the resource") url: String,
    @description("Contexts of the resource") contexts: List[ApiTaxonomyContext],
    @description("Languages the resource exists in") supportedLanguages: Seq[String],
    @description("Learning resource type, either 'standard', 'topic-article', 'learningpath', 'concept' or 'gloss'") learningResourceType: String,
    @description("Status information of the resource") status: Option[Status],
    @description("Traits for the resource") traits: List[String],
    @description("Relevance score. The higher the score, the better the document matches your search criteria.") score: Float,
    @description("List of objects describing matched field with matching words emphasized") highlights: List[HighlightedField],
    @description("The taxonomy paths for the resource") paths: List[String],
    @description("The time and date of last update") lastUpdated: NDLADate,
    @description("Describes the license of the resource") license: Option[String],
    @description("A list of revisions planned for the article") revisions: Seq[RevisionMeta],
    @description("Responsible field") responsible: Option[DraftResponsible],
    @description("Information about comments attached to the article") comments: Option[Seq[Comment]],
    @description("If the article should be prioritized" ) prioritized: Option[Boolean],
    @description("If the article should be prioritized. Possible values are prioritized, on-hold, unspecified") priority: Option[String],
    @description("A combined resource type name if a standard article, otherwise the article type name") resourceTypeName: Option[String],
    @description("Name of the parent topic if exists") parentTopicName: Option[String],
    @description("Name of the primary context root if exists") primaryRootName: Option[String],
    @description("When the article was last published") published: Option[NDLADate],
    @description("Number of times favorited in MyNDLA") favorited: Option[Long],
    @description("Type of the resource") resultType: SearchType
)
// format: on

object MultiSearchSummary {
  implicit val encoder: Encoder[MultiSearchSummary] = deriveEncoder
  implicit val decoder: Decoder[MultiSearchSummary] = deriveDecoder
}
