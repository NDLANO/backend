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
import no.ndla.common.model.api.draft.CommentDTO
import no.ndla.searchapi.model.domain.LearningResourceType
import no.ndla.searchapi.model.search.SearchType
import sttp.tapir.Schema.annotations.description

@description("Object describing matched field with matching words emphasized")
case class HighlightedFieldDTO(
    @description("Field that matched") field: String,
    @description("List of segments that matched in `field`") matches: Seq[String]
)

object HighlightedFieldDTO {
  implicit val encoder: Encoder[HighlightedFieldDTO] = deriveEncoder
  implicit val decoder: Decoder[HighlightedFieldDTO] = deriveDecoder
}

// format: off
@description("Short summary of information about the resource")
case class MultiSearchSummaryDTO(
                                  @description("The unique id of the resource") id: Long,
                                  @description("The title of the resource") title: TitleWithHtmlDTO,
                                  @description("The meta description of the resource") metaDescription: MetaDescriptionDTO,
                                  @description("The meta image for the resource") metaImage: Option[MetaImageDTO],
                                  @description("Url pointing to the resource") url: String,
                                  @description("Contexts of the resource") contexts: List[ApiTaxonomyContextDTO],
                                  @description("Languages the resource exists in") supportedLanguages: Seq[String],
                                  @description("Learning resource type") learningResourceType: LearningResourceType,
                                  @description("Status information of the resource") status: Option[StatusDTO],
                                  @description("Traits for the resource") traits: List[String],
                                  @description("Relevance score. The higher the score, the better the document matches your search criteria.") score: Float,
                                  @description("List of objects describing matched field with matching words emphasized") highlights: List[HighlightedFieldDTO],
                                  @description("The taxonomy paths for the resource") paths: List[String],
                                  @description("The time and date of last update") lastUpdated: NDLADate,
                                  @description("Describes the license of the resource") license: Option[String],
                                  @description("A list of revisions planned for the article") revisions: Seq[RevisionMetaDTO],
                                  @description("Responsible field") responsible: Option[DraftResponsibleDTO],
                                  @description("Information about comments attached to the article") comments: Option[Seq[CommentDTO]],
                                  @description("If the article should be prioritized" ) prioritized: Option[Boolean],
                                  @description("If the article should be prioritized. Possible values are prioritized, on-hold, unspecified") priority: Option[String],
                                  @description("A combined resource type name if a standard article, otherwise the article type name") resourceTypeName: Option[String],
                                  @description("Name of the parent topic if exists") parentTopicName: Option[String],
                                  @description("Name of the primary context root if exists") primaryRootName: Option[String],
                                  @description("When the article was last published") published: Option[NDLADate],
                                  @description("Number of times favorited in MyNDLA") favorited: Option[Long],
                                  @description("Type of the resource") resultType: SearchType,
                                  @description("Subject ids for the resource, if a concept") conceptSubjectIds: Option[List[String]]
)
// format: on

object MultiSearchSummaryDTO {
  implicit val encoder: Encoder[MultiSearchSummaryDTO] = deriveEncoder
  implicit val decoder: Decoder[MultiSearchSummaryDTO] = deriveDecoder
}
