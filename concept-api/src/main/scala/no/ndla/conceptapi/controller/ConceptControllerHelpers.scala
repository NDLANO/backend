/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.controller

import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.domain.{ConceptType, Sort}
import no.ndla.language.Language
import sttp.tapir._
import sttp.tapir.model.{CommaSeparated, Delimited}

trait ConceptControllerHelpers {
  this: Props =>
  import props._

  object ConceptControllerHelpers {

    val pathConceptId =
      path[Long]("concept_id")
        .description("Id of the concept that is to be returned")

    val queryParam =
      query[Option[String]]("query").description("Return only concepts with content matching the specified query.")

    val conceptIds = query[CommaSeparated[Long]]("ids")
      .description(
        "Return only concepts that have one of the provided ids. To provide multiple ids, separate by comma (,)."
      )
      .default(Delimited[",", Long](List.empty))

    val aggregatePaths =
      query[CommaSeparated[String]]("aggregate-paths")
        .description("List of index-paths that should be term-aggregated and returned in result.")
        .default(Delimited[",", String](List.empty))

    val conceptType =
      query[Option[String]]("concept-type")
        .description(s"Return only concepts of given type. Allowed values are ${ConceptType.values.mkString(",")}")

    val pageNo =
      query[Int]("page")
        .description("The page number of the search hits to display.")
        .validate(Validator.min(1))
        .default(1)

    val pageSize =
      query[Int]("page-size")
        .description("The number of search hits to display for each page.")
        .validate(Validator.min(1))
        .default(props.DefaultPageSize)

    val sort =
      query[Option[String]]("sort")
        .description(
          s"""The sorting used on results.
             The following are supported: ${Sort.values.mkString(",")}
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
        )
    val language =
      query[String]("language")
        .description("The ISO 639-1 language code describing language.")
        .default(Language.AllLanguages)
    val license =
      query[Option[String]]("license")
        .description("Return only results with provided license.")
    val fallback =
      query[Boolean]("fallback")
        .description("Fallback to existing language if language is specified.")
        .default(false)
    val scrollId =
      query[Option[String]]("search-context")
        .description(
          s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
              .mkString("[", ",", "]")}.
       |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.name}' and '${this.fallback.name}'.
       |This value may change between scrolls. Always use the one in the latest scroll result.
       |If you are not paginating very far, you can ignore this and use '${this.pageNo.name}' and '${this.pageSize.name}' instead.
       |""".stripMargin
        )
    val subjects =
      query[CommaSeparated[String]]("subjects")
        .description("A comma-separated list of subjects that should appear in the search.")
        .default(Delimited[",", String](List.empty))

    val tagsToFilterBy =
      query[CommaSeparated[String]]("tags")
        .description("A comma-separated list of tags to filter the search by.")
        .default(Delimited[",", String](List.empty))

    val userFilter = query[CommaSeparated[String]]("users")
      .description(
        s"""List of users to filter by.
       |The value to search for is the user-id from Auth0.""".stripMargin
      )
      .default(Delimited[",", String](List.empty))

    val embedResource = query[CommaSeparated[String]]("embed-resource")
      .description("Return concepts with matching embed type.")
      .default(Delimited[",", String](List.empty))
    val embedId = query[Option[String]]("embed-id").description("Return concepts with matching embed id.")

    val exactTitleMatch =
      query[Boolean]("exact-match")
        .description("If provided, only return concept where query matches title exactly.")
        .default(false)
    val responsibleIdFilter =
      query[CommaSeparated[String]]("responsible-ids")
        .description("List of responsible ids to filter by (OR filter)")
        .default(Delimited[",", String](List.empty))
  }
}
