/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import cats.implicits.*
import no.ndla.common.errors.AccessDeniedException
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.common.model.api.search.{LearningResourceType, MultiSearchResultDTO, SearchTrait, SearchType}
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.common.model.domain.Availability
import no.ndla.language.Language.AllLanguages
import no.ndla.network.clients.FeideApiClient
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Parameters.feideHeader
import no.ndla.network.tapir.{AllErrors, DynamicHeaders, NonEmptyString, TapirController}
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import no.ndla.searchapi.controller.parameters.{
  DraftSearchParamsDTO,
  GetSearchQueryParams,
  GrepSearchInputDTO,
  SearchParamsDTO,
  SubjectAggsInputDTO
}
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.SearchApiClient
import no.ndla.searchapi.model.api.grep.GrepSearchResultsDTO
import no.ndla.searchapi.model.api.{ErrorHandling, GroupSearchResultDTO, SubjectAggregationsDTO}
import no.ndla.searchapi.model.domain.Sort
import no.ndla.searchapi.model.search.settings.{MultiDraftSearchSettings, SearchSettings}
import no.ndla.searchapi.model.taxonomy.NodeType
import no.ndla.searchapi.service.search.{
  GrepSearchService,
  MultiDraftSearchService,
  MultiSearchService,
  SearchConverterService,
  SearchService
}
import sttp.model.QueryParams

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MINUTES
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

trait SearchController {
  this: SearchApiClient & MultiSearchService & SearchConverterService & SearchService & MultiDraftSearchService &
    FeideApiClient & Props & ErrorHandling & TapirController & GrepSearchService & GetSearchQueryParams =>
  val searchController: SearchController

  class SearchController extends TapirController {
    import props.*

    override val serviceName: String         = "search"
    override val prefix: EndpointInput[Unit] = "search-api" / "v1" / serviceName

    private val queryParam =
      query[Option[NonEmptyString]]("query")
        .description("Return only results with content matching the specified query.")
        .schema(NonEmptyString.schemaOpt)
    private val language =
      query[String]("language")
        .description("The ISO 639-1 language code describing language.")
        .default(AllLanguages)
    private val sort = query[Option[String]]("sort").description(s"""The sorting used on results.
             The following are supported: ${Sort.all.mkString(", ")}. Default is by -relevance (desc).""".stripMargin)

    private val pageNo = query[Int]("page")
      .description("The page number of the search hits to display.")
      .default(1)
      .validate(Validator.min(1))
    private val pageSize = query[Int]("page-size")
      .description(
        s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize."
      )
      .default(DefaultPageSize)
      .validate(Validator.inRange(0, MaxPageSize))
    private val learningResourceIds =
      listQuery[Long]("ids")
        .description(
          "Return only learning resources that have one of the provided ids. To provide multiple ids, separate by comma (,)."
        )
    private val fallback =
      query[Boolean]("fallback")
        .description("Fallback to existing language if language is specified.")
        .default(false)
    private val subjects =
      listQuery[String]("subjects")
        .description("A comma separated list of subjects the learning resources should be filtered by.")
    private val contextTypes =
      listQuery[String]("context-types")
        .description(
          s"A comma separated list of types the learning resources should be filtered by. Available values is ${LearningResourceType.values
              .mkString(", ")}"
        )
    private val groupTypes =
      listQuery[String]("resource-types")
        .description("A comma separated list of resource-types the learning resources should be grouped by.")
    private val languageFilter = listQuery[String]("language-filter")
      .description("A comma separated list of ISO 639-1 language codes that the learning resource can be available in.")
    private val relevanceFilter = listQuery[String]("relevance")
      .description(
        """A comma separated list of relevances the learning resources should be filtered by.
        |If subjects are specified the learning resource must have specified relevances in relation to a specified subject.
        |If levels are specified the learning resource must have specified relevances in relation to a specified level.""".stripMargin
      )
    private val includeMissingResourceTypeGroup = query[Boolean]("missing-group")
      .description(
        "Whether to include group without resource-types for group-search. Defaults to false."
      )
      .default(false)
    private val grepCodes = listQuery[String]("grep-codes")
      .description("A comma separated list of codes from GREP API the resources should be filtered by.")
    private val traits = listQuery[String]("traits")
      .description("A comma separated list of traits the resources should be filtered by.")
    private val aggregatePaths = listQuery[String]("aggregate-paths")
      .description("List of index-paths that should be term-aggregated and returned in result.")
    private val embedResource =
      listQuery[String]("embed-resource")
        .description(
          "Return only results with embed data-resource the specified resource. Can specify multiple with a comma separated list to filter for one of the embed types."
        )
    private val embedId =
      query[Option[String]]("embed-id")
        .description("Return only results with embed data-resource_id, data-videoid or data-url with the specified id.")
    private val filterInactive =
      query[Boolean]("filter-inactive").description("Filter out inactive taxonomy contexts.").default(false)

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      groupSearch,
      searchLearningResources,
      searchDraftLearningResources,
      searchDraftLearningResourcesGet,
      postSearchLearningResources,
      subjectAggs,
      searchGrep,
      getGrepReplacements
    )

    def subjectAggs: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("List subjects with aggregated data about their contents")
      .description("List subjects with aggregated data about their contents")
      .in("subjects")
      .in(jsonBody[SubjectAggsInputDTO])
      .out(jsonBody[SubjectAggregationsDTO])
      .errorOut(errorOutputsFor(400, 401, 403))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ => input =>
        val subjects = input.subjects.getOrElse(List.empty)
        multiDraftSearchService.aggregateSubjects(subjects)
      }

    def groupSearch: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Search across multiple groups of learning resources")
      .description("Search across multiple groups of learning resources")
      .in("group")
      .in(queryParam)
      .in(groupTypes)
      .in(pageNo)
      .in(pageSize)
      .in(language)
      .in(fallback)
      .in(subjects)
      .in(sort)
      .in(learningResourceIds)
      .in(contextTypes)
      .in(languageFilter)
      .in(relevanceFilter)
      .in(includeMissingResourceTypeGroup)
      .in(aggregatePaths)
      .in(grepCodes)
      .in(traits)
      .in(embedResource)
      .in(embedId)
      .in(filterInactive)
      .in(feideHeader)
      .out(jsonBody[Seq[GroupSearchResultDTO]])
      .errorOut(errorOutputsFor(401, 403))
      .serverLogicPure {
        case (
              query,
              groupTypes,
              page,
              pageSize,
              language,
              fallback,
              subjects,
              sortStr,
              learningResourceIds,
              contextTypes,
              languageFilter,
              relevanceFilter,
              includeMissingResourceTypeGroup,
              aggregatePaths,
              grepCodes,
              traits,
              embedResource,
              embedId,
              filterInactive,
              feideToken
            ) =>
          val sort = sortStr
            .flatMap(Sort.valueOf)
            .getOrElse(if (query.isDefined) Sort.ByRelevanceDesc else Sort.ByRelevanceDesc)

          getAvailability(feideToken) match {
            case Failure(ex) => returnLeftError(ex)
            case Success(availability) =>
              val settings = SearchSettings(
                query = query,
                fallback = fallback,
                language = language,
                license = Some("all"),
                page = page,
                pageSize = pageSize,
                sort = sort,
                withIdIn = learningResourceIds.values,
                subjects = subjects.values,
                resourceTypes = groupTypes.values,
                learningResourceTypes = contextTypes.values.flatMap(LearningResourceType.valueOf),
                supportedLanguages = languageFilter.values,
                relevanceIds = relevanceFilter.values,
                grepCodes = grepCodes.values,
                traits = traits.values.flatMap(SearchTrait.valueOf),
                shouldScroll = false,
                filterByNoResourceType = false,
                aggregatePaths = aggregatePaths.values,
                embedResource = embedResource.values,
                embedId = embedId,
                availability = availability,
                articleTypes = List.empty,
                filterInactive = filterInactive,
                resultTypes = None,
                nodeTypeFilter = List.empty
              )

              groupSearch(settings, includeMissingResourceTypeGroup)
          }
      }

    private def searchInGroup(group: String, settings: SearchSettings): Try[GroupSearchResultDTO] = {
      multiSearchService
        .matchingQuery(settings)
        .map(res => searchConverterService.toApiGroupMultiSearchResult(group, res))
    }

    /** Will create a separate search for each entry in [[SearchSettings.resourceTypes]] and
      * [[SearchSettings.learningResourceTypes]]
      */
    private def groupSearch(
        settings: SearchSettings,
        includeMissingResourceTypeGroup: Boolean
    ): Either[AllErrors, Seq[GroupSearchResultDTO]] = {
      val numMissingRtThreads = if (includeMissingResourceTypeGroup) 1 else 0
      val numGroups           = settings.resourceTypes.size + settings.learningResourceTypes.size + numMissingRtThreads
      if (numGroups >= 1) {
        implicit val ec: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(Math.max(numGroups, 1)))

        val rtSearches = settings.resourceTypes.map(group =>
          Future {
            searchInGroup(group, settings.copy(resourceTypes = List(group), learningResourceTypes = List.empty))
          }
        )

        val lrSearches = settings.learningResourceTypes.map(group =>
          Future {
            searchInGroup(
              group.toString,
              settings.copy(resourceTypes = List.empty, learningResourceTypes = List(group))
            )
          }
        )

        val withoutRt =
          if (includeMissingResourceTypeGroup)
            Seq(
              Future {
                searchInGroup(
                  "missing",
                  settings.copy(
                    resourceTypes = List.empty,
                    learningResourceTypes = List(LearningResourceType.Article),
                    filterByNoResourceType = true
                  )
                )
              }
            )
          else Seq.empty

        val searches = rtSearches ++ lrSearches ++ withoutRt

        val futureSearches    = Future.sequence(searches)
        val completedSearches = Await.result(futureSearches, Duration(1, MINUTES))

        val failedSearches = completedSearches.collect { case Failure(ex) => ex }
        if (failedSearches.nonEmpty) {
          returnLeftError(failedSearches.head)
        } else {
          completedSearches.collect { case Success(r) => r }.asRight
        }
      } else {
        List.empty.asRight
      }
    }

    /** Does a scroll with @scroller specified in the first parameter list If no scrollId is specified execute the
      * function @orFunction in the second parameter list.
      *
      * @param scroller
      *   SearchService to scroll with
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @tparam T
      *   SearchService
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollWithOr[T <: SearchService](scrollId: Option[String], language: String, scroller: T)(
        orFunction: => Try[(MultiSearchResultDTO, DynamicHeaders)]
    ): Try[(MultiSearchResultDTO, DynamicHeaders)] = {
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          for {
            scrollResult <- scroller.scroll(scroll, language)
            body    = searchConverterService.toApiMultiSearchResult(scrollResult)
            headers = DynamicHeaders.fromMaybeValue("search-context", scrollResult.scrollId)
          } yield (body, headers)
        case _ => orFunction
      }
    }

    def searchLearningResources: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Find learning resources")
      .description("Shows all learning resources. You can search too.")
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(jsonBody[MultiSearchResultDTO])
      .out(EndpointOutput.derived[DynamicHeaders])
      .in(GetSearchQueryParams.input)
      .in(feideHeader)
      .serverLogicPure { case (queryWrapper, feideToken) =>
        val pagination = queryWrapper.pagination
        val q          = queryWrapper.searchParams
        scrollWithOr(q.scrollId, q.language, multiSearchService) {
          val sort         = q.sort.flatMap(Sort.valueOf)
          val shouldScroll = q.scrollId.exists(InitialScrollContextKeywords.contains)
          getAvailability(feideToken).flatMap(availability => {
            val settings = SearchSettings(
              query = q.queryParam,
              fallback = q.fallback,
              language = q.language,
              license = q.license,
              page = pagination.page,
              pageSize = pagination.pageSize,
              sort = sort.getOrElse(Sort.ByRelevanceDesc),
              withIdIn = q.learningResourceIds.values,
              subjects = q.subjects.values,
              resourceTypes = q.resourceTypes.values,
              learningResourceTypes = q.contextTypes.values.flatMap(LearningResourceType.valueOf),
              supportedLanguages = q.languageFilter.values,
              relevanceIds = q.relevanceFilter.values,
              grepCodes = q.grepCodes.values,
              shouldScroll = shouldScroll,
              filterByNoResourceType = false,
              aggregatePaths = q.aggregatePaths.values,
              embedResource = q.embedResource.values,
              embedId = q.embedId,
              availability = availability,
              articleTypes = q.articleTypes.values,
              filterInactive = q.filterInactive,
              traits = q.traits.values.flatMap(SearchTrait.valueOf),
              resultTypes = q.resultTypes.values.flatMap(SearchType.withNameOption).some,
              nodeTypeFilter = q.nodeTypeFilter.values.flatMap(NodeType.withNameOption)
            )
            multiSearchService.matchingQuery(settings) match {
              case Success(searchResult) =>
                val result  = searchConverterService.toApiMultiSearchResult(searchResult)
                val headers = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
                Success((result, headers))
              case Failure(ex) => Failure(ex)
            }
          })
        }

      }

    def postSearchLearningResources: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Find learning resources")
      .description("Shows all learning resources. You can search too.")
      .errorOut(errorOutputsFor(400))
      .out(jsonBody[MultiSearchResultDTO])
      .out(EndpointOutput.derived[DynamicHeaders])
      .in(jsonBody[Option[SearchParamsDTO]].schema(SearchParamsDTO.schema.asOption))
      .in(feideHeader)
      .serverLogicPure { case (searchParams, feideToken) =>
        getAvailability(feideToken)
          .flatMap(availability => {
            val settings = asSettings(searchParams, availability)
            scrollWithOr(searchParams.flatMap(_.scrollId), settings.language, multiSearchService) {
              multiSearchService.matchingQuery(settings).map { searchResult =>
                val result  = searchConverterService.toApiMultiSearchResult(searchResult)
                val headers = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
                (result, headers)
              }
            }
          })
      }

    def intParamOrNone(name: String)(implicit queryParams: QueryParams): Option[Int] = {
      queryParams
        .get(name)
        .flatMap(str => {
          str.toIntOption
        })
    }

    def intParamOrDefault(name: String, default: => Int)(implicit queryParams: QueryParams): Int =
      intParamOrNone(name)
        .getOrElse(default)

    def stringParamOrDefault(name: String, default: => String)(implicit queryParams: QueryParams): String =
      queryParams
        .get(name)
        .getOrElse(default)

    def stringParamOrNone(name: String)(implicit queryParams: QueryParams): Option[String] =
      queryParams.get(name).filterNot(_.isEmpty)

    def stringListParam(name: String)(implicit queryParams: QueryParams): List[String] =
      queryParams
        .get(name)
        .map(_.split(",").toList)
        .getOrElse(List.empty)

    def dateParamOrNone(name: String)(implicit queryParams: QueryParams): Option[NDLADate] =
      queryParams
        .get(name)
        .flatMap(str => NDLADate.fromString(str).toOption)

    def longListParam(name: String)(implicit queryParams: QueryParams): List[Long] =
      queryParams
        .get(name)
        .map(x => x.split(",").toList.flatMap(_.toLongOption))
        .getOrElse(List.empty)

    def booleanParamOrNone(name: String)(implicit queryParams: QueryParams): Option[Boolean] =
      queryParams.get(name).flatMap(_.toBooleanOption)

    def searchDraftLearningResourcesGet: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Find draft learning resources")
      .description(
        """Shows all draft learning resources. You can search too.
          |Query parameters are undocumented, but are the same as the body for the POST endpoint, except `kebab-case`.
          |""".stripMargin
      )
      .in("editorial")
      .in(queryParams)
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(jsonBody[MultiSearchResultDTO])
      .out(EndpointOutput.derived[DynamicHeaders])
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure {
        _ =>
          { implicit queryParams =>
            val searchParams = Some(
              DraftSearchParamsDTO(
                page = intParamOrNone("page"),
                pageSize = intParamOrNone("page-size"),
                articleTypes = stringListParam("article-types").some,
                contextTypes = stringListParam("context-types").some,
                language = stringParamOrNone("language"),
                ids = longListParam("ids").some,
                resourceTypes = stringListParam("resource-types").some,
                license = stringParamOrNone("license"),
                query = NonEmptyString.fromOptString(stringParamOrNone("query")),
                noteQuery = NonEmptyString.fromOptString(stringParamOrNone("note-query")),
                sort = stringParamOrNone("sort").flatMap(Sort.valueOf),
                fallback = booleanParamOrNone("fallback"),
                subjects = stringListParam("subjects").some,
                languageFilter = stringListParam("language-filter").some,
                relevance = stringListParam("relevance").some,
                scrollId = stringParamOrNone("search-context"),
                draftStatus = stringListParam("draft-status").some,
                users = stringListParam("users").some,
                grepCodes = stringListParam("grep-codes").some,
                traits = stringListParam("traits").flatMap(SearchTrait.withNameOption).some,
                aggregatePaths = stringListParam("aggregate-paths").some,
                embedResource = stringListParam("embed-resource").some,
                embedId = stringParamOrNone("embed-id"),
                includeOtherStatuses = booleanParamOrNone("include-other-statuses"),
                revisionDateFrom = dateParamOrNone("revision-date-from"),
                revisionDateTo = dateParamOrNone("revision-date-to"),
                excludeRevisionLog = booleanParamOrNone("exclude-revision-log"),
                responsibleIds = stringListParam("responsible-ids").some,
                filterInactive = booleanParamOrNone("filter-inactive"),
                prioritized = booleanParamOrNone("prioritized"),
                priority = stringListParam("priority").some,
                topics = stringListParam("topics").some,
                publishedDateFrom = dateParamOrNone("published-date-from"),
                publishedDateTo = dateParamOrNone("published-date-to"),
                resultTypes = stringListParam("result-types").flatMap(SearchType.withNameOption).some
              )
            )

            val settings = asDraftSettings(searchParams)
            scrollWithOr(searchParams.flatMap(_.scrollId), settings.language, multiDraftSearchService) {
              multiDraftSearchService.matchingQuery(settings).map { searchResult =>
                val result  = searchConverterService.toApiMultiSearchResult(searchResult)
                val headers = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
                (result, headers)
              }
            }
          }
      }

    def searchDraftLearningResources: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Find draft learning resources")
      .description("Shows all draft learning resources. You can search too.")
      .in("editorial")
      .in(jsonBody[Option[DraftSearchParamsDTO]].schema(DraftSearchParamsDTO.schema.asOption))
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(jsonBody[MultiSearchResultDTO])
      .out(EndpointOutput.derived[DynamicHeaders])
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ => searchParams =>
        val settings = asDraftSettings(searchParams)
        scrollWithOr(searchParams.flatMap(_.scrollId), settings.language, multiDraftSearchService) {
          multiDraftSearchService.matchingQuery(settings).map { searchResult =>
            val result  = searchConverterService.toApiMultiSearchResult(searchResult)
            val headers = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
            (result, headers)
          }
        }
      }

    def searchGrep: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Search for grep codes")
      .description("Search for grep codes")
      .in("grep")
      .in(jsonBody[GrepSearchInputDTO])
      .out(jsonBody[GrepSearchResultsDTO])
      .errorOut(errorOutputsFor(400, 401, 403))
      .serverLogicPure { input => grepSearchService.searchGreps(input) }

    def getGrepReplacements: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get grep replacements")
      .in("grep" / "replacements")
      .in(
        listQuery[String]("codes").description(
          "Grep codes to find replacements for. To provide codes ids, separate by comma (,)."
        )
      )
      .errorOut(errorOutputsFor(400))
      .out(jsonBody[Map[String, String]])
      .serverLogicPure { input =>
        grepSearchService.getReplacements(input.values)
      }

    /** This method fetches availability based on FEIDE access token in the request This does an actual api-call to the
      * feide api and should be used sparingly.
      */
    private def getAvailability(feideToken: Option[String]): Try[List[Availability]] = {
      feideToken match {
        case None => Success(List.empty)
        case Some(token) =>
          feideApiClient.getFeideExtendedUser(Some(token)) match {
            case Success(user) => Success(user.availabilities.toList)
            case Failure(ex: AccessDeniedException) =>
              logger.info(
                s"Access denied when fetching user from feide with accessToken '$token': ${ex.getMessage}",
                ex
              )
              Success(List.empty)
            case Failure(ex) =>
              logger.error(s"Error when fetching user from feide with accessToken '$token': ${ex.getMessage}", ex)
              Failure(ex)
          }
      }
    }

    def asSettings(p: Option[SearchParamsDTO], availability: List[Availability]): SearchSettings = {
      p match {
        case None => SearchSettings.default
        case Some(params) =>
          val shouldScroll = params.scrollId.exists(InitialScrollContextKeywords.contains)
          SearchSettings(
            query = params.query,
            fallback = params.fallback.getOrElse(false),
            language = params.language.getOrElse(AllLanguages),
            license = params.license,
            page = params.page.getOrElse(1),
            pageSize = params.pageSize.getOrElse(10),
            sort = params.sort.getOrElse(Sort.ByRelevanceDesc),
            withIdIn = params.ids.getOrElse(List.empty),
            subjects = params.subjects.getOrElse(List.empty),
            resourceTypes = params.resourceTypes.getOrElse(List.empty),
            learningResourceTypes = params.contextTypes.getOrElse(List.empty).flatMap(LearningResourceType.valueOf),
            supportedLanguages = params.languageFilter.getOrElse(List.empty),
            relevanceIds = params.relevance.getOrElse(List.empty),
            grepCodes = params.grepCodes.getOrElse(List.empty),
            traits = params.traits.getOrElse(List.empty),
            shouldScroll = shouldScroll,
            filterByNoResourceType = false,
            aggregatePaths = params.aggregatePaths.getOrElse(List.empty),
            embedResource = params.embedResource.getOrElse(List.empty),
            embedId = params.embedId,
            availability = availability,
            articleTypes = params.articleTypes.getOrElse(List.empty),
            filterInactive = params.filterInactive.getOrElse(false),
            resultTypes = params.resultTypes,
            nodeTypeFilter = params.nodeTypeFilter.getOrElse(List.empty)
          )

      }

    }

    def asDraftSettings(p: Option[DraftSearchParamsDTO]): MultiDraftSearchSettings = {
      p match {
        case None => MultiDraftSearchSettings.default
        case Some(params) =>
          val shouldScroll = params.scrollId.exists(InitialScrollContextKeywords.contains)
          MultiDraftSearchSettings(
            query = params.query,
            noteQuery = params.noteQuery,
            fallback = params.fallback.getOrElse(false),
            language = params.language.getOrElse(AllLanguages),
            license = params.license,
            page = params.page.getOrElse(1),
            pageSize = params.pageSize.getOrElse(10),
            sort = params.sort.getOrElse(Sort.ByRelevanceDesc),
            withIdIn = params.ids.getOrElse(List.empty),
            subjects = params.subjects.getOrElse(List.empty),
            topics = params.topics.getOrElse(List.empty),
            resourceTypes = params.resourceTypes.getOrElse(List.empty),
            learningResourceTypes = params.contextTypes.getOrElse(List.empty).flatMap(LearningResourceType.valueOf),
            supportedLanguages = params.languageFilter.getOrElse(List.empty),
            relevanceIds = params.relevance.getOrElse(List.empty),
            statusFilter = params.draftStatus.getOrElse(List.empty).flatMap(DraftStatus.valueOf),
            userFilter = params.users.getOrElse(List.empty),
            grepCodes = params.grepCodes.getOrElse(List.empty),
            traits = params.traits.getOrElse(List.empty),
            shouldScroll = shouldScroll,
            searchDecompounded = false,
            aggregatePaths = params.aggregatePaths.getOrElse(List.empty),
            embedResource = params.embedResource.getOrElse(List.empty),
            embedId = params.embedId,
            includeOtherStatuses = params.includeOtherStatuses.getOrElse(false),
            revisionDateFilterFrom = params.revisionDateFrom,
            revisionDateFilterTo = params.revisionDateTo,
            excludeRevisionHistory = params.excludeRevisionLog.getOrElse(false),
            responsibleIdFilter = params.responsibleIds.getOrElse(List.empty),
            articleTypes = params.articleTypes.getOrElse(List.empty),
            filterInactive = params.filterInactive.getOrElse(false),
            prioritized = params.prioritized,
            priority = params.priority.getOrElse(List.empty),
            publishedFilterFrom = params.publishedDateFrom,
            publishedFilterTo = params.publishedDateTo,
            resultTypes = params.resultTypes
          )
      }
    }
  }
}
