/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import enumeratum.Json4s
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.License
import no.ndla.common.model.domain.ArticleType
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api._
import no.ndla.draftapi.model.domain.{SearchSettings, Sort}
import no.ndla.draftapi.service.search.{ArticleSearchService, SearchConverterService}
import no.ndla.draftapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.language.Language
import no.ndla.mapping
import no.ndla.mapping.LicenseDefinition
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{ResponseMessage, Swagger}
import org.scalatra.{Created, NotFound, Ok}

import scala.util.{Failure, Success, Try}

trait DraftController {
  this: ReadService
    with WriteService
    with ArticleSearchService
    with SearchConverterService
    with ConverterService
    with ContentValidator
    with NdlaController
    with Props
    with ErrorHelpers =>
  val draftController: DraftController

  class DraftController(implicit val swagger: Swagger) extends NdlaController {
    import props.{DefaultPageSize, InitialScrollContextKeywords}

    protected implicit override val jsonFormats: Formats =
      DefaultFormats.withLong +
        Json4s.serializer(PartialArticleFields) ++
        JavaTimeSerializers.all +
        NDLADate.Json4sSerializer

    protected val applicationDescription = "API for accessing draft articles."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val query =
      Param[Option[String]]("query", "Return only articles with content matching the specified query.")
    private val optionalArticleId =
      Param[Option[Long]]("articleId", description = "The ID of the article to generate a status state machine for")
    private val articleId = Param[Long]("article_id", "Id of the article that is to be fetched")
    private val nodeId    = Param[String]("node_id", "Id of the taxonomy node to process")
    private val articleTypes = Param[Option[String]](
      "articleTypes",
      "Return only articles of specific type(s). To provide multiple types, separate by comma (,)."
    )
    private val articleIds = Param[Option[String]](
      "ids",
      "Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,)."
    )
    private val filter    = Param[Option[String]]("filter", "A filter to include a specific entry")
    private val filterNot = Param[Option[String]]("filterNot", "A filter to remove a specific entry")
    private val statuss   = Param[String]("STATUS", "An article status")
    private val copiedTitleFlag =
      Param[Option[String]](
        "copied-title-postfix",
        "Add a string to the title marking this article as a copy, defaults to 'true'."
      )
    private val grepCodes = Param[Option[Seq[String]]](
      "grep-codes",
      "A comma separated list of codes from GREP API the resources should be filtered by."
    )
    private val articleSlug = Param[String]("slug", "Slug of the article that is to be fecthed.")

    /** Does a scroll with [[ArticleSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(scrollId: Option[String], language: String)(orFunction: => Any): Any = {
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          articleSearchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
              Ok(searchConverterService.asApiSearchResult(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
          }
        case _ => orFunction
      }
    }

    get(
      "/tag-search/",
      operation(
        apiOperation[TagsSearchResult]("getTags-paginated")
          .summary("Retrieves a list of all previously used tags in articles")
          .description("Retrieves a list of all previously used tags in articles")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(pageSize),
            asQueryParam(pageNo),
            asQueryParam(language)
          )
          .responseMessages(response403, response500)
          .authorizations("oauth2")
      )
    ) {
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        val query = paramOrDefault(this.query.paramName, "")
        val pageSize = intOrDefault(this.pageSize.paramName, DefaultPageSize) match {
          case tooSmall if tooSmall < 1 => DefaultPageSize
          case x                        => x
        }
        val pageNo = intOrDefault(this.pageNo.paramName, 1) match {
          case tooSmall if tooSmall < 1 => 1
          case x                        => x
        }
        val language = paramOrDefault(this.language.paramName, Language.AllLanguages)

        readService.getAllTags(query, pageSize, pageNo, language) match {
          case Failure(ex)     => errorHandler(ex)
          case Success(result) => Ok(result)
        }
      }
    }: Unit

    private def search(
        query: Option[String],
        sort: Option[Sort],
        language: String,
        license: Option[String],
        page: Int,
        pageSize: Int,
        idList: List[Long],
        articleTypesFilter: Seq[String],
        fallback: Boolean,
        grepCodes: Seq[String],
        shouldScroll: Boolean
    ) = {
      val searchSettings = query match {
        case Some(q) =>
          SearchSettings(
            query = Some(q),
            withIdIn = idList,
            searchLanguage = language,
            license = license,
            page = page,
            pageSize = if (idList.isEmpty) pageSize else idList.size,
            sort = sort.getOrElse(Sort.ByRelevanceDesc),
            if (articleTypesFilter.isEmpty) ArticleType.all else articleTypesFilter,
            fallback = fallback,
            grepCodes = grepCodes,
            shouldScroll = shouldScroll
          )
        case None =>
          SearchSettings(
            query = None,
            withIdIn = idList,
            searchLanguage = language,
            license = license,
            page = page,
            pageSize = if (idList.isEmpty) pageSize else idList.size,
            sort = sort.getOrElse(Sort.ByTitleAsc),
            if (articleTypesFilter.isEmpty) ArticleType.all else articleTypesFilter,
            fallback = fallback,
            grepCodes = grepCodes,
            shouldScroll = shouldScroll
          )
      }

      articleSearchService.matchingQuery(searchSettings) match {
        case Success(searchResult) =>
          val responseHeader = searchResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
          Ok(searchConverterService.asApiSearchResult(searchResult), headers = responseHeader)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/grep-codes/",
      operation(
        apiOperation[GrepCodesSearchResult]("getGrepCodes")
          .summary("Retrieves a list of all previously used grepCodes in articles")
          .description("Retrieves a list of all previously used grepCodes in articles")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(pageSize),
            asQueryParam(pageNo)
          )
          .responseMessages(response403, response500)
          .authorizations("oauth2")
      )
    ) {
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        val query = paramOrDefault(this.query.paramName, "")
        val pageSize = intOrDefault(this.pageSize.paramName, DefaultPageSize) match {
          case tooSmall if tooSmall < 1 => DefaultPageSize
          case x                        => x
        }
        val pageNo = intOrDefault(this.pageNo.paramName, 1) match {
          case tooSmall if tooSmall < 1 => 1
          case x                        => x
        }

        readService.getAllGrepCodes(query, pageSize, pageNo)
      }
    }: Unit

    get(
      "/",
      operation(
        apiOperation[List[SearchResult]]("getAllArticles")
          .summary("Show all articles")
          .description("Shows all articles. You can search it too.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(articleTypes),
            asQueryParam(query),
            asQueryParam(articleIds),
            asQueryParam(language),
            asQueryParam(license),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(sort),
            asQueryParam(scrollId),
            asQueryParam(grepCodes)
          )
          .authorizations("oauth2")
          .responseMessages(response500)
      )
    ) {
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
        val scrollId = paramOrNone(this.scrollId.paramName)

        scrollSearchOr(scrollId, language) {
          val query              = paramOrNone(this.query.paramName)
          val sort               = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
          val license            = paramOrNone(this.license.paramName)
          val pageSize           = intOrDefault(this.pageSize.paramName, DefaultPageSize)
          val page               = intOrDefault(this.pageNo.paramName, 1)
          val idList             = paramAsListOfLong(this.articleIds.paramName)
          val articleTypesFilter = paramAsListOfString(this.articleTypes.paramName)
          val fallback           = booleanOrDefault(this.fallback.paramName, default = false)
          val grepCodes          = paramAsListOfString(this.grepCodes.paramName)
          val shouldScroll       = paramOrNone(this.scrollId.paramName).exists(InitialScrollContextKeywords.contains)

          search(
            query,
            sort,
            language,
            license,
            page,
            pageSize,
            idList,
            articleTypesFilter,
            fallback,
            grepCodes,
            shouldScroll
          )
        }
      }
    }: Unit

    post(
      "/search/",
      operation(
        apiOperation[List[SearchResult]]("getAllArticlesPost")
          .summary("Show all articles")
          .description("Shows all articles. You can search it too.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(language),
            bodyParam[ArticleSearchParams],
            asQueryParam(scrollId)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response500)
      )
    ) {
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        tryExtract[ArticleSearchParams](request.body) match {
          case Success(searchParams) =>
            val language = searchParams.language.getOrElse(Language.AllLanguages)
            scrollSearchOr(searchParams.scrollId, language) {
              val query              = searchParams.query
              val sort               = Sort.valueOf(searchParams.sort.getOrElse(""))
              val license            = searchParams.license
              val pageSize           = searchParams.pageSize.getOrElse(DefaultPageSize)
              val page               = searchParams.page.getOrElse(1)
              val idList             = searchParams.idList
              val articleTypesFilter = searchParams.articleTypes
              val fallback           = searchParams.fallback.getOrElse(false)
              val grepCodes          = searchParams.grepCodes
              val shouldScroll       = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)

              search(
                query,
                sort,
                language,
                license,
                page,
                pageSize,
                idList,
                articleTypesFilter,
                fallback,
                grepCodes,
                shouldScroll
              )
            }
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit

    get(
      "/:article_id",
      operation(
        apiOperation[Article]("getArticleById")
          .summary("Show article with a specified Id")
          .description("Shows the article for the specified id.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(articleId),
            asQueryParam(language),
            asQueryParam(fallback)
          )
          .authorizations("oauth2")
          .responseMessages(response404, response500)
      )
    ) {
      val articleId = long(this.articleId.paramName)
      val language  = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback  = booleanOrDefault(this.fallback.paramName, default = false)

      val article        = readService.withId(articleId, language, fallback)
      val currentOption  = article.map(_.status.current).toOption
      val isPublicStatus = currentOption.contains(DraftStatus.EXTERNAL_REVIEW.toString)
      requirePermissionOrAccessDenied(DRAFT_API_WRITE, isPublicStatus) {
        article match {
          case Success(a)  => a
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit

    get(
      "/ids/",
      operation(
        apiOperation[List[Article]]("getArticlesByIds")
          .summary("Fetch articles that matches ids parameter.")
          .description("Returns articles that matches ids parameter.")
          .parameters(
            asQueryParam(articleIds),
            asQueryParam(fallback),
            asQueryParam(language),
            asQueryParam(pageSize),
            asQueryParam(pageNo)
          )
          .responseMessages(response400, response403, response500)
      )
    ) {
      val idList   = paramAsListOfLong(this.articleIds.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val pageSize = intOrDefault(this.pageSize.paramName, props.DefaultPageSize) match {
        case tooSmall if tooSmall < 1 => props.DefaultPageSize
        case x                        => x
      }
      val page = intOrDefault(this.pageNo.paramName, 1) match {
        case tooSmall if tooSmall < 1 => 1
        case x                        => x
      }
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        readService.getArticlesByIds(idList, language, fallback, page.toLong, pageSize.toLong)
      }
    }: Unit

    get(
      "/:article_id/history",
      operation(
        apiOperation[Article]("getHistoricArticleById")
          .summary("Get all saved articles with a specified Id, latest revision first")
          .description(
            "Retrieves all current and previously published articles with the specified id, latest revision first."
          )
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(articleId),
            asQueryParam(language),
            asQueryParam(fallback)
          )
          .authorizations("oauth2")
          .responseMessages(response404, response500)
      )
    ) {
      val articleId = long(this.articleId.paramName)
      val language  = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback  = booleanOrDefault(this.fallback.paramName, default = false)

      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        readService.getArticles(articleId, language, fallback)
      }
    }: Unit

    get(
      "/external_id/:deprecated_node_id",
      operation(
        apiOperation[ContentId]("getInternalIdByExternalId")
          .summary("Get internal id of article for a specified ndla_node_id")
          .description("Get internal id of article for a specified ndla_node_id")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(deprecatedNodeId)
          )
          .authorizations("oauth2")
          .responseMessages(response404, response500)
      )
    ) {
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        val externalId = long(this.deprecatedNodeId.paramName)
        readService.getInternalArticleIdByExternalId(externalId) match {
          case Some(id) => id
          case None     => NotFound(body = Error(ErrorHelpers.NOT_FOUND, s"No article with id $externalId"))
        }
      }
    }: Unit

    get(
      "/licenses/",
      operation(
        apiOperation[List[License]]("getLicenses")
          .summary("Show all valid licenses")
          .description("Shows all valid licenses")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(filter),
            asQueryParam(filterNot)
          )
          .responseMessages(response403, response500)
          .authorizations("oauth2")
      )
    ) {
      val filterNot = paramOrNone(this.filterNot.paramName)
      val filter    = paramOrNone(this.filter.paramName)

      val licenses: Seq[LicenseDefinition] = mapping.License.getLicenses
        .filter {
          case license: LicenseDefinition if filter.isDefined => license.license.toString.contains(filter.get)
          case _                                              => true
        }
        .filterNot {
          case license: LicenseDefinition if filterNot.isDefined => license.license.toString.contains(filterNot.get)
          case _                                                 => false
        }

      licenses.map(x => License(x.license.toString, Option(x.description), x.url))
    }: Unit

    post(
      "/",
      operation(
        apiOperation[Article]("newArticle")
          .summary("Create a new article")
          .description("Creates a new article")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[NewArticle]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(DRAFT_API_WRITE) { userInfo =>
        val externalId         = paramAsListOfString("externalId")
        val oldNdlaCreatedDate = paramOrNone("oldNdlaCreatedDate").flatMap(NDLADate.fromString(_).toOption)
        val oldNdlaUpdatedDate = paramOrNone("oldNdlaUpdatedDate").flatMap(NDLADate.fromString(_).toOption)
        val externalSubjectids = paramAsListOfString("externalSubjectIds")
        val importId           = paramOrNone("importId")
        tryExtract[NewArticle](request.body).flatMap(
          writeService
            .newArticle(_, externalId, externalSubjectids, userInfo, oldNdlaCreatedDate, oldNdlaUpdatedDate, importId)
        ) match {
          case Success(article)   => Created(body = article)
          case Failure(exception) => errorHandler(exception)
        }
      }
    }: Unit

    patch(
      "/:article_id",
      operation(
        apiOperation[Article]("updateArticle")
          .summary("Update an existing article")
          .description("Update an existing article")
          .parameters(
            asHeaderParam[Option[String]](correlationId),
            asPathParam[Long](articleId),
            bodyParam[UpdatedArticle]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(DRAFT_API_WRITE) { userInfo =>
        val externalId          = paramAsListOfString("externalId")
        val externalSubjectIds  = paramAsListOfString("externalSubjectIds")
        val oldNdlaCreateddDate = paramOrNone("oldNdlaCreatedDate").flatMap(NDLADate.fromString(_).toOption)
        val oldNdlaUpdatedDate  = paramOrNone("oldNdlaUpdatedDate").flatMap(NDLADate.fromString(_).toOption)
        val importId            = paramOrNone("importId")
        val id                  = long(this.articleId.paramName)
        val updateArticle: Try[UpdatedArticle] = tryExtract[UpdatedArticle](request.body)

        updateArticle.flatMap(
          writeService.updateArticle(
            id,
            _,
            externalId,
            externalSubjectIds,
            userInfo,
            oldNdlaCreateddDate,
            oldNdlaUpdatedDate,
            importId
          )
        ) match {
          case Success(article)   => Ok(body = article)
          case Failure(exception) => errorHandler(exception)
        }
      }
    }: Unit

    put(
      "/:article_id/status/:STATUS",
      operation(
        apiOperation[Article]("updateArticleStatus")
          .summary("Update status of an article")
          .description("Update status of an article")
          .parameters(
            asPathParam(articleId),
            asPathParam(statuss)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(DRAFT_API_WRITE) { userInfo =>
        val id         = long(this.articleId.paramName)
        val isImported = booleanOrDefault("import_publish", default = false)
        DraftStatus
          .valueOfOrError(params(this.statuss.paramName))
          .flatMap(writeService.updateArticleStatus(_, id, userInfo, isImported)) match {
          case Success(a)  => a
          case Failure(ex) => errorHandler(ex)
        }

      }
    }: Unit

    put(
      "/:article_id/validate/",
      operation(
        apiOperation[ContentId]("validateArticle")
          .summary("Validate an article")
          .description("Validate an article")
          .parameters(
            asHeaderParam[Option[String]](correlationId),
            asPathParam[Long](articleId),
            bodyParam[Option[UpdatedArticle]]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(DRAFT_API_WRITE) { user =>
        val importValidate = booleanOrDefault("import_validate", default = false)
        val updateArticle  = tryExtract[UpdatedArticle](request.body)

        val validationMessage = updateArticle match {
          case Success(art) =>
            contentValidator.validateArticleApiArticle(long(this.articleId.paramName), art, importValidate, user)
          case Failure(_) if request.body.isEmpty =>
            contentValidator.validateArticleApiArticle(long(this.articleId.paramName), importValidate, user)
          case Failure(ex) => Failure(ex)
        }

        validationMessage match {
          case Success(x)  => x
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit

    delete(
      "/:article_id/language/:language",
      operation(
        apiOperation[Article]("deleteLanguage")
          .summary("Delete language from article")
          .description("Delete language from article")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(articleId),
            asPathParam(pathLanguage)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(DRAFT_API_WRITE) { userInfo =>
        val id       = long(this.articleId.paramName)
        val language = params(this.language.paramName)
        writeService.deleteLanguage(id, language, userInfo)
      }
    }: Unit

    get(
      "/status-state-machine/",
      operation(
        apiOperation[Map[String, List[StateMachineStatus]]]("getStatusStateMachine")
          .summary("Get status state machine")
          .description("Get status state machine")
          .parameters(
            asQueryParam(optionalArticleId)
          )
          .authorizations("oauth2")
          .responseMessages(response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(DRAFT_API_WRITE) { user =>
        val id = longOrNone(this.optionalArticleId.paramName)
        converterService.stateTransitionsToApi(user, id) match {
          case Success(transitions) => Ok(transitions)
          case Failure(ex)          => errorHandler(ex)
        }
      }
    }: Unit

    post(
      "/clone/:article_id",
      operation(
        apiOperation[Article]("cloneArticle")
          .summary("Create a new article with the content of the article with the specified id")
          .description("Create a new article with the content of the article with the specified id")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(articleId),
            asQueryParam(language),
            asQueryParam(copiedTitleFlag),
            asQueryParam(fallback)
          )
          .authorizations("oauth2")
          .responseMessages(response404, response500)
      )
    ) {
      val articleId          = long(this.articleId.paramName)
      val language           = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback           = booleanOrDefault(this.fallback.paramName, default = false)
      val copiedTitlePostfix = booleanOrDefault(this.copiedTitleFlag.paramName, default = true)

      requirePermissionOrAccessDeniedWithUser(DRAFT_API_WRITE) { userInfo =>
        writeService.copyArticleFromId(articleId, userInfo, language, fallback, copiedTitlePostfix) match {
          case Success(article) => article
          case Failure(ex)      => errorHandler(ex)
        }
      }
    }: Unit

    post(
      "/partial-publish/:article_id",
      operation(
        apiOperation[Article]("partialPublish")
          .summary("Partial publish selected fields")
          .description("Partial publish selected fields")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(articleId),
            asQueryParam(language),
            asQueryParam(fallback),
            bodyParam[Seq[String]]
          )
          .authorizations("oauth2")
          .responseMessages(response404, response500)
      )
    ) {
      val articleId = long(this.articleId.paramName)
      val language  = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback  = booleanOrDefault(this.fallback.paramName, default = false)

      requirePermissionOrAccessDeniedWithUser(DRAFT_API_WRITE) { user =>
        tryExtract[Seq[PartialArticleFields]](request.body) match {
          case Failure(ex) => errorHandler(ex)
          case Success(articleFieldsToUpdate) =>
            writeService.partialPublishAndConvertToApiArticle(
              articleId,
              articleFieldsToUpdate,
              language,
              fallback,
              user
            ) match {
              case Success(article) => Ok(article)
              case Failure(ex)      => errorHandler(ex)
            }
        }

      }
    }: Unit

    post(
      "/partial-publish/",
      operation(
        apiOperation[MultiPartialPublishResult]("partialPublishMultiple")
          .summary("Partial publish selected fields for multiple articles")
          .description("Partial publish selected fields for multiple articles")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(language),
            bodyParam[Seq[String]]
          )
          .authorizations("oauth2")
          .responseMessages(response404, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(DRAFT_API_WRITE) { user =>
        val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
        tryExtract[PartialBulkArticles](request.body) match {
          case Failure(ex) => errorHandler(ex)
          case Success(partialBulk) =>
            writeService.partialPublishMultiple(language, partialBulk, user) match {
              case Success(response) => Ok(response)
              case Failure(ex)       => errorHandler(ex)
            }
        }
      }
    }: Unit

    post(
      "/copyRevisionDates/:node_id",
      operation(
        apiOperation[Unit]("copyRevisionDates")
          .summary("Copy revision dates from the node with this id to _all_ children in taxonomy")
          .description("Copy revision dates from the node with this id to _all_ children in taxonomy")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(nodeId)
          )
          .authorizations("oauth2")
          .responseMessages(response404, response500)
      )
    ) {
      val nodeId = paramOrNone(this.nodeId.paramName)

      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        nodeId match {
          case None => NotFound(body = Error(ErrorHelpers.NOT_FOUND, s"No nodeid supplied"))
          case Some(publicId) =>
            writeService.copyRevisionDates(publicId) match {
              case Success(_)  => Ok()
              case Failure(ex) => errorHandler(ex)
            }
        }
      }
    }: Unit

    get(
      "/slug/:slug",
      operation(
        apiOperation[Article]("getArticleBySlug")
          .summary("Show article with a specified slug")
          .description("Shows the article for the specified slug.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(articleSlug),
            asQueryParam(language),
            asQueryParam(fallback)
          )
          .authorizations("oauth2")
          .responseMessages(response404, response500)
      )
    ) {
      val slug     = params(this.articleSlug.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      val article        = readService.getArticleBySlug(slug, language, fallback)
      val currentOption  = article.map(_.status.current).toOption
      val isPublicStatus = currentOption.contains(DraftStatus.EXTERNAL_REVIEW.toString)
      requirePermissionOrAccessDenied(DRAFT_API_WRITE, isPublicStatus) {
        article match {
          case Success(a)  => a
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit

  }
}
