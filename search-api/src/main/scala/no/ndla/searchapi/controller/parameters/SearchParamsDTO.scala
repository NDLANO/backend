/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller.parameters

import com.scalatsi.TypescriptType.{TSString, TSUndefined}
import com.scalatsi.{TSIType, TSType}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.search.SearchTrait
import no.ndla.network.tapir.NonEmptyString
import no.ndla.searchapi.model.domain.Sort
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

case class SearchParamsDTO(
    @description("The page number of the search hits to display.")
    page: Option[Int],
    @description(s"The number of search hits to display for each page.")
    pageSize: Option[Int],
    @description("A list of article-types the search should be filtered by.")
    articleTypes: Option[List[String]],
    @description(
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ["0", "initial", "start", "first"].
         |When scrolling, the parameters from the initial search is used, except in the case of 'language' and 'fallback'.
         |This value may change between scrolls. Always use the one in the latest scroll result.
         |""".stripMargin
    )
    scrollId: Option[String],
    @description("Return only results with content matching the specified query.")
    query: Option[NonEmptyString],
    @description("Fallback to existing language if language is specified.")
    fallback: Option[Boolean],
    @description("The ISO 639-1 language code describing language.")
    language: Option[String],
    @description("Return only results with provided license.")
    license: Option[String],
    @description("The sorting used on results.")
    sort: Option[Sort],
    @description("Return only learning resources that have one of the provided ids.")
    ids: Option[List[Long]],
    @description("A comma separated list of subjects the learning resources should be filtered by.")
    subjects: Option[List[String]],
    @description("Return only learning resources of specific type(s).")
    resourceTypes: Option[List[String]],
    @description("A list of context-types the learning resources should be filtered by.")
    contextTypes: Option[List[String]],
    @description(
      """A list of relevances the learning resources should be filtered by.
        |If subjects are specified the learning resource must have specified relevances in relation to a specified subject.
        |If levels are specified the learning resource must have specified relevances in relation to a specified level.""".stripMargin
    )
    relevance: Option[List[String]],
    @description("A list of ISO 639-1 language codes that the learning resource can be available in.")
    languageFilter: Option[List[String]],
    @description("A list of codes from GREP API the resources should be filtered by.")
    grepCodes: Option[List[String]],
    @description("A comma separated list of traits the resources should be filtered by.")
    traits: Option[List[SearchTrait]],
    @description("List of index-paths that should be term-aggregated and returned in result.")
    aggregatePaths: Option[List[String]],
    @description(
      "Return only results with embed data-resource the specified resource. Can specify multiple with a comma separated list to filter for one of the embed types."
    )
    embedResource: Option[List[String]],
    @description("Return only results with embed data-resource_id, data-videoid or data-url with the specified id.")
    embedId: Option[String],
    @description("Filter out inactive taxonomy contexts.")
    filterInactive: Option[Boolean]
)

object SearchParamsDTO {
  implicit val encoder: Encoder[SearchParamsDTO] = deriveEncoder
  implicit val decoder: Decoder[SearchParamsDTO] = deriveDecoder
  implicit val schema: Schema[SearchParamsDTO]   = Schema.derived[SearchParamsDTO]

  import com.scalatsi.dsl.*
  implicit val tsType: TSIType[SearchParamsDTO] =
    TSType.fromCaseClass[SearchParamsDTO] - "sort" + ("sort", TSString | TSUndefined)
}
