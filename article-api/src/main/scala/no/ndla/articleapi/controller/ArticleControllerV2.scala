/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.Props
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api._
import no.ndla.articleapi.model.domain.Sort
import no.ndla.articleapi.service.search.{ArticleSearchService, SearchConverterService}
import no.ndla.articleapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.common.ContentURIUtil.parseArticleIdAndRevision
import no.ndla.common.scalatra.NdlaSwaggerSupport
import no.ndla.language.Language.AllLanguages
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{ResponseMessage, Swagger}
import org.scalatra.{NotFound, Ok}

import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success}

trait ArticleControllerV2 {
  this: ReadService
    with WriteService
    with ArticleSearchService
    with SearchConverterService
    with ConverterService
    with ContentValidator
    with Props
    with ErrorHelpers
    with NdlaController
    with NdlaSwaggerSupport =>
  val articleControllerV2: ArticleControllerV2

  import props._

  class ArticleControllerV2(implicit val swagger: Swagger) extends NdlaController with NdlaSwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats.withLong ++ JavaTimeSerializers.all
    protected val applicationDescription                 = "Services for accessing articles from NDLA."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query =
      Param[Option[String]]("query", "Return only articles with content matching the specified query.")
    private val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val license = Param[Option[String]](
      "license",
      "Return only results with provided license. Specifying 'all' gives all articles regardless of licence."
    )
    private val sort = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.all.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and id (asc) when query is empty.""".stripMargin
    )
    private val pageNo    = Param[Option[Int]]("page", "The page number of the search hits to display.")
    private val pageSize  = Param[Option[Int]]("page-size", "The number of search hits to display for each page.")
    private val articleId = Param[Long]("article_id", "Id of the article that is to be fecthed.")
    private val revision =
      Param[Option[Int]]("revision", "Revision of article to fetch. If not provided the current revision is returned.")
    private val articleTypes = Param[Option[String]](
      "articleTypes",
      "Return only articles of specific type(s). To provide multiple types, separate by comma (,)."
    )
    private val articleIds = Param[Option[Seq[Long]]](
      "ids",
      "Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,)."
    )
    private val deprecatedNodeId = Param[String]("deprecated_node_id", "Id of deprecated NDLA node")
    private val fallback = Param[Option[Boolean]]("fallback", "Fallback to existing language if language is specified.")
    protected val scrollId = Param[Option[String]](
      "search-context",
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.paramName}' and '${this.fallback.paramName}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.paramName}' and '${this.pageSize.paramName}' instead.
         |""".stripMargin
    )
    private val grepCodes = Param[Option[Seq[String]]](
      "grep-codes",
      "A comma separated list of codes from GREP API the resources should be filtered by."
    )

    private val feideToken  = Param[Option[String]]("FeideAuthorization", "Header containing FEIDE access token.")
    private val articleSlug = Param[String]("slug", "Slug of the article that is to be fecthed.")

    /** Does a scroll with [[ArticleSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(scrollId: Option[String], language: String, fallback: Boolean)(
        orFunction: => Any
    ): Any = {
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          articleSearchService.scroll(scroll, language, fallback) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
              Ok(searchConverterService.asApiSearchResultV2(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
          }
        case _ => orFunction
      }
    }

    private def requestFeideToken(implicit request: HttpServletRequest): Option[String] = {
      request.header(this.feideToken.paramName).map(_.replaceFirst("Bearer ", ""))
    }

    get(
      "/tag-search/",
      operation(
        apiOperation[ArticleTag]("getTags-paginated")
          .summary("Fetch tags used in articles.")
          .description("Retrieves a list of all previously used tags in articles.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(pageSize),
            asQueryParam(pageNo),
            asQueryParam(language)
          )
          .responseMessages(response500)
      )
    ) {
      val query = paramOrDefault(this.query.paramName, "")
      val pageSize = intOrDefault(this.pageSize.paramName, DefaultPageSize) match {
        case tooSmall if tooSmall < 1 => DefaultPageSize
        case x                        => x
      }
      val pageNo = intOrDefault(this.pageNo.paramName, 1) match {
        case tooSmall if tooSmall < 1 => 1
        case x                        => x
      }
      val language = paramOrDefault(this.language.paramName, AllLanguages)

      readService.getAllTags(query, pageSize, pageNo, language)
    }

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
    )(implicit request: HttpServletRequest) = {
      val result = readService.search(
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
        shouldScroll,
        requestFeideToken
      )

      result match {
        case Success(searchResult) =>
          val scrollHeader = searchResult.value.scrollId.map(i => this.scrollId.paramName -> i).toMap
          searchResult.map(searchConverterService.asApiSearchResultV2).Ok(scrollHeader)
        case Failure(ex) => Failure(ex)
      }

    }

    get(
      "/",
      operation(
        apiOperation[SearchResultV2]("getAllArticles")
          .summary("Find published articles.")
          .description("Returns all articles. You can search it too.")
          .parameters(
            asHeaderParam(correlationId),
            asHeaderParam(feideToken),
            asQueryParam(articleTypes),
            asQueryParam(query),
            asQueryParam(articleIds),
            asQueryParam(language),
            asQueryParam(license),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(sort),
            asQueryParam(scrollId)
          )
          .responseMessages(response500)
      )
    ) {
      val scrollId = paramOrNone(this.scrollId.paramName)
      val language = paramOrDefault(this.language.paramName, AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      scrollSearchOr(scrollId, language, fallback) {
        val query              = paramOrNone(this.query.paramName)
        val sort               = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
        val license            = paramOrNone(this.license.paramName)
        val pageSize           = intOrDefault(this.pageSize.paramName, DefaultPageSize)
        val page               = intOrDefault(this.pageNo.paramName, 1)
        val idList             = paramAsListOfLong(this.articleIds.paramName)
        val articleTypesFilter = paramAsListOfString(this.articleTypes.paramName)
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

    get(
      "/ids/",
      operation(
        apiOperation[List[ArticleV2]]("getArticlesByIds")
          .summary("Fetch articles that matches ids parameter.")
          .description("Returns articles that matches ids parameter.")
          .parameters(
            asQueryParam(articleIds),
            asQueryParam(fallback),
            asQueryParam(language),
            asQueryParam(pageSize),
            asQueryParam(pageNo)
          )
          .responseMessages(response500)
      )
    ) {
      val idList   = paramAsListOfLong(this.articleIds.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val language = paramOrDefault(this.language.paramName, AllLanguages)
      val pageSize = intOrDefault(this.pageSize.paramName, props.DefaultPageSize) match {
        case tooSmall if tooSmall < 1 => props.DefaultPageSize
        case x                        => x
      }
      val page = intOrDefault(this.pageNo.paramName, 1) match {
        case tooSmall if tooSmall < 1 => 1
        case x                        => x
      }

      readService.getArticlesByIds(idList, language, fallback, page, pageSize, requestFeideToken) match {
        case Failure(ex)       => errorHandler(ex)
        case Success(articles) => Ok(articles)
      }

    }

    post(
      "/search/",
      operation(
        apiOperation[List[SearchResultV2]]("getAllArticlesPost")
          .summary("Find published articles.")
          .description("Search all articles.")
          .parameters(
            asHeaderParam(correlationId),
            asHeaderParam(feideToken),
            asQueryParam(scrollId),
            bodyParam[ArticleSearchParams]
          )
          .responseMessages(response400, response500)
      )
    ) {
      val searchParams = extract[ArticleSearchParams](request.body)
      val language     = searchParams.language.getOrElse(AllLanguages)
      val fallback     = searchParams.fallback.getOrElse(false)

      scrollSearchOr(searchParams.scrollId, language, fallback) {
        val query              = searchParams.query
        val sort               = Sort.valueOf(searchParams.sort.getOrElse(""))
        val license            = searchParams.license
        val pageSize           = searchParams.pageSize.getOrElse(DefaultPageSize)
        val page               = searchParams.page.getOrElse(1)
        val idList             = searchParams.idList
        val articleTypesFilter = searchParams.articleTypes
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
    }

    get(
      "/:article_id",
      operation(
        apiOperation[ArticleV2]("getArticleById")
          .summary("Fetch specified article.")
          .description("Returns the article for the specified id.")
          .parameters(
            asHeaderParam(correlationId),
            asHeaderParam(feideToken),
            asPathParam(articleId),
            asQueryParam(language),
            asQueryParam(fallback),
            asQueryParam(revision)
          )
          .responseMessages(response404, response500)
      )
    ) {
      parseArticleIdAndRevision(params(this.articleId.paramName)) match {
        case (Failure(_), _) =>
          val ex = digitsOnlyError(articleId.paramName).exception
          errorHandler(ex)
        case (Success(articleId), inlineRevision) =>
          val language = paramOrDefault(this.language.paramName, AllLanguages)
          val fallback = booleanOrDefault(this.fallback.paramName, default = false)
          val revision = inlineRevision.orElse(intOrNone(this.revision.paramName))

          readService.withIdV2(articleId, language, fallback, revision, requestFeideToken) match {
            case Success(cachableArticle) => cachableArticle.Ok()
            case Failure(ex)              => errorHandler(ex)
          }
      }
    }

    get(
      "/:article_id/revisions",
      operation(
        apiOperation[List[Int]]("getRevisionsForArticle")
          .summary("Fetch list of existing revisions for article-id")
          .description("Fetch list of existing revisions for article-id")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(articleId)
          )
          .responseMessages(response404, response500)
      )
    ) {
      val articleId = long(this.articleId.paramName)
      readService.getRevisions(articleId) match {
        case Failure(ex)   => errorHandler(ex)
        case Success(revs) => Ok(revs)
      }
    }

    get(
      "/external_id/:deprecated_node_id",
      operation(
        apiOperation[ArticleIdV2]("getInternalIdByExternalId")
          .summary("Get id of article corresponding to specified deprecated node id.")
          .description("Get internal id of article for a specified ndla_node_id.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(deprecatedNodeId)
          )
          .responseMessages(response404, response500)
      )
    ) {
      val externalId = long(this.deprecatedNodeId.paramName)
      readService.getInternalIdByExternalId(externalId) match {
        case Some(id) => id
        case None     => NotFound(body = Error(ErrorHelpers.NOT_FOUND, s"No article with id $externalId"))
      }
    }

    get(
      "/external_ids/:deprecated_node_id",
      operation(
        apiOperation[api.ArticleIds]("getExternalIdsByExternalId")
          .summary("Get all ids related to article corresponding to specified deprecated node id.")
          .description(
            "Get internal id as well as all deprecated ndla_node_ids of article for a specified ndla_node_id."
          )
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(deprecatedNodeId)
          )
          .responseMessages(response404, response500)
      )
    ) {
      val externalId = params(this.deprecatedNodeId.paramName)
      readService.getArticleIdsByExternalId(externalId) match {
        case Some(idObject) => idObject
        case None           => NotFound()
      }
    }

    get(
      "/slug/:slug",
      operation(
        apiOperation[ArticleV2]("getArticleBySlug")
          .summary("Fetch specified article.")
          .description("Returns the article for the specified slug.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(articleSlug),
            asQueryParam(language),
            asQueryParam(fallback)
          )
          .responseMessages(response404, response500)
      )
    ) {
      val slug     = params(this.articleSlug.paramName)
      val language = paramOrDefault(this.language.paramName, AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      readService.getFrontpageArticle(slug, language, fallback) match {
        case Success(article) => article
        case Failure(ex)      => errorHandler(ex)
      }
    }
  }
}
