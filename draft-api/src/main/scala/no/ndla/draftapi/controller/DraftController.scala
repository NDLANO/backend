/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.controller

import cats.implicits.*
import io.circe.generic.auto.*
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.common.model.api.{LanguageCode, LicenseDTO}
import no.ndla.common.model.domain.ArticleType
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.draftapi.model.api.*
import no.ndla.draftapi.model.domain.{SearchSettings, Sort}
import no.ndla.draftapi.service.search.{ArticleSearchService, SearchConverterService}
import no.ndla.draftapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.draftapi.Props
import no.ndla.language.Language
import no.ndla.mapping
import no.ndla.mapping.LicenseDefinition
import no.ndla.network.tapir.NoNullJsonPrinter.*
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.{ARTICLE_API_WRITE, DRAFT_API_WRITE}
import no.ndla.network.tapir.{DynamicHeaders, TapirController}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success, Try}

trait DraftController {
  this: ReadService & WriteService & ArticleSearchService & SearchConverterService & ConverterService &
    ContentValidator & Props & ErrorHandling & TapirController =>
  val draftController: DraftController

  class DraftController extends TapirController {
    import props.{DefaultPageSize, InitialScrollContextKeywords}
    override val serviceName: String         = "drafts"
    override val prefix: EndpointInput[Unit] = "draft-api" / "v1" / serviceName

    private val queryParam =
      query[Option[String]]("query").description("Return only articles with content matching the specified query.")
    private val optionalArticleId =
      query[Option[Long]]("articleId")
        .description("The ID of the article to generate a status state machine for")
    private val pathArticleId = path[Long]("article_id").description("Id of the article that is to be fetched")
    private val pathNodeId    = path[String]("node_id").description("Id of the taxonomy node to process")
    private val articleTypes = listQuery[String]("articleTypes")
      .description("Return only articles of specific type(s). To provide multiple types, separate by comma (,).")
    private val articleIds = listQuery[Long]("ids")
      .description(
        "Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,)."
      )
    private val filter     = query[Option[String]]("filter").description("A filter to include a specific entry")
    private val filterNot  = query[Option[String]]("filterNot").description("A filter to remove a specific entry")
    private val pathStatus = path[String]("STATUS").description("An article status")
    private val copiedTitleFlag = query[Boolean]("copied-title-postfix")
      .description("Add a string to the title marking this article as a copy, defaults to 'true'.")
      .default(true)
    private val grepCodes = listQuery[String]("grep-codes")
      .description("A comma separated list of codes from GREP API the resources should be filtered by.")
    private val articleSlug = path[String]("slug").description("Slug of the article that is to be fetched.")
    private val pageNo = query[Int]("page")
      .description("The page number of the search hits to display.")
      .default(1)
      .validate(Validator.min(1))
    private val pageSize = query[Int]("page-size")
      .description("The number of search hits to display for each page.")
      .default(DefaultPageSize)
      .validate(Validator.min(0))
    private val sort = query[Option[String]]("sort").description(
      """The sorting used on results.
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    private val language = query[LanguageCode]("language")
      .description("The ISO 639-1 language code describing language.")
      .default(LanguageCode(Language.AllLanguages))
    private val pathLanguage =
      path[LanguageCode]("language").description("The ISO 639-1 language code describing language.")
    private val license = query[Option[String]]("license").description("Return only results with provided license.")
    private val fallback = query[Boolean]("fallback")
      .description("Fallback to existing language if language is specified.")
      .default(false)
    private val scrollId = query[Option[String]]("search-context").description(
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.name}' and '${this.fallback.name}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after ${props.ElasticSearchScrollKeepAlive}).
         |If you are not paginating past ${props.ElasticSearchIndexMaxResultWindow} hits, you can ignore this and use '${this.pageNo.name}' and '${this.pageSize.name}' instead.
         |""".stripMargin
    )
    private val externalIds      = listQuery[String]("externalId")
    private val oldCreatedDate   = query[Option[String]]("oldNdlaCreatedDate")
    private val oldUpdatedDate   = query[Option[String]]("oldNdlaUpdatedDate")
    private val externalSubjects = listQuery[String]("externalSubjectIds")
    private val importId         = query[Option[String]]("importId")

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getLicenses,
      getTagSearch,
      getGrepCodes,
      getAllArticles,
      postSearch,
      getStatusStateMachine,
      getArticlesByIds,
      getArticleById,
      getHistoricArticleById,
      getInternalIdByExternalId,
      newArticle,
      updateArticle,
      updateArticleStatus,
      validateArticle,
      deleteLanguage,
      cloneArticle,
      partialPublish,
      partialPublishMultiple,
      copyRevisionDates,
      getArticleBySlug,
      migrateOutdatedGreps
    )

    /** Does a scroll with [[ArticleSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(scrollId: Option[String], language: LanguageCode)(
        orFunction: => Try[(ArticleSearchResultDTO, DynamicHeaders)]
    ): Try[(ArticleSearchResultDTO, DynamicHeaders)] = {
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          articleSearchService.scroll(scroll, language.code) match {
            case Success(scrollResult) =>
              val body    = searchConverterService.asApiSearchResult(scrollResult)
              val headers = DynamicHeaders.fromMaybeValue("search-context", scrollResult.scrollId)
              Success((body, headers))
            case Failure(ex) => Failure(ex)
          }
        case _ => orFunction
      }
    }

    def getTagSearch: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Retrieves a list of all previously used tags in articles")
      .description("Retrieves a list of all previously used tags in articles")
      .in("tag-search")
      .in(queryParam)
      .in(pageSize)
      .in(pageNo)
      .in(language)
      .errorOut(errorOutputsFor(401, 403))
      .out(jsonBody[TagsSearchResultDTO])
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ =>
        { case (maybeQuery, pageSize, pageNo, language) =>
          val query = maybeQuery.getOrElse("")
          readService.getAllTags(query, pageSize, pageNo, language.code)
        }
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
    ): Try[(ArticleSearchResultDTO, DynamicHeaders)] = {
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
          val scrollHeader = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
          val output       = searchConverterService.asApiSearchResult(searchResult)
          Success((output, scrollHeader))
        case Failure(ex) => Failure(ex)
      }
    }

    def getGrepCodes: ServerEndpoint[Any, Eff] = endpoint.get
      .in("grep-codes")
      .summary("Retrieves a list of all previously used grepCodes in articles")
      .description("Retrieves a list of all previously used grepCodes in articles")
      .deprecated()
      .in(queryParam)
      .in(pageSize)
      .in(pageNo)
      .errorOut(errorOutputsFor(401, 403))
      .requirePermission(DRAFT_API_WRITE)
      .out(jsonBody[GrepCodesSearchResultDTO])
      .serverLogicPure { _ =>
        { case (maybeQuery, pageSize, pageNo) =>
          val query = maybeQuery.getOrElse("")
          readService.getAllGrepCodes(query, pageSize, pageNo)
        }
      }

    def getAllArticles: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Show all articles")
      .description("Shows all articles. You can search it too.")
      .in(articleTypes)
      .in(queryParam)
      .in(articleIds)
      .in(language)
      .in(license)
      .in(pageNo)
      .in(pageSize)
      .in(sort)
      .in(scrollId)
      .in(grepCodes)
      .in(fallback)
      .errorOut(errorOutputsFor(401, 403))
      .out(jsonBody[ArticleSearchResultDTO])
      .out(EndpointOutput.derived[DynamicHeaders])
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ =>
        {
          case (
                articleTypes,
                maybeQuery,
                articleIds,
                language,
                license,
                pageNo,
                pageSize,
                maybeSort,
                scrollId,
                grepCodes,
                fallback
              ) =>
            scrollSearchOr(scrollId, language) {
              val sort               = Sort.valueOf(maybeSort.getOrElse(""))
              val idList             = articleIds.values
              val articleTypesFilter = articleTypes.values
              val shouldScroll       = scrollId.exists(InitialScrollContextKeywords.contains)

              search(
                maybeQuery,
                sort,
                language.code,
                license,
                pageNo,
                pageSize,
                idList,
                articleTypesFilter,
                fallback,
                grepCodes.values,
                shouldScroll
              )
            }
        }
      }

    def postSearch: ServerEndpoint[Any, Eff] = endpoint.post
      .in("search")
      .summary("Show all articles")
      .description("Shows all articles. You can search it too.")
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(jsonBody[ArticleSearchResultDTO])
      .out(EndpointOutput.derived[DynamicHeaders])
      .in(jsonBody[ArticleSearchParamsDTO])
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ => searchParams =>
        val language = searchParams.language.getOrElse(LanguageCode(Language.AllLanguages))
        scrollSearchOr(searchParams.scrollId, language) {
          val query              = searchParams.query
          val sort               = searchParams.sort
          val license            = searchParams.license
          val pageSize           = searchParams.pageSize.getOrElse(DefaultPageSize)
          val page               = searchParams.page.getOrElse(1)
          val idList             = searchParams.ids
          val articleTypesFilter = searchParams.articleTypes
          val fallback           = searchParams.fallback.getOrElse(false)
          val grepCodes          = searchParams.grepCodes
          val shouldScroll       = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)

          search(
            query,
            sort,
            language.code,
            license,
            page,
            pageSize,
            idList.getOrElse(List.empty),
            articleTypesFilter.getOrElse(List.empty),
            fallback,
            grepCodes.getOrElse(List.empty),
            shouldScroll
          )
        }
      }

    def getArticleById: ServerEndpoint[Any, Eff] = endpoint.get
      .in(pathArticleId)
      .summary("Show article with a specified Id")
      .description("Shows the article for the specified id.")
      .in(language)
      .in(fallback)
      .errorOut(errorOutputsFor(401, 403, 404))
      .out(jsonBody[ArticleDTO])
      .withOptionalUser
      .serverLogicPure { user =>
        { case (articleId, language, fallback) =>
          val article        = readService.withId(articleId, language.code, fallback)
          val currentOption  = article.map(_.status.current).toOption
          val isPublicStatus = currentOption.contains(DraftStatus.EXTERNAL_REVIEW.toString)
          val permitted      = user.hasPermission(DRAFT_API_WRITE) || isPublicStatus

          if (permitted) article
          else ErrorHelpers.forbidden.asLeft
        }
      }

    def getArticlesByIds: ServerEndpoint[Any, Eff] = endpoint.get
      .in("ids")
      .summary("Fetch articles that matches ids parameter.")
      .description("Returns articles that matches ids parameter.")
      .out(jsonBody[Seq[ArticleDTO]])
      .errorOut(errorOutputsFor(400, 401, 403))
      .requirePermission(DRAFT_API_WRITE)
      .in(articleIds)
      .in(fallback)
      .in(language)
      .in(pageSize)
      .in(pageNo)
      .serverLogicPure { _ =>
        { case (articleIds, fallback, language, pageSize, page) =>
          readService
            .getArticlesByIds(
              articleIds.values,
              language.code,
              fallback,
              page.toLong,
              pageSize.toLong
            )

        }
      }

    def getHistoricArticleById: ServerEndpoint[Any, Eff] = endpoint.get
      .in(pathArticleId / "history")
      .summary("Get all saved articles with a specified Id, latest revision first")
      .description(
        "Retrieves all current and previously published articles with the specified id, latest revision first."
      )
      .in(language)
      .in(fallback)
      .out(jsonBody[Seq[ArticleDTO]])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ =>
        { case (articleId, language, fallback) =>
          readService
            .getArticles(articleId, language.code, fallback)
            .asRight
        }
      }

    def getInternalIdByExternalId: ServerEndpoint[Any, Eff] = endpoint.get
      .in("external_id" / path[Long]("deprecated_node_id"))
      .summary("Get internal id of article for a specified ndla_node_id")
      .description("Get internal id of article for a specified ndla_node_id")
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[ContentIdDTO])
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure {
        _ =>
          { externalId =>
            readService.getInternalArticleIdByExternalId(externalId) match {
              case Some(id) => id.asRight
              case None     => ErrorHelpers.notFoundWithMsg(s"No article with id $externalId").asLeft
            }
          }
      }

    def getLicenses: ServerEndpoint[Any, Eff] = endpoint.get
      .in("licenses")
      .summary("Show all valid licenses")
      .description("Shows all valid licenses")
      .errorOut(errorOutputsFor(401, 403))
      .out(jsonBody[Seq[LicenseDTO]])
      .in(filterNot)
      .in(filter)
      .serverLogicPure { case (filterNot, filter) =>
        val licenses: Seq[LicenseDefinition] = mapping.License.getLicenses
          .filter {
            case license: LicenseDefinition if filter.isDefined => license.license.toString.contains(filter.get)
            case _                                              => true
          }
          .filterNot {
            case license: LicenseDefinition if filterNot.isDefined => license.license.toString.contains(filterNot.get)
            case _                                                 => false
          }

        licenses
          .map(x => LicenseDTO(x.license.toString, Option(x.description), x.url))
          .asRight
      }

    def newArticle: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Create a new article")
      .description("Creates a new article")
      .in(jsonBody[NewArticleDTO])
      .in(externalIds)
      .in(oldCreatedDate)
      .in(oldUpdatedDate)
      .in(externalSubjects)
      .in(importId)
      .errorOut(errorOutputsFor(401, 403))
      .out(statusCode(StatusCode.Created).and(jsonBody[ArticleDTO]))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { user => params =>
        val (
          newArticle,
          externalId,
          oldNdlaCreatedDateStr,
          oldNdlaUpdatedDateStr,
          externalSubjectIds,
          importId
        ) = params
        val oldNdlaCreatedDate = oldNdlaCreatedDateStr.flatMap(NDLADate.fromString(_).toOption)
        val oldNdlaUpdatedDate = oldNdlaUpdatedDateStr.flatMap(NDLADate.fromString(_).toOption)

        writeService
          .newArticle(
            newArticle,
            externalId.values,
            externalSubjectIds.values,
            user,
            oldNdlaCreatedDate,
            oldNdlaUpdatedDate,
            importId
          )

      }

    def updateArticle: ServerEndpoint[Any, Eff] = endpoint.patch
      .in(pathArticleId)
      .summary("Update an existing article")
      .description("Update an existing article")
      .in(jsonBody[UpdatedArticleDTO])
      .in(externalIds)
      .in(oldCreatedDate)
      .in(oldUpdatedDate)
      .in(externalSubjects)
      .in(importId)
      .errorOut(errorOutputsFor(401, 403, 404, 409, 502))
      .out(jsonBody[ArticleDTO])
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { user => params =>
        val (
          articleId,
          updatedArticle,
          externalId,
          oldNdlaCreatedDateStr,
          oldNdlaUpdatedDateStr,
          externalSubjectIds,
          importId
        ) = params
        val oldNdlaCreatedDate = oldNdlaCreatedDateStr.flatMap(NDLADate.fromString(_).toOption)
        val oldNdlaUpdatedDate = oldNdlaUpdatedDateStr.flatMap(NDLADate.fromString(_).toOption)

        writeService
          .updateArticle(
            articleId,
            updatedArticle,
            externalId.values,
            externalSubjectIds.values,
            user,
            oldNdlaCreatedDate,
            oldNdlaUpdatedDate,
            importId
          )

      }

    def updateArticleStatus: ServerEndpoint[Any, Eff] = endpoint.put
      .in(pathArticleId / "status" / pathStatus)
      .summary("Update status of an article")
      .description("Update status of an article")
      .in(query[Boolean]("import_publish").default(false))
      .errorOut(errorOutputsFor(401, 403, 404))
      .out(jsonBody[ArticleDTO])
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { user =>
        { case (id, status, isImported) =>
          DraftStatus
            .valueOfOrError(status)
            .flatMap(
              writeService.updateArticleStatus(_, id, user, isImported)
            )

        }
      }

    def validateArticle: ServerEndpoint[Any, Eff] = endpoint.put
      .in(pathArticleId / "validate")
      .summary("Validate an article")
      .description("Validate an article")
      .in(query[Boolean]("import_validate").default(false))
      .in(jsonBody[Option[UpdatedArticleDTO]])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[ContentIdDTO])
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure {
        user =>
          { params =>
            val (articleId, importValidate, updateArticle) = params
            val result = updateArticle match {
              case Some(art) => contentValidator.validateArticleApiArticle(articleId, art, importValidate, user)
              case None      => contentValidator.validateArticleApiArticle(articleId, importValidate, user)
            }

            result
          }
      }

    def deleteLanguage: ServerEndpoint[Any, Eff] = endpoint.delete
      .in(pathArticleId / "language" / pathLanguage)
      .summary("Delete language from article")
      .description("Delete language from article")
      .out(jsonBody[ArticleDTO])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { user =>
        { case (articleId, language) =>
          writeService.deleteLanguage(articleId, language.code, user)
        }
      }

    def getStatusStateMachine: ServerEndpoint[Any, Eff] = endpoint.get
      .in("status-state-machine")
      .summary("Get status state machine")
      .description("Get status state machine")
      .in(optionalArticleId)
      .out(jsonBody[Map[String, List[String]]])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure {
        user =>
          { id =>
            converterService.stateTransitionsToApi(user, id)
          }
      }

    def cloneArticle: ServerEndpoint[Any, Eff] = endpoint.post
      .in("clone" / pathArticleId)
      .summary("Create a new article with the content of the article with the specified id")
      .description("Create a new article with the content of the article with the specified id")
      .in(language)
      .in(fallback)
      .in(copiedTitleFlag)
      .out(jsonBody[ArticleDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { user =>
        { case (articleId, language, fallback, copiedTitlePostfix) =>
          writeService
            .copyArticleFromId(articleId, user, language.code, fallback, copiedTitlePostfix)

        }
      }

    def partialPublish: ServerEndpoint[Any, Eff] = endpoint.post
      .in("partial-publish" / pathArticleId)
      .summary("Partial publish selected fields")
      .description("Partial publish selected fields")
      .in(language)
      .in(fallback)
      .out(jsonBody[ArticleDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .in(jsonBody[Seq[PartialArticleFieldsDTO]])
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { user =>
        { case (articleId, language, fallback, articleFieldsToUpdate) =>
          writeService
            .partialPublishAndConvertToApiArticle(
              articleId,
              articleFieldsToUpdate,
              language.code,
              fallback,
              user
            )

        }
      }

    def partialPublishMultiple: ServerEndpoint[Any, Eff] = endpoint.post
      .in("partial-publish")
      .summary("Partial publish selected fields for multiple articles")
      .description("Partial publish selected fields for multiple articles")
      .in(language)
      .in(jsonBody[PartialBulkArticlesDTO])
      .out(jsonBody[MultiPartialPublishResultDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { user =>
        { case (language, partialBulk) =>
          writeService
            .partialPublishMultiple(language.code, partialBulk, user)

        }
      }

    def copyRevisionDates: ServerEndpoint[Any, Eff] = endpoint.post
      .in("copyRevisionDates" / pathNodeId)
      .summary("Copy revision dates from the node with this id to _all_ children in taxonomy")
      .description("Copy revision dates from the node with this id to _all_ children in taxonomy")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ => publicId =>
        writeService.copyRevisionDates(publicId) match {
          case Success(_)  => Right(())
          case Failure(ex) => returnLeftError(ex)
        }
      }

    def getArticleBySlug: ServerEndpoint[Any, Eff] = endpoint.get
      .in("slug" / articleSlug)
      .summary("Show article with a specified slug")
      .description("Shows the article for the specified slug.")
      .out(jsonBody[ArticleDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .in(language)
      .in(fallback)
      .withOptionalUser
      .serverLogicPure { user =>
        { case (slug, language, fallback) =>
          val article        = readService.getArticleBySlug(slug, language.code, fallback)
          val currentOption  = article.map(_.status.current).toOption
          val isPublicStatus = currentOption.contains(DraftStatus.EXTERNAL_REVIEW.toString)
          val permitted      = user.hasPermission(DRAFT_API_WRITE) || isPublicStatus
          if (permitted) article
          else ErrorHelpers.forbidden.asLeft
        }
      }

    def migrateOutdatedGreps: ServerEndpoint[Any, Eff] = endpoint.post
      .in("migrate-greps")
      .summary("Iterate all articles and migrate outdated grep codes")
      .description("Iterate all articles and migrate outdated grep codes")
      .errorOut(errorOutputsFor(500))
      .out(emptyOutput)
      .requirePermission(DRAFT_API_WRITE, ARTICLE_API_WRITE)
      .serverLogicPure { user => _ =>
        writeService.migrateOutdatedGreps(user).handleErrorsOrOk
      }
  }
}
