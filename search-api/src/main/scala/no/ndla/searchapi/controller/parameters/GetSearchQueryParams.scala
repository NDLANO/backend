/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller.parameters

import no.ndla.common.model.api.CommaSeparatedList.CommaSeparatedList
import no.ndla.common.model.api.LanguageCode
import no.ndla.language.Language
import no.ndla.network.tapir.NonEmptyString
import no.ndla.searchapi.Props
import sttp.tapir.*
import sttp.tapir.EndpointIO.annotations.query
import sttp.tapir.Schema.annotations.{default, description}
import sttp.tapir.ValidationResult.{Invalid, Valid}
import no.ndla.common.DeriveHelpers

class GetSearchQueryParams(using props: Props) {

  case class PaginationParams(
      @query("page")
      @description("The page number of the search hits to display.")
      @default(1)
      page: Int,
      @query("page-size")
      @description("The number of search hits to display for each page.")
      @default(props.DefaultPageSize)
      pageSize: Int,
  )

  case class GetParamsWrapper(pagination: PaginationParams, searchParams: SearchQueryParams)

  case class SearchQueryParams(
      @query("article-types")
      @description("A comma separated list of article-types the search should be filtered by.")
      articleTypes: CommaSeparatedList[String],
      @query("context-types")
      @description("A comma separated list of types the learning resources should be filtered by.")
      contextTypes: CommaSeparatedList[String],
      @query("language")
      @description("The ISO 639-1 language code describing language.")
      @default(LanguageCode(Language.AllLanguages))
      language: LanguageCode,
      @query("ids")
      @description(
        "Return only learning resources that have one of the provided ids. To provide multiple ids, separate by comma (,)."
      )
      learningResourceIds: CommaSeparatedList[Long],
      @query("resource-types")
      @description(
        "Return only learning resources with specific taxonomy type(s), e.g. 'urn:resourcetype:learningpath'. To provide multiple types, separate by comma (,)."
      )
      resourceTypes: CommaSeparatedList[String],
      @query("license")
      @description("Return only results with provided license.")
      license: Option[String],
      @query("query")
      @description("Return only results with content matching the specified query.")
      @default(None)
      queryParam: Option[NonEmptyString],
      @query("sort")
      @description("Sort the search results by the specified field.")
      sort: Option[String],
      @query("fallback")
      @default(false)
      @description("Fallback to existing language if language is specified.")
      fallback: Boolean,
      @query("subjects")
      @description("A comma separated list of subjects the learning resources should be filtered by.")
      subjects: CommaSeparatedList[String],
      @query("language-filter")
      @description("A comma separated list of ISO 639-1 language codes that the learning resource can be available in.")
      languageFilter: CommaSeparatedList[String],
      @query("relevance")
      @description(
        "A comma separated list of relevances the learning resources should be filtered by. If subjects are specified the learning resource must have specified relevances in relation to a specified subject. If levels are specified the learning resource must have specified relevances in relation to a specified level."
      )
      relevanceFilter: CommaSeparatedList[String],
      @query("search-context")
      @description("A unique string obtained from a search you want to keep scrolling in.")
      scrollId: Option[String],
      @query("grep-codes")
      @description("A comma separated list of codes from GREP API the resources should be filtered by.")
      grepCodes: CommaSeparatedList[String],
      @query("aggregate-paths")
      @description("List of index-paths that should be term-aggregated and returned in result.")
      aggregatePaths: CommaSeparatedList[String],
      @query("embed-resource")
      @description(
        "Return only results with embed data-resource the specified resource. Can specify multiple with a comma separated list to filter for one of the embed types."
      )
      embedResource: CommaSeparatedList[String],
      @query("embed-id")
      @description("Return only results with embed data-resource_id, data-videoid or data-url with the specified id.")
      embedId: Option[String],
      @query("filter-inactive")
      @description("Filter out inactive taxonomy contexts.")
      @default(false)
      filterInactive: Boolean,
      @query("traits")
      @description("A comma separated list of traits the resources should be filtered by.")
      traits: CommaSeparatedList[String],
      @query("result-types")
      @description("A comma separated list of indexes that should be searched.")
      resultTypes: CommaSeparatedList[String],
      @query("node-types")
      @description("A comma separated list of node types the nodes should be filtered by.")
      nodeTypeFilter: CommaSeparatedList[String],
      @query("tags")
      @description("Only return results that have one of the specified tags.")
      tags: CommaSeparatedList[String],
  )

  object SearchQueryParams {
    implicit val schema: Schema[SearchQueryParams]            = DeriveHelpers.getSchema[SearchQueryParams]
    implicit val schemaOpt: Schema[Option[SearchQueryParams]] = schema.asOption

    def input2 = EndpointInput.derived[SearchQueryParams]
    def input1 = EndpointInput
      .derived[PaginationParams]
      .validate {
        Validator.custom {
          case q if q.page < 1                                       => Invalid("page must be greater than 0")
          case q if q.pageSize < 1 || q.pageSize > props.MaxPageSize =>
            Invalid(s"page-size must be between 1 and ${props.MaxPageSize}")
          case _ => Valid
        }
      }
    def input = input1.and(input2).mapTo[GetParamsWrapper]

  }
}
