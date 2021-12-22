/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MINUTES
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.SearchApiProperties.{
  DefaultPageSize,
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive,
  InitialScrollContextKeywords,
  MaxPageSize
}
import no.ndla.searchapi.auth.{Role, User, UserInfo}
import no.ndla.searchapi.integration.{FeideApiClient, SearchApiClient}
import no.ndla.searchapi.model.api.{
  AccessDeniedException,
  Error,
  FeideApiException,
  GroupSearchResult,
  MultiSearchResult,
  SearchResults,
  ValidationError
}
import no.ndla.searchapi.model.domain.article.{Availability, LearningResourceType}
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.{Language, SearchParams, Sort}
import no.ndla.searchapi.model.search.settings.{MultiDraftSearchSettings, SearchSettings}
import no.ndla.searchapi.service.search.{
  MultiDraftSearchService,
  MultiSearchService,
  SearchConverterService,
  SearchService
}
import no.ndla.searchapi.service.{ApiSearchService, SearchClients}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.Ok
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.scalatra.util.NotNothing

import javax.servlet.http.HttpServletRequest
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

trait SearchController {
  this: ApiSearchService
    with SearchClients
    with SearchApiClient
    with MultiSearchService
    with SearchConverterService
    with SearchService
    with MultiDraftSearchService
    with User
    with FeideApiClient =>
  val searchController: SearchController

  class SearchController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    protected val applicationDescription = "API for searching across NDLA APIs"

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400: ResponseMessage = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403: ResponseMessage = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404: ResponseMessage = ResponseMessage(404, "Not found", Some("Error"))
    val response500: ResponseMessage = ResponseMessage(500, "Unknown error", Some("Error"))

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query = Param[Option[String]]("query", "Return only results with content matching the specified query.")
    private val noteQuery =
      Param[Option[String]]("note-query", "Return only results with notes matching the specified note-query.")
    private val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val license = Param[Option[String]]("license", "Return only results with provided license.")
    private val sort = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.values.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and id (asc) when query is empty.""".stripMargin
    )

    private val pageNo = Param[Option[Int]]("page", "The page number of the search hits to display.")
    private val pageSize = Param[Option[Int]](
      "page-size",
      s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize.")
    private val resourceTypes = Param[Option[Seq[String]]](
      "resource-types",
      "Return only learning resources of specific type(s). To provide multiple types, separate by comma (,).")
    private val learningResourceIds = Param[Option[Seq[String]]](
      "ids",
      "Return only learning resources that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val apiTypes =
      Param[Option[Seq[String]]]("types", "A comma separated list of types to search in. f.ex articles,images")
    private val fallback = Param[Option[Boolean]]("fallback", "Fallback to existing language if language is specified.")
    private val levels =
      Param[Option[Seq[String]]]("levels",
                                 "A comma separated list of levels the learning resources should be filtered by.")
    private val subjects =
      Param[Option[Seq[String]]]("subjects",
                                 "A comma separated list of subjects the learning resources should be filtered by.")
    private val topics =
      Param[Option[Seq[String]]](
        "topics",
        "A comma separated list of parent topics the learning resources should be filtered by.")
    private val contextTypes =
      Param[Option[Seq[String]]](
        "context-types",
        "A comma separated list of context-types the learning resources should be filtered by.")
    private val groupTypes =
      Param[Option[Seq[String]]](
        "resource-types",
        "A comma separated list of resource-types the learning resources should be grouped by.")
    private val languageFilter = Param[Option[Seq[String]]](
      "language-filter",
      "A comma separated list of ISO 639-1 language codes that the learning resource can be available in.")
    private val relevanceFilter = Param[Option[Seq[String]]](
      "relevance",
      """A comma separated list of relevances the learning resources should be filtered by.
        |If subjects are specified the learning resource must have specified relevances in relation to a specified subject.
        |If levels are specified the learning resource must have specified relevances in relation to a specified level.""".stripMargin
    )
    private val contextFilters = Param[Option[Seq[String]]](
      "context-filters",
      """A comma separated list of resource-types the learning resources should be filtered by.
        |Used in conjunction with the parameter resource-types to filter additional resource-types.
      """.stripMargin
    )

    private val includeMissingResourceTypeGroup = Param[Option[Boolean]](
      "missing-group",
      "Whether to include group without resource-types for group-search. Defaults to false.")

    private val grepCodes = Param[Option[Seq[String]]](
      "grep-codes",
      "A comma separated list of codes from GREP API the resources should be filtered by.")

    private val scrollId = Param[Option[String]](
      "search-context",
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
           .mkString("[", ",", "]")}.
          |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.paramName}' and '${this.fallback.paramName}'.
          |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
          |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.paramName}' and '${this.pageSize.paramName}' instead.
          |""".stripMargin
    )

    private val statusFilter = Param[Option[Seq[String]]](
      "draft-status",
      s"""List of statuses to filter by.
         |A draft only needs to have one of the available statuses to be included in result (OR).
         |Supported values are ${ArticleStatus.values.mkString(", ")}.""".stripMargin
    )

    private val includeOtherStatuses =
      Param[Option[Boolean]](
        "include-other-statuses",
        s"Whether or not to include the 'other' status field when filtering with '${statusFilter.paramName}' param.")

    private val userFilter = Param[Option[Seq[String]]](
      "users",
      s"""List of users to filter by.
         |The value to search for is the user-id from Auth0.
         |UpdatedBy on article and user in editorial-notes are searched.""".stripMargin
    )

    private val aggregatePaths = Param[Option[Seq[String]]](
      "aggregate-paths",
      "List of index-paths that should be term-aggregated and returned in result."
    )

    private val embedResource =
      Param[Option[String]](
        "embed-resource",
        "Return only results with embed data-resource the specified resource. Can specify multiple with a comma separated list to filter for one of the embed types."
      )

    private val embedId =
      Param[Option[String]](
        "embed-id",
        "Return only results with embed data-resource_id, data-videoid or data-url with the specified id.")

    private val feideToken = Param[Option[String]]("FeideAuthorization", "Header containing FEIDE access token.")

    private def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
      queryParam[T](param.paramName).description(param.description)

    private def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
      headerParam[T](param.paramName).description(param.description)

    private def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
      pathParam[T](param.paramName).description(param.description)

    get(
      "/group/",
      operation(
        apiOperation[Seq[GroupSearchResult]]("groupSearch")
          .summary("Search across multiple groups of learning resources")
          .description("Search across multiple groups of learning resources")
          .parameters(
            asHeaderParam(correlationId),
            asHeaderParam(feideToken),
            asQueryParam(query),
            asQueryParam(groupTypes),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(language),
            asQueryParam(fallback),
            asQueryParam(subjects),
            asQueryParam(sort),
            asQueryParam(learningResourceIds),
            asQueryParam(levels),
            asQueryParam(contextTypes),
            asQueryParam(languageFilter),
            asQueryParam(relevanceFilter),
            asQueryParam(contextFilters),
            asQueryParam(includeMissingResourceTypeGroup),
            asQueryParam(aggregatePaths),
            asQueryParam(grepCodes),
            asQueryParam(embedResource),
            asQueryParam(embedId)
          )
          .responseMessages(response500))
    ) {
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val resourceTypes = paramAsListOfString(this.groupTypes.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val query = paramOrNone(this.query.paramName)
      val subjects = paramAsListOfString(this.subjects.paramName)
      val sort = Sort
        .valueOf(paramOrDefault(this.sort.paramName, ""))
        .getOrElse(if (query.isDefined) Sort.ByRelevanceDesc else Sort.ByIdAsc)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val taxonomyFilters = paramAsListOfString(this.levels.paramName)
      val contextTypes = paramAsListOfString(this.contextTypes.paramName)
      val supportedLanguagesFilter = paramAsListOfString(this.languageFilter.paramName)
      val relevances = paramAsListOfString(this.relevanceFilter.paramName)
      val anotherResourceTypes = paramAsListOfString(this.contextFilters.paramName)
      val grepCodes = paramAsListOfString(this.grepCodes.paramName)
      val includeMissingResourceTypeGroup =
        booleanOrDefault(this.includeMissingResourceTypeGroup.paramName, default = false)
      val aggregatePaths = paramAsListOfString(this.aggregatePaths.paramName)
      val embedResource = paramAsListOfString(this.embedResource.paramName)
      val embedId = paramOrNone(this.embedId.paramName)

      getAvailability() match {
        case Failure(ex) => errorHandler(ex)
        case Success(availability) =>
          val settings = SearchSettings(
            query = query,
            fallback = fallback,
            language = language,
            license = None,
            page = page,
            pageSize = pageSize,
            sort = sort,
            withIdIn = idList,
            subjects = subjects,
            resourceTypes = resourceTypes ++ anotherResourceTypes,
            learningResourceTypes = contextTypes.flatMap(LearningResourceType.valueOf),
            supportedLanguages = supportedLanguagesFilter,
            relevanceIds = relevances,
            grepCodes = grepCodes,
            shouldScroll = false,
            filterByNoResourceType = false,
            aggregatePaths = aggregatePaths,
            embedResource = embedResource,
            embedId = embedId,
            availability = availability
          )

          groupSearch(settings, includeMissingResourceTypeGroup)
      }
    }

    private def searchInGroup(group: String, settings: SearchSettings): Try[GroupSearchResult] = {
      multiSearchService
        .matchingQuery(settings)
        .map(res => searchConverterService.toApiGroupMultiSearchResult(group, res))
    }

    /** Will create a separate search for each entry in [[SearchSettings.resourceTypes]] and [[SearchSettings.learningResourceTypes]] */
    private def groupSearch(settings: SearchSettings, includeMissingResourceTypeGroup: Boolean) = {
      val numMissingRtThreads = if (includeMissingResourceTypeGroup) 1 else 0
      val numGroups = settings.resourceTypes.size + settings.learningResourceTypes.size + numMissingRtThreads
      if (numGroups >= 1) {
        implicit val ec: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(Math.max(numGroups, 1)))

        val rtSearches = settings.resourceTypes.map(group =>
          Future {
            searchInGroup(group, settings.copy(resourceTypes = List(group), learningResourceTypes = List.empty))
        })

        val lrSearches = settings.learningResourceTypes.map(group =>
          Future {
            searchInGroup(group.toString,
                          settings.copy(resourceTypes = List.empty, learningResourceTypes = List(group)))
        })

        val withoutRt =
          if (includeMissingResourceTypeGroup)
            Seq(
              Future {
                searchInGroup("missing",
                              settings.copy(
                                resourceTypes = List.empty,
                                learningResourceTypes = List(LearningResourceType.Article),
                                filterByNoResourceType = true
                              ))
              }
            )
          else Seq.empty

        val searches = rtSearches ++ lrSearches ++ withoutRt

        val futureSearches = Future.sequence(searches)
        val completedSearches = Await.result(futureSearches, Duration(1, MINUTES))

        val failedSearches = completedSearches.collect { case Failure(ex) => ex }
        if (failedSearches.nonEmpty) {
          errorHandler(failedSearches.head)
        } else {
          completedSearches.collect { case Success(r) => r }
        }
      } else {
        List.empty
      }
    }

    get(
      "/draft/",
      operation(
        apiOperation[Seq[SearchResults]]("searchAPIs")
          .summary("search across APIs")
          .description("search across APIs")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(language),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(apiTypes),
            asQueryParam(sort)
          )
          .responseMessages(response500))
    ) {
      val language = paramOrNone(this.language.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, "-relevance")).getOrElse(Sort.ByRelevanceDesc)
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, 5)
      val apisToSearch: Set[SearchApiClient] =
        paramAsListOfString(this.apiTypes.paramName).flatMap(SearchClients.get).toSet match {
          case apiClients if apiClients.nonEmpty => apiClients
          case apiClients if apiClients.isEmpty  => SearchClients.values.toSet
        }

      val usedKeys =
        Set(this.language.paramName, this.pageNo.paramName, this.pageSize.paramName, this.apiTypes.paramName)
      val remainingParams = params(request).toMap.view.filterKeys(key => !usedKeys.contains(key)).toMap

      searchService.search(SearchParams(language, sort, page, pageSize, remainingParams), apisToSearch)
    }

    private def getSearchSettingsFromRequest: Try[SearchSettings] = {
      val query = paramOrNone(this.query.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val contextTypes = paramAsListOfString(this.contextTypes.paramName)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val resourceTypes = paramAsListOfString(this.resourceTypes.paramName)
      val taxonomyFilters = paramAsListOfString(this.levels.paramName)
      val license = paramOrNone(this.license.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val subjects = paramAsListOfString(this.subjects.paramName)
      val supportedLanguagesFilter = paramAsListOfString(this.languageFilter.paramName)
      val relevances = paramAsListOfString(this.relevanceFilter.paramName)
      val anotherResourceTypes = paramAsListOfString(this.contextFilters.paramName)
      val grepCodes = paramAsListOfString(this.grepCodes.paramName)
      val shouldScroll = paramOrNone(this.scrollId.paramName).exists(InitialScrollContextKeywords.contains)
      val aggregatePaths = paramAsListOfString(this.aggregatePaths.paramName)
      val embedResource = paramAsListOfString(this.embedResource.paramName)
      val embedId = paramOrNone(this.embedId.paramName)

      getAvailability().map(
        availability =>
          SearchSettings(
            query = query,
            fallback = fallback,
            language = language,
            license = license,
            page = page,
            pageSize = pageSize,
            sort = sort.getOrElse(if (query.isDefined) Sort.ByRelevanceDesc else Sort.ByIdAsc),
            withIdIn = idList,
            subjects = subjects,
            resourceTypes = resourceTypes ++ anotherResourceTypes,
            learningResourceTypes = contextTypes.flatMap(LearningResourceType.valueOf),
            supportedLanguages = supportedLanguagesFilter,
            relevanceIds = relevances,
            grepCodes = grepCodes,
            shouldScroll = shouldScroll,
            filterByNoResourceType = false,
            aggregatePaths = aggregatePaths,
            embedResource = embedResource,
            embedId = embedId,
            availability = availability
        ))
    }

    private def getDraftSearchSettingsFromRequest = {
      val page = intOrDefault(this.pageNo.paramName, 1)
      val pageSize = intOrDefault(this.pageSize.paramName, SearchApiProperties.DefaultPageSize)
      val contextTypes = paramAsListOfString(this.contextTypes.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val idList = paramAsListOfLong(this.learningResourceIds.paramName)
      val resourceTypes = paramAsListOfString(this.resourceTypes.paramName)
      val taxonomyFilters = paramAsListOfString(this.levels.paramName)
      val license = paramOrNone(this.license.paramName)
      val query = paramOrNone(this.query.paramName)
      val noteQuery = paramOrNone(this.noteQuery.paramName)
      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val subjects = paramAsListOfString(this.subjects.paramName)
      val topics = paramAsListOfString(this.topics.paramName)
      val supportedLanguagesFilter = paramAsListOfString(this.languageFilter.paramName)
      val relevances = paramAsListOfString(this.relevanceFilter.paramName)
      val statusFilter = paramAsListOfString(this.statusFilter.paramName)
      val userFilter = paramAsListOfString(this.userFilter.paramName)
      val grepCodes = paramAsListOfString(this.grepCodes.paramName)
      val shouldScroll = paramOrNone(this.scrollId.paramName).exists(InitialScrollContextKeywords.contains)
      val aggregatePaths = paramAsListOfString(this.aggregatePaths.paramName)
      val embedResource = paramAsListOfString(this.embedResource.paramName)
      val embedId = paramOrNone(this.embedId.paramName)
      val includeOtherStatuses = booleanOrDefault(this.includeOtherStatuses.paramName, default = false)

      MultiDraftSearchSettings(
        query = query,
        noteQuery = noteQuery,
        fallback = fallback,
        language = language,
        license = license,
        page = page,
        pageSize = pageSize,
        sort = sort.getOrElse(if (query.isDefined) Sort.ByRelevanceDesc else Sort.ByIdAsc),
        withIdIn = idList,
        subjects = subjects,
        topics = topics,
        resourceTypes = resourceTypes,
        learningResourceTypes = contextTypes.flatMap(LearningResourceType.valueOf),
        supportedLanguages = supportedLanguagesFilter,
        relevanceIds = relevances,
        statusFilter = statusFilter.flatMap(ArticleStatus.valueOf),
        userFilter = userFilter,
        grepCodes = grepCodes,
        shouldScroll = shouldScroll,
        searchDecompounded = false,
        aggregatePaths = aggregatePaths,
        embedResource = embedResource,
        embedId = embedId,
        includeOtherStatuses = includeOtherStatuses
      )
    }

    /**
      * Does a scroll with @scroller specified in the first parameter list
      * If no scrollId is specified execute the function @orFunction in the second parameter list.
      *
      * @param scroller SearchService to scroll with
      * @param orFunction Function to execute if no scrollId in parameters (Usually searching)
      * @tparam T SearchService
      * @return A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollWithOr[T <: SearchService](scroller: T)(orFunction: => Any): Any = {

      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      paramOrNone(this.scrollId.paramName) match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          scroller.scroll(scroll, language, fallback) match {
            case Success(scrollResult) =>
              Ok(searchConverterService.toApiMultiSearchResult(scrollResult),
                 headers = scrollIdToHeader(scrollResult.scrollId))
            case Failure(ex) => errorHandler(ex)
          }
        case _ => orFunction
      }
    }

    private def scrollIdToHeader(id: Option[String]) = id.map(i => this.scrollId.paramName -> i).toList.toMap

    get(
      "/",
      operation(
        apiOperation[MultiSearchResult]("searchLearningResources")
          .summary("Find learning resources")
          .description("Shows all learning resources. You can search too.")
          .parameters(
            asHeaderParam(correlationId),
            asHeaderParam(feideToken),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(contextTypes),
            asQueryParam(language),
            asQueryParam(learningResourceIds),
            asQueryParam(resourceTypes),
            asQueryParam(levels),
            asQueryParam(license),
            asQueryParam(query),
            asQueryParam(sort),
            asQueryParam(fallback),
            asQueryParam(subjects),
            asQueryParam(languageFilter),
            asQueryParam(relevanceFilter),
            asQueryParam(contextFilters),
            asQueryParam(scrollId),
            asQueryParam(grepCodes),
            asQueryParam(aggregatePaths),
            asQueryParam(embedResource),
            asQueryParam(embedId)
          )
          .responseMessages(response500))
    ) {
      scrollWithOr(multiSearchService) {
        getSearchSettingsFromRequest match {
          case Failure(ex) => errorHandler(ex)
          case Success(settings) =>
            multiSearchService.matchingQuery(settings) match {
              case Success(searchResult) =>
                Ok(searchConverterService.toApiMultiSearchResult(searchResult), scrollIdToHeader(searchResult.scrollId))
              case Failure(ex) =>
                errorHandler(ex)
            }
        }
      }
    }

    get(
      "/editorial/",
      operation(
        apiOperation[MultiSearchResult]("searchDraftLearningResources")
          .summary("Find draft learning resources")
          .description("Shows all draft learning resources. You can search too.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(contextTypes),
            asQueryParam(language),
            asQueryParam(learningResourceIds),
            asQueryParam(resourceTypes),
            asQueryParam(levels),
            asQueryParam(license),
            asQueryParam(query),
            asQueryParam(noteQuery),
            asQueryParam(sort),
            asQueryParam(fallback),
            asQueryParam(subjects),
            asQueryParam(languageFilter),
            asQueryParam(relevanceFilter),
            asQueryParam(scrollId),
            asQueryParam(statusFilter),
            asQueryParam(userFilter),
            asQueryParam(grepCodes),
            asQueryParam(aggregatePaths),
            asQueryParam(embedResource),
            asQueryParam(embedId),
            asQueryParam(includeOtherStatuses)
          )
          .authorizations("oauth2")
          .responseMessages(response500))
    ) {
      if (!user.getUser.roles.contains(Role.DRAFTWRITE)) {
        errorHandler(AccessDeniedException("You do not have access to the requested resource."))
      } else {
        scrollWithOr(multiDraftSearchService) {
          multiDraftSearchService.matchingQuery(getDraftSearchSettingsFromRequest) match {
            case Success(searchResult) =>
              Ok(searchConverterService.toApiMultiSearchResult(searchResult), scrollIdToHeader(searchResult.scrollId))
            case Failure(ex) =>
              errorHandler(ex)
          }
        }
      }
    }

    /**
      * This method fetches availability based on FEIDE access token in the request
      * This does an actual api-call to the feide api and should be used sparingly.
      */
    private def getAvailability()(implicit request: HttpServletRequest): Try[List[Availability.Value]] = {
      requestFeideToken match {
        case None => Success(List.empty)
        case Some(token) =>
          feideApiClient.getUser(token) match {
            case Success(user) => Success(user.availabilities)
            case Failure(ex) =>
              logger.error(s"Error when fetching user from feide with accessToken '$token'")
              Failure(ex)
          }
      }
    }

    private def requestFeideToken(implicit request: HttpServletRequest): Option[String] = {
      request.header(this.feideToken.paramName).map(_.replaceFirst("Bearer ", ""))
    }

  }
}
