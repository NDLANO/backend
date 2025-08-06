/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

import cats.implicits.toFunctorOps
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil.deriveEncoderWithTypename
import no.ndla.common.SchemaImplicits
import no.ndla.common.TapirUtil.withDiscriminator
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{CommentDTO, ResponsibleDTO}
import sttp.tapir.Schema
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

sealed trait MultiSummaryBaseDTO

object MultiSummaryBaseDTO {
  implicit val encoder: Encoder[MultiSummaryBaseDTO] = Encoder.instance {
    case x: MultiSearchSummaryDTO => x.asJson
    case x: NodeHitDTO            => x.asJson
  }

  implicit val decoder: Decoder[MultiSummaryBaseDTO] = List[Decoder[MultiSummaryBaseDTO]](
    Decoder[MultiSearchSummaryDTO].widen,
    Decoder[NodeHitDTO].widen
  ).reduceLeft(_ or _)
}

case class NodeHitDTO(
    @description("The unique id of the taxonomy node")
    id: String,
    @description("The title of the taxonomy node")
    title: String,
    @description("The url to the frontend page of the taxonomy node")
    url: Option[String],
    @description("Subject page summary if the node is connected to a subject page")
    subjectPage: Option[SubjectPageSummaryDTO],
    @description("Primary context of the resource")
    context: Option[ApiTaxonomyContextDTO],
    @description("Contexts of the resource")
    contexts: List[ApiTaxonomyContextDTO]
) extends MultiSummaryBaseDTO

object NodeHitDTO extends SchemaImplicits {
  implicit val encoder: Encoder[NodeHitDTO] = deriveEncoderWithTypename[NodeHitDTO]
  implicit val decoder: Decoder[NodeHitDTO] = deriveDecoder
  implicit def schema: Schema[NodeHitDTO]   = {
    import sttp.tapir.generic.auto.*
    def nodeHitSchema: Schema[NodeHitDTO] = Schema.derived[NodeHitDTO]
    withDiscriminator(nodeHitSchema)
  }
}

@description("Short summary of information about the resource")
case class MultiSearchSummaryDTO(
    @description("The unique id of the resource")
    id: Long,
    @description("The title of the resource")
    title: TitleWithHtmlDTO,
    @description("The meta description of the resource")
    metaDescription: MetaDescriptionDTO,
    @description("The meta image for the resource")
    metaImage: Option[MetaImageDTO],
    @description("Url pointing to the resource")
    url: String,
    @description("Primary context of the resource")
    context: Option[ApiTaxonomyContextDTO],
    @description("Contexts of the resource")
    contexts: List[ApiTaxonomyContextDTO],
    @description("Languages the resource exists in")
    supportedLanguages: Seq[String],
    @description("Learning resource type")
    learningResourceType: LearningResourceType,
    @description("Status information of the resource")
    status: Option[StatusDTO],
    @description("Traits for the resource")
    traits: List[SearchTrait],
    @description("Relevance score. The higher the score, the better the document matches your search criteria.")
    score: Float,
    @description("List of objects describing matched field with matching words emphasized")
    highlights: List[HighlightedFieldDTO],
    @description("The taxonomy paths for the resource")
    paths: List[String],
    @description("The time and date of last update")
    lastUpdated: NDLADate,
    @description("Describes the license of the resource")
    license: Option[String],
    @description("A list of revisions planned for the article")
    revisions: Seq[RevisionMetaDTO],
    @description("Responsible field")
    responsible: Option[ResponsibleDTO],
    @description("Information about comments attached to the article")
    comments: Option[Seq[CommentDTO]],
    @description("If the article should be prioritized. Possible values are prioritized, on-hold, unspecified")
    priority: Option[String],
    @description("A combined resource type name if a standard article, otherwise the article type name")
    resourceTypeName: Option[String],
    @description("Name of the parent topic if exists")
    parentTopicName: Option[String],
    @description("Name of the primary context root if exists")
    primaryRootName: Option[String],
    @description("When the article was last published")
    published: Option[NDLADate],
    @description("Number of times favorited in MyNDLA")
    favorited: Option[Long],
    @description("Type of the resource")
    resultType: SearchType
) extends MultiSummaryBaseDTO

object MultiSearchSummaryDTO extends SchemaImplicits {
  implicit val encoder: Encoder[MultiSearchSummaryDTO] = deriveEncoderWithTypename[MultiSearchSummaryDTO]
  implicit val decoder: Decoder[MultiSearchSummaryDTO] = deriveDecoder
  implicit def schema: Schema[MultiSearchSummaryDTO]   = {
    import sttp.tapir.generic.auto.*
    def multiSearchSummary: Schema[MultiSearchSummaryDTO] = Schema.derived[MultiSearchSummaryDTO]
    withDiscriminator(multiSearchSummary)
  }
}
