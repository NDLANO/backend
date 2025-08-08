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
import no.ndla.common.model.api.LanguageCode
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
import no.ndla.network.tapir.auth.TokenUser
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
import no.ndla.common.model.domain.Priority

trait SearchController {
  this: SearchApiClient & MultiSearchService & SearchConverterService & SearchService & MultiDraftSearchService &
    FeideApiClient & Props & ErrorHandling & TapirController & GrepSearchService & GetSearchQueryParams =>
  val searchController: SearchController

  class SearchController extends TapirController {
    import props.*

    override val serviceName: String         = "search"
    override val prefix: EndpointInput[Unit] = "search-api" / "v1" / serviceName

    private val includeMissingResourceTypeGroup = query[Boolean]("missing-group")
      .description(
        "Whether to include group without resource-types for group-search. Defaults to false."
      )
      .default(false)

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
      .serverLogicPure { userInfo => input =>
        val subjects = input.subjects.getOrElse(List.empty)
        multiDraftSearchService.aggregateSubjects(subjects, userInfo)
      }

    def groupSearch: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Search across multiple groups of learning resources")
      .description("Search across multiple groups of learning resources")
      .in("group")
      .in(GetSearchQueryParams.input)
      .in(includeMissingResourceTypeGroup)
      .in(feideHeader)
      .out(jsonBody[Seq[GroupSearchResultDTO]])
      .errorOut(errorOutputsFor(401, 403))
      .serverLogicPure { case (q, includeMissingResourceTypeGroup, feideToken) =>
        getAvailability(feideToken) match {
          case Failure(ex)           => returnLeftError(ex)
          case Success(availability) =>
            val searchParams = asSearchParamsDTO(q)
            val settings     = asSettings(searchParams.some, availability)
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
    private def scrollWithOr[T <: SearchService](scrollId: Option[String], language: LanguageCode, scroller: T)(
        orFunction: => Try[(MultiSearchResultDTO, DynamicHeaders)]
    ): Try[(MultiSearchResultDTO, DynamicHeaders)] = {
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          for {
            scrollResult <- scroller.scroll(scroll, language.code)
            body    = searchConverterService.toApiMultiSearchResult(scrollResult)
            headers = DynamicHeaders.fromMaybeValue("search-context", scrollResult.scrollId)
          } yield (body, headers)
        case _ => orFunction
      }
    }

    private def asSearchParamsDTO(queryWrapper: GetParamsWrapper): SearchParamsDTO = {
      val pagination = queryWrapper.pagination
      val q          = queryWrapper.searchParams
      val sort       = q.sort.flatMap(Sort.valueOf)

      SearchParamsDTO(
        page = pagination.page.some,
        pageSize = pagination.pageSize.some,
        articleTypes = q.articleTypes.values.some,
        scrollId = q.scrollId,
        query = q.queryParam,
        fallback = q.fallback.some,
        language = q.language.some,
        license = q.license,
        sort = sort,
        ids = q.learningResourceIds.values.some,
        subjects = q.subjects.values.some,
        resourceTypes = q.resourceTypes.values.some,
        contextTypes = q.contextTypes.values.some,
        relevance = q.relevanceFilter.values.some,
        languageFilter = q.languageFilter.values.some,
        grepCodes = q.grepCodes.values.some,
        traits = q.traits.values.flatMap(SearchTrait.valueOf).some,
        aggregatePaths = q.aggregatePaths.values.some,
        embedResource = q.embedResource.values.some,
        embedId = q.embedId,
        filterInactive = q.filterInactive.some,
        resultTypes = q.resultTypes.values.flatMap(SearchType.withNameOption).some,
        nodeTypeFilter = q.nodeTypeFilter.values.flatMap(NodeType.withNameOption).some
      )
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
        scrollWithOr(queryWrapper.searchParams.scrollId, queryWrapper.searchParams.language, multiSearchService) {
          val searchParams = asSearchParamsDTO(queryWrapper)
          getAvailability(feideToken).flatMap { availability =>
            val settings = asSettings(searchParams.some, availability)
            multiSearchService.matchingQuery(settings) match {
              case Success(searchResult) =>
                val result  = searchConverterService.toApiMultiSearchResult(searchResult)
                val headers = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
                Success((result, headers))
              case Failure(ex) => Failure(ex)
            }
          }
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
            scrollWithOr(searchParams.flatMap(_.scrollId), LanguageCode(settings.language), multiSearchService) {
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
      .serverLogicPure { userInfo =>
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
              priority = stringListParam("priority").flatMap(Priority.withNameOption).some,
              topics = stringListParam("topics").some,
              publishedDateFrom = dateParamOrNone("published-date-from"),
              publishedDateTo = dateParamOrNone("published-date-to"),
              resultTypes = stringListParam("result-types").flatMap(SearchType.withNameOption).some
            )
          )

          val settings = asDraftSettings(searchParams, userInfo)
          scrollWithOr(searchParams.flatMap(_.scrollId), LanguageCode(settings.language), multiDraftSearchService) {
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
      .serverLogicPure { userInfo => searchParams =>
        val settings = asDraftSettings(searchParams, userInfo)
        scrollWithOr(searchParams.flatMap(_.scrollId), LanguageCode(settings.language), multiDraftSearchService) {
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
        case None        => Success(List.empty)
        case Some(token) =>
          feideApiClient.getFeideExtendedUser(Some(token)) match {
            case Success(user)                      => Success(user.availabilities.toList)
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

    private def asSettings(p: Option[SearchParamsDTO], availability: List[Availability]): SearchSettings = {
      p match {
        case None         => SearchSettings.default
        case Some(params) =>
          val shouldScroll = params.scrollId.exists(InitialScrollContextKeywords.contains)
          SearchSettings(
            query = params.query,
            fallback = params.fallback.getOrElse(false),
            language = params.language.getOrElse(LanguageCode(AllLanguages)).code,
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

    private def asDraftSettings(p: Option[DraftSearchParamsDTO], user: TokenUser): MultiDraftSearchSettings = {
      p match {
        case None         => MultiDraftSearchSettings.default(user)
        case Some(params) =>
          val shouldScroll = params.scrollId.exists(InitialScrollContextKeywords.contains)
          MultiDraftSearchSettings(
            user = user,
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
            priority = params.priority.getOrElse(List.empty),
            publishedFilterFrom = params.publishedDateFrom,
            publishedFilterTo = params.publishedDateTo,
            resultTypes = params.resultTypes
          )
      }
    }
  }
}
