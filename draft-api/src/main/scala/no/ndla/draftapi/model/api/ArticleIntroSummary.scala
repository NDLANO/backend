package no.ndla.draftapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

@description("Summary of the article")
case class ArticleIntroSummary(
    @description("The summary content") summary: String,
    @description(
      "The ISO 639-1 language code describing which article translation this summary belongs to"
    ) language: String
)

object ArticleIntroSummary {
  implicit val encoder: Encoder[ArticleIntroSummary] = deriveEncoder
  implicit val decoder: Decoder[ArticleIntroSummary] = deriveDecoder
}
