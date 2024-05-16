/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import cats.implicits.*
import io.circe.generic.auto.*
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.*
import no.ndla.articleapi.model.domain.Sort
import no.ndla.articleapi.service.search.{ArticleSearchService, SearchConverterService}
import no.ndla.articleapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.articleapi.{Eff, Props}
import no.ndla.common.ContentURIUtil.parseArticleIdAndRevision
import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.language.Language.AllLanguages
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Parameters.feideHeader
import no.ndla.network.tapir.{DynamicHeaders, Service}
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import sttp.model.{Header, MediaType}
import sttp.tapir.EndpointIO.annotations.{header, jsonbody}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success, Try}

trait ArticleControllerV2 {
  this: ReadService
    with WriteService
    with ArticleSearchService
    with SearchConverterService
    with ConverterService
    with ContentValidator
    with Props
    with ErrorHelpers =>
  val articleControllerV2: ArticleControllerV2

  import props._

  class ArticleControllerV2() extends Service[Eff] {
    protected val applicationDescription = "Services for accessing articles from NDLA."

    override val serviceName: String         = "articles"
    override val prefix: EndpointInput[Unit] = "article-api" / "v2" / serviceName

    private val queryParam =
      query[Option[String]]("query").description("Return only articles with content matching the specified query.")
    private val language =
      query[String]("language")
        .description("The ISO 639-1 language code describing language.")
        .default(AllLanguages)
    private val license = query[Option[String]]("license").description(
      "Return only results with provided license. Specifying 'all' gives all articles regardless of licence."
    )
    private val sort = query[Option[String]]("sort").description(s"""The sorting used on results.
             The following are supported: ${Sort.all.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and id (asc) when query is empty.""".stripMargin)
    private val pageNo = query[Option[Int]]("page").description("The page number of the search hits to display.")
    private val pageSize =
      query[Option[Int]]("page-size")
        .description("The number of search hits to display for each page.")
    private val articleId = path[String]("article_id").description("Id or slug of the article that is to be fetched.")
    private val articleIdLong = path[Long]("article_id").description("Id or slug of the article that is to be fetched.")
    private val revision =
      query[Option[Int]]("revision")
        .description("Revision of article to fetch. If not provided the current revision is returned.")
    private val articleTypes = listQuery[String]("articleTypes")
      .description(
        "Return only articles of specific type(s). To provide multiple types, separate by comma (,)."
      )
    private val articleIds = listQuery[Long]("ids")
      .description(
        "Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,)."
      )
    private val deprecatedNodeId = path[String]("deprecated_node_id").description("Id of deprecated NDLA node")
    private val fallback =
      query[Boolean]("fallback").description("Fallback to existing language if language is specified.").default(false)
    protected val scrollId: EndpointInput.Query[Option[String]] = query[Option[String]]("search-context").description(
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.name}' and '${this.fallback.name}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.name}' and '${this.pageSize.name}' instead.
         |""".stripMargin
    )
    private val grepCodes = listQuery[String]("grep-codes")
      .description(
        "A comma separated list of codes from GREP API the resources should be filtered by."
      )

    private case class SummaryWithHeader(
        @jsonbody
        body: SearchResultV2,
        @header("search-context")
        searchContext: Option[String]
    )

    /** Does a scroll with [[AudioSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(scrollId: Option[String], language: String)(
        orFunction: => Try[(SearchResultV2, DynamicHeaders)]
    ): Try[(SearchResultV2, DynamicHeaders)] =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          articleSearchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val body    = searchConverterService.asApiSearchResultV2(scrollResult)
              val headers = DynamicHeaders.fromMaybeValue("search-context", scrollResult.scrollId)
              Success((body, headers))
            case Failure(ex) => Failure(ex)
          }
        case _ => orFunction
      }

    def tagSearch: ServerEndpoint[Any, Eff] =
      endpoint.get
        .in("tag-search")
        .summary("Fetch tags used in articles.")
        .description("Retrieves a list of all previously used tags in articles.")
        .in(queryParam)
        .in(pageSize)
        .in(pageNo)
        .in(language)
        .out(jsonBody[TagsSearchResult])
        .errorOut(errorOutputsFor())
        .serverLogicPure { case (query, pageSize, pageNo, language) =>
          val queryOrEmpty = query.getOrElse("")
          val parsedPageSize = pageSize.getOrElse(DefaultPageSize) match {
            case tooSmall if tooSmall < 1 => DefaultPageSize
            case x                        => x
          }
          val parsedPageNo = pageNo.getOrElse(1) match {
            case tooSmall if tooSmall < 1 => 1
            case x                        => x
          }

          readService
            .getAllTags(
              queryOrEmpty,
              parsedPageSize,
              parsedPageNo,
              language
            )
            .asRight
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
        shouldScroll: Boolean,
        feideToken: Option[String]
    ): Try[(SearchResultV2, DynamicHeaders)] = {
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
        feideToken
      )

      result match {
        case Success(searchResult) =>
          val scrollHeader = DynamicHeaders.fromOpt("search-context", searchResult.value.scrollId)
          val output       = searchResult.map(searchConverterService.asApiSearchResultV2).Ok(scrollHeader.toList)
          Success(output)
        case Failure(ex) => Failure(ex)
      }

    }

    def getSearch: ServerEndpoint[Any, Eff] = {
      endpoint.get
        .summary("Find published articles.")
        .description("Returns all articles. You can search it too.")
        .in(feideHeader)
        .in(queryParam)
        .in(articleTypes)
        .in(articleIds)
        .in(language)
        .in(license)
        .in(pageNo)
        .in(pageSize)
        .in(sort)
        .in(fallback)
        .in(scrollId)
        .in(grepCodes)
        .out(jsonBody[SearchResultV2])
        .out(EndpointOutput.derived[DynamicHeaders])
        .errorOut(errorOutputsFor())
        .serverLogicPure {
          case (
                feideToken,
                query,
                articleTypes,
                articleIds,
                language,
                license,
                maybePageNo,
                maybePageSize,
                maybeSort,
                fallback,
                scrollId,
                grepCodes
              ) =>
            scrollSearchOr(scrollId, language) {
              val sort         = Sort.valueOf(maybeSort.getOrElse(""))
              val pageSize     = maybePageSize.getOrElse(DefaultPageSize)
              val page         = maybePageNo.getOrElse(1)
              val shouldScroll = scrollId.exists(InitialScrollContextKeywords.contains)

              search(
                query,
                sort,
                language,
                license,
                page,
                pageSize,
                articleIds.values,
                articleTypes.values,
                fallback,
                grepCodes.values,
                shouldScroll,
                feideToken
              )
            }.handleErrorsOrOk
        }
    }

    def getByIds: ServerEndpoint[Any, Eff] = endpoint.get
      .in("ids")
      .summary("Fetch articles that matches ids parameter.")
      .description("Returns articles that matches ids parameter.")
      .in(feideHeader)
      .in(articleIds)
      .in(fallback)
      .in(language)
      .in(pageSize)
      .in(pageNo)
      .errorOut(errorOutputsFor())
      .out(jsonBody[Seq[ArticleV2]])
      .serverLogicPure { case (feideToken, ids, fallback, language, mbPageSize, mbPageNo) =>
        val pageSize = mbPageSize.getOrElse(props.DefaultPageSize) match {

          case tooSmall if tooSmall < 1 => props.DefaultPageSize
          case x                        => x
        }
        val page = mbPageNo.getOrElse(1) match {
          case tooSmall if tooSmall < 1 => 1
          case x                        => x
        }

        readService
          .getArticlesByIds(
            ids.values,
            language,
            fallback,
            page,
            pageSize,
            feideToken
          )
          .handleErrorsOrOk
      }

    def postSearch: ServerEndpoint[Any, Eff] = endpoint.post
      .in("search")
      .summary("Find published articles.")
      .description("Search all articles.")
      .in(feideHeader)
      .in(jsonBody[ArticleSearchParams])
      .errorOut(errorOutputsFor())
      .out(jsonBody[SearchResultV2])
      .out(EndpointOutput.derived[DynamicHeaders])
      .serverLogicPure { case (feideToken, searchParams) =>
        val language = searchParams.language.getOrElse(AllLanguages)
        val fallback = searchParams.fallback.getOrElse(false)

        scrollSearchOr(searchParams.scrollId, language) {
          val query              = searchParams.query
          val sort               = searchParams.sort
          val license            = searchParams.license
          val pageSize           = searchParams.pageSize.getOrElse(DefaultPageSize)
          val page               = searchParams.page.getOrElse(1)
          val idList             = searchParams.ids.getOrElse(List.empty)
          val articleTypesFilter = searchParams.articleTypes.getOrElse(List.empty)
          val grepCodes          = searchParams.grepCodes.getOrElse(Seq.empty)
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
            shouldScroll,
            feideToken
          )
        }.handleErrorsOrOk
      }

    def getSingle: ServerEndpoint[Any, Eff] = endpoint.get
      .in(articleId)
      .in(revision)
      .in(feideHeader)
      .in(language)
      .in(fallback)
      .errorOut(errorOutputsFor(410))
      .out(jsonBody[ArticleV2])
      .out(EndpointOutput.derived[DynamicHeaders])
      .serverLogicPure { params =>
        val (articleId, revisionQuery, feideToken, language, fallback) = params
        (parseArticleIdAndRevision(articleId) match {
          case (Failure(_), _) =>
            readService.getArticleBySlug(articleId, language, fallback)
          case (Success(articleId), inlineRevision) =>
            val revision = inlineRevision.orElse(revisionQuery)
            readService.withIdV2(articleId, language, fallback, revision, feideToken)
        }).map(_.Ok()).handleErrorsOrOk
      }

    def getRevisions: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch list of existing revisions for article-id")
      .description("Fetch list of existing revisions for article-id")
      .in(articleIdLong)
      .in("revisions")
      .errorOut(errorOutputsFor(404, 500))
      .out(jsonBody[Seq[Int]])
      .serverLogicPure(articleId => {
        readService.getRevisions(articleId).handleErrorsOrOk
      })

    def getByExternal: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get id of article corresponding to specified deprecated node id.")
      .description("Get internal id of article for a specified ndla_node_id.")
      .in("external_id")
      .in(deprecatedNodeId)
      .errorOut(errorOutputsFor(404, 500))
      .out(jsonBody[ArticleIdV2])
      .serverLogicPure(externalId => {
        readService.getInternalIdByExternalId(externalId) match {
          case Some(id) => Right(id)
          case None     => Left(ErrorHelpers.notFoundWithMsg(s"No article with id $externalId"))
        }
      })

    def getIdsByExternal: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get all ids related to article corresponding to specified deprecated node id.")
      .description(
        "Get internal id as well as all deprecated ndla_node_ids of article for a specified ndla_node_id."
      )
      .in("external_ids")
      .in(deprecatedNodeId)
      .errorOut(errorOutputsFor(404, 500))
      .out(jsonBody[api.ArticleIds])
      .serverLogicPure(externalId => {
        readService.getArticleIdsByExternalId(externalId) match {
          case Some(idObject) => Right(idObject)
          case None           => Left(ErrorHelpers.notFound)
        }
      })

    def articleFrontPageRSS: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get RSS feed for articles at a level in the frontpage menu")
      .in(path[String]("slug").description("Slug of the article to generate RSS for") / "rss.xml")
      .errorOut(errorOutputsFor(400, 410, 404))
      .out(stringBody)
      .out(EndpointOutput.derived[DynamicHeaders])
      .serverLogicPure { slug =>
        readService
          .getArticleFrontpageRSS(slug)
          .map(_.Ok(List(Header.contentType(MediaType.ApplicationXml))))
          .handleErrorsOrOk
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      tagSearch,
      getByIds,
      articleFrontPageRSS,
      getSingle,
      getSearch,
      postSearch,
      getRevisions,
      getByExternal,
      getIdsByExternal
    )

  }
}
