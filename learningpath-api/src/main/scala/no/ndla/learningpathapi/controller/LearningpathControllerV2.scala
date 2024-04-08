/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import cats.implicits.catsSyntaxEitherId
import no.ndla.common.model.api.CommaSeparatedList._
import no.ndla.common.model.api.{Author, License}
import no.ndla.language.Language
import no.ndla.language.Language.AllLanguages
import no.ndla.learningpathapi.{Eff, Props}
import no.ndla.learningpathapi.integration.TaxonomyApiClient
import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathTokenUser
import no.ndla.learningpathapi.model.domain.{LearningPathStatus => _, License => _, _}
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchService}
import no.ndla.learningpathapi.service.{ConverterService, ReadService, UpdateService}
import no.ndla.learningpathapi.validation.LanguageValidator
import no.ndla.mapping
import no.ndla.mapping.LicenseDefinition
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.network.tapir.{DynamicHeaders, Service}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success, Try}

trait LearningpathControllerV2 {

  this: ReadService
    with UpdateService
    with SearchService
    with LanguageValidator
    with ConverterService
    with TaxonomyApiClient
    with SearchConverterServiceComponent
    with Props
    with ErrorHelpers =>
  val learningpathControllerV2: LearningpathControllerV2

  class LearningpathControllerV2 extends Service[Eff] {

    import ErrorHelpers._
    import props.{
      DefaultLanguage,
      ElasticSearchIndexMaxResultWindow,
      ElasticSearchScrollKeepAlive,
      InitialScrollContextKeywords
    }

    override val serviceName: String         = "learningpaths"
    override val prefix: EndpointInput[Unit] = "learningpath-api" / "v2" / serviceName

    private val pathArticleId =
      path[Long]("article_id").description("Id of the article to search with")
    private val queryParam =
      query[Option[String]]("query").description("Return only Learningpaths with content matching the specified query.")
    private val language =
      query[String]("language")
        .description("The ISO 639-1 language code describing language.")
        .default(Language.AllLanguages)
    private val sort = query[Option[String]]("sort").description(
      s"""The sorting used on results.
             The following are supported: ${Sort.all.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    private val pageNo = query[Option[Int]]("page")
      .description("The page number of the search hits to display.")
    private val pageSize = query[Option[Int]]("page-size")
      .description("The number of search hits to display for each page.")
    private val pathLearningpathId =
      path[Long]("learningpath_id").description("Id of the learningpath.")
    private val pathLearningstepId =
      path[Long]("learningstep_id").description("Id of the learningstep.")
    private val tag =
      query[Option[String]]("tag").description("Return only Learningpaths that are tagged with this exact tag.")
    private val learningpathIds = listQuery[Long]("ids")
      .description(
        "Return only Learningpaths that have one of the provided ids. To provide multiple ids, separate by comma (,)."
      )
    private val licenseFilter =
      query[Option[String]]("filter")
        .description("Query for filtering licenses. Only licenses containing filter-string are returned.")
    private val fallback = query[Boolean]("fallback")
      .description("Fallback to existing language if language is specified.")
      .default(false)
    private val createResourceIfMissing = query[Boolean]("create-if-missing")
      .description("Create taxonomy resource if missing for learningPath")
      .default(false)
    private val learningPathStatus = path[String]("STATUS").description("Status of LearningPaths")
    private val scrollId = query[Option[String]]("search-context")
      .description(
        s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
            .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.name}' and '${this.fallback.name}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.name}' and '${this.pageSize.name}' instead.
         |""".stripMargin
      )
    private val verificationStatus = query[Option[String]]("verificationStatus")
      .description("Return only learning paths that have this verification status.")
    private val ids = listQuery[Long]("ids")
      .description(
        "Return only learningpaths that have one of the provided ids. To provide multiple ids, separate by comma (,)."
      )

    /** Does a scroll with [[SearchService]] If no scrollId is specified execute the function @orFunction in the second
      * parameter list.
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
          searchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val body    = searchConverterService.asApiSearchResult(scrollResult)
              val headers = DynamicHeaders.fromMaybeValue("search-context", scrollResult.scrollId)
              Success((body, headers))
            case Failure(ex) => Failure(ex)
          }
        case _ => orFunction
      }

    private def search(
        query: Option[String],
        searchLanguage: String,
        tag: Option[String],
        idList: List[Long],
        sort: Option[String],
        pageSize: Option[Int],
        page: Option[Int],
        fallback: Boolean,
        verificationStatus: Option[String],
        shouldScroll: Boolean
    ) = {
      val settings = query match {
        case Some(q) =>
          SearchSettings(
            query = Some(q),
            withIdIn = idList,
            taggedWith = tag,
            withPaths = List.empty,
            language = Some(searchLanguage),
            sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc),
            page = page,
            pageSize = pageSize,
            fallback = fallback,
            verificationStatus = verificationStatus,
            shouldScroll = shouldScroll,
            status = List(domain.LearningPathStatus.PUBLISHED)
          )
        case None =>
          SearchSettings(
            query = None,
            withIdIn = idList,
            taggedWith = tag,
            withPaths = List.empty,
            language = Some(searchLanguage),
            sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc),
            page = page,
            pageSize = pageSize,
            fallback = fallback,
            verificationStatus = verificationStatus,
            shouldScroll = shouldScroll,
            status = List(domain.LearningPathStatus.PUBLISHED)
          )
      }

      searchService.matchingQuery(settings) match {
        case Success(searchResult) =>
          val scrollHeader = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
          val output       = searchConverterService.asApiSearchResult(searchResult)
          Success((output, scrollHeader))
        case Failure(ex) => Failure(ex)
      }
    }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getLearningpaths,
      postSearch,
      getTags,
      getLicenses,
      getMyLearningpaths,
      getContributors,
      getLearningpathsByIds,
      getLearningpath,
      getLearningpathStatus,
      getLearningStepsInTrash,
      getLearningsteps,
      getLearningStep,
      fetchLearningPathContainingArticle,
      getLearningStepStatus,
      addLearningpath,
      copyLearningpath,
      updateLearningPath,
      addLearningStep,
      updateLearningStep,
      updatedLearningstepSeqNo,
      updateLearningStepStatus,
      updateLearningPathStatus,
      withStatus,
      deleteLearningpath,
      deleteLearningStep,
      updateLearningPathTaxonomy
    )

    def getLearningpaths: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Find public learningpaths")
      .description("Show public learningpaths.")
      .in(queryParam)
      .in(tag)
      .in(learningpathIds)
      .in(language)
      .in(pageNo)
      .in(pageSize)
      .in(sort)
      .in(fallback)
      .in(scrollId)
      .in(verificationStatus)
      .out(jsonBody[SearchResultV2])
      .out(EndpointOutput.derived[DynamicHeaders])
      .errorOut(errorOutputsFor(400))
      .serverLogicPure {
        case (query, tag, idList, language, pageNo, pageSize, sortStr, fallback, scrollId, verificationStatus) =>
          scrollSearchOr(scrollId, language) {
            val shouldScroll = scrollId.exists(InitialScrollContextKeywords.contains)
            search(
              query,
              language,
              tag,
              idList.values,
              sortStr,
              pageSize,
              pageNo,
              fallback,
              verificationStatus,
              shouldScroll
            )
          }.handleErrorsOrOk
      }

    def postSearch: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Find public learningpaths")
      .description("Show public learningpaths")
      .in("search")
      .in(jsonBody[SearchParams])
      .errorOut(errorOutputsFor(400))
      .out(jsonBody[SearchResultV2])
      .out(EndpointOutput.derived[DynamicHeaders])
      .serverLogicPure { searchParams =>
        scrollSearchOr(searchParams.scrollId, searchParams.language.getOrElse(AllLanguages)) {
          val shouldScroll = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)
          search(
            query = searchParams.query,
            searchLanguage = searchParams.language.getOrElse(AllLanguages),
            tag = searchParams.tag,
            idList = searchParams.ids.getOrElse(List.empty),
            sort = searchParams.sort,
            pageSize = searchParams.pageSize,
            page = searchParams.page,
            fallback = searchParams.fallback.getOrElse(false),
            verificationStatus = searchParams.verificationStatus,
            shouldScroll = shouldScroll
          )
        }.handleErrorsOrOk
      }

    def getLearningpathsByIds: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch learningpaths that matches ids parameter.")
      .description("Returns learningpaths that matches ids parameter.")
      .in("ids")
      .in(ids)
      .in(fallback)
      .in(language)
      .in(pageSize)
      .in(pageNo)
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(jsonBody[Seq[LearningPathV2]])
      .requirePermission()
      .serverLogicPure { user =>
        { case (idList, fallback, language, pageSizeQ, pageNoQ) =>
          if (!user.isNdla) {
            forbidden.asLeft
          } else {
            val pageSize = pageSizeQ.getOrElse(props.DefaultPageSize) match {
              case tooSmall if tooSmall < 1 => props.DefaultPageSize
              case x                        => x
            }
            val page = pageNoQ.getOrElse(1) match {
              case tooSmall if tooSmall < 1 => 1
              case x                        => x
            }
            readService.withIdV2List(idList.values, language, fallback, page, pageSize, user) match {
              case Failure(ex)       => returnLeftError(ex)
              case Success(articles) => articles.asRight
            }
          }
        }
      }

    def getLearningpath: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch details about the specified learningpath")
      .description("Shows all information about the specified learningpath.")
      .in(pathLearningpathId)
      .in(language)
      .in(fallback)
      .out(jsonBody[LearningPathV2])
      .errorOut(errorOutputsFor(401, 403, 404))
      .withOptionalUser
      .serverLogicPure { maybeUser =>
        { case (id, language, fallback) =>
          val user = maybeUser.getOrElse(TokenUser.PublicUser)
          readService.withIdV2(id, language, fallback, user).handleErrorsOrOk
        }
      }

    def getLearningpathStatus: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Show status information for the learningpath")
      .description("Shows publishingstatus for the learningpath")
      .in(pathLearningpathId / "status")
      .out(jsonBody[LearningPathStatus])
      .errorOut(errorOutputsFor(401, 403, 404))
      .withOptionalUser
      .serverLogicPure {
        maybeUser =>
          { id =>
            readService.statusFor(id, maybeUser.getOrElse(TokenUser.PublicUser)).handleErrorsOrOk
          }
      }

    def getLearningsteps: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch learningsteps for given learningpath")
      .description("Show all learningsteps for given learningpath id")
      .in(pathLearningpathId / "learningsteps")
      .in(fallback)
      .in(language)
      .out(jsonBody[LearningStepContainerSummary])
      .errorOut(errorOutputsFor(401, 403, 404))
      .withOptionalUser
      .serverLogicPure { maybeUser =>
        { case (id, fallback, language) =>
          readService
            .learningstepsForWithStatusV2(
              id,
              StepStatus.ACTIVE,
              language,
              fallback,
              maybeUser.getOrElse(TokenUser.PublicUser)
            )
            .handleErrorsOrOk
        }
      }

    def getLearningStep: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch details about the specified learningstep")
      .description("Show the given learningstep for the given learningpath")
      .in(pathLearningpathId / "learningsteps" / pathLearningstepId)
      .in(language)
      .in(fallback)
      .out(jsonBody[LearningStepV2])
      .errorOut(errorOutputsFor(401, 403, 404))
      .withOptionalUser
      .serverLogicPure { maybeUser =>
        { case (pathId, stepId, language, fallback) =>
          readService
            .learningstepV2For(pathId, stepId, language, fallback, maybeUser.getOrElse(TokenUser.PublicUser))
            .handleErrorsOrOk
        }
      }

    def getLearningStepsInTrash: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch deleted learningsteps for given learningpath")
      .description("Show all learningsteps for the given learningpath that are marked as deleted")
      .in(pathLearningpathId / "learningsteps" / "trash")
      .in(language)
      .in(fallback)
      .out(jsonBody[LearningStepContainerSummary])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requirePermission()
      .serverLogicPure { user =>
        { case (id, language, fallback) =>
          readService.learningstepsForWithStatusV2(id, StepStatus.DELETED, language, fallback, user).handleErrorsOrOk
        }
      }

    def getLearningStepStatus: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Show status information for learningstep")
      .description("Shows status for the learningstep")
      .in(pathLearningpathId / "learningsteps" / pathLearningstepId / "status")
      .in(fallback)
      .out(jsonBody[LearningStepStatus])
      .errorOut(errorOutputsFor(401, 403, 404))
      .withOptionalUser
      .serverLogicPure { maybeUser =>
        { case (pathId, stepId, fallback) =>
          val user = maybeUser.getOrElse(TokenUser.PublicUser)
          readService
            .learningStepStatusForV2(
              pathId,
              stepId,
              DefaultLanguage,
              fallback,
              user
            )
            .handleErrorsOrOk
        }
      }

    def getMyLearningpaths: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch all learningspaths you have created")
      .description("Shows your learningpaths.")
      .in("mine")
      .out(jsonBody[List[LearningPathSummaryV2]])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requirePermission()
      .serverLogicPure { user => _ => readService.withOwnerV2(user).asRight }

    def getLicenses: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Show all valid licenses")
      .description("Shows all valid licenses")
      .in("licenses")
      .in(licenseFilter)
      .out(jsonBody[Seq[License]])
      .errorOut(errorOutputsFor(401, 403, 404))
      .serverLogicPure { license =>
        val licenses: Seq[LicenseDefinition] =
          license match {
            case None => mapping.License.getLicenses
            case Some(filter) =>
              mapping.License.getLicenses
                .filter(_.license.toString.contains(filter))
          }
        licenses.map(x => License(x.license.toString, Option(x.description), x.url)).asRight
      }

    def addLearningpath: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Store new learningpath")
      .description("Adds the given learningpath")
      .in(jsonBody[NewLearningPathV2])
      .out(statusCode(StatusCode.Created).and(jsonBody[LearningPathV2]))
      .out(EndpointOutput.derived[DynamicHeaders])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .requirePermission()
      .serverLogicPure { user => newLearningPath =>
        updateService.addLearningPathV2(newLearningPath, user) match {
          case Failure(ex) => returnLeftError(ex)
          case Success(learningPath) =>
            logger.info(s"CREATED LearningPath with ID =  ${learningPath.id}")
            val headers = DynamicHeaders.fromValue("Location", learningPath.metaUrl)
            (learningPath, headers).asRight
        }
      }

    def copyLearningpath: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Copy given learningpath and store it as a new learningpath")
      .description("Copies the given learningpath, with the option to override some fields")
      .in(pathLearningpathId / "copy")
      .in(jsonBody[NewCopyLearningPathV2])
      .out(statusCode(StatusCode.Created).and(jsonBody[LearningPathV2]))
      .out(EndpointOutput.derived[DynamicHeaders])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .requirePermission()
      .serverLogicPure { user =>
        { case (pathId, newLearningPath) =>
          UserInfo
            .getWithUserIdOrAdmin(user)
            .flatMap(userInfo =>
              updateService
                .newFromExistingV2(pathId, newLearningPath, userInfo)
                .map(learningPath => {
                  logger.info(s"COPIED LearningPath with ID =  ${learningPath.id}")
                  val headers = DynamicHeaders.fromValue("Location", learningPath.metaUrl)
                  (learningPath, headers)
                })
            )
            .handleErrorsOrOk
        }
      }

    def updateLearningPath: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update given learningpath")
      .description("Updates the given learningPath")
      .in(pathLearningpathId)
      .in(jsonBody[UpdatedLearningPathV2])
      .out(jsonBody[LearningPathV2])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .requirePermission()
      .serverLogicPure { user =>
        { case (pathId, newLearningPath) =>
          updateService
            .updateLearningPathV2(
              pathId,
              newLearningPath,
              user
            )
            .handleErrorsOrOk
        }
      }

    def addLearningStep: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Add new learningstep to learningpath")
      .description("Adds the given LearningStep")
      .in(pathLearningpathId / "learningsteps")
      .in(jsonBody[NewLearningStepV2])
      .out(statusCode(StatusCode.Created).and(jsonBody[LearningStepV2]))
      .out(EndpointOutput.derived[DynamicHeaders])
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .requirePermission()
      .serverLogicPure { user =>
        { case (pathId, newLearningStep) =>
          updateService
            .addLearningStepV2(pathId, newLearningStep, user)
            .map { learningStep =>
              logger.info(s"CREATED LearningStep with ID =  ${learningStep.id} for LearningPath with ID = $pathId")
              val headers = DynamicHeaders.fromValue("Location", learningStep.metaUrl)
              (learningStep, headers)
            }
            .handleErrorsOrOk
        }
      }

    def updateLearningStep: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update given learningstep")
      .description("Update the given learningStep")
      .in(pathLearningpathId / "learningsteps" / pathLearningstepId)
      .in(jsonBody[UpdatedLearningStepV2])
      .out(jsonBody[LearningStepV2])
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .requirePermission()
      .serverLogicPure { user =>
        { case (pathId, stepId, updatedLearningStep) =>
          updateService
            .updateLearningStepV2(pathId, stepId, updatedLearningStep, user)
            .map(learningStep => {
              logger.info(s"UPDATED LearningStep with ID = $stepId for LearningPath with ID = $pathId")
              learningStep
            })
            .handleErrorsOrOk
        }
      }

    def updatedLearningstepSeqNo: ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Store new sequence number for learningstep.")
      .description(
        "Updates the sequence number for the given learningstep. The sequence number of other learningsteps will be affected by this."
      )
      .in(pathLearningpathId / "learningsteps" / pathLearningstepId / "seqNo")
      .in(jsonBody[LearningStepSeqNo])
      .out(jsonBody[LearningStepSeqNo])
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .requirePermission()
      .serverLogicPure { user =>
        { case (pathId, stepId, newSeqNo) =>
          updateService.updateSeqNo(pathId, stepId, newSeqNo.seqNo, user).handleErrorsOrOk
        }
      }

    def updateLearningStepStatus: ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Update status of given learningstep")
      .description("Updates the status of the given learningstep")
      .in(pathLearningpathId / "learningsteps" / pathLearningstepId / "status")
      .in(jsonBody[LearningStepStatus])
      .out(jsonBody[LearningStepV2])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .requirePermission()
      .serverLogicPure { user =>
        { case (pathId, stepId, learningStepStatus) =>
          val stepStatus = StepStatus.valueOfOrError(learningStepStatus.status)
          updateService
            .updateLearningStepStatusV2(pathId, stepId, stepStatus, user)
            .map(learningStep => {
              logger.info(
                s"UPDATED LearningStep with id: $stepId for LearningPath with id: $pathId to STATUS = ${learningStep.status}"
              )
              learningStep
            })
            .handleErrorsOrOk
        }
      }

    def updateLearningPathStatus: ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Update status of given learningpath")
      .description("Updates the status of the given learningPath")
      .in(pathLearningpathId / "status")
      .in(jsonBody[UpdateLearningPathStatus])
      .out(jsonBody[LearningPathV2])
      .errorOut(errorOutputsFor(400, 403, 404, 500))
      .requirePermission()
      .serverLogicPure { user =>
        { case (pathId, updateLearningPathStatus) =>
          val pathStatus = domain.LearningPathStatus.valueOfOrError(updateLearningPathStatus.status)
          updateService
            .updateLearningPathStatusV2(pathId, pathStatus, user, DefaultLanguage, updateLearningPathStatus.message)
            .map(learningPath => {
              logger.info(s"UPDATED status of LearningPath with ID = ${learningPath.id}")
              learningPath
            })
            .handleErrorsOrOk
        }
      }

    def withStatus: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch all learningpaths with specified status")
      .description("Fetch all learningpaths with specified status")
      .in("status" / learningPathStatus)
      .out(jsonBody[List[LearningPathV2]])
      .errorOut(errorOutputsFor(400, 401, 403, 500))
      .withOptionalUser
      .serverLogicPure { maybeUser =>
        { case status =>
          val user = maybeUser.getOrElse(TokenUser.PublicUser)
          readService.learningPathWithStatus(status, user).handleErrorsOrOk
        }
      }

    def deleteLearningpath: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Delete given learningpath")
      .description("Deletes the given learningPath")
      .in(pathLearningpathId)
      .out(statusCode(StatusCode.NoContent).and(emptyOutput))
      .errorOut(errorOutputsFor(403, 404, 500))
      .requirePermission()
      .serverLogicPure { user => pathId =>
        updateService.updateLearningPathStatusV2(
          pathId,
          domain.LearningPathStatus.DELETED,
          user,
          DefaultLanguage
        ) match {
          case Failure(ex) => returnLeftError(ex)
          case Success(_) =>
            logger.info(s"MARKED LearningPath with ID: $pathId as DELETED")
            ().asRight
        }
      }

    def deleteLearningStep: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Delete given learningstep")
      .description("Deletes the given learningStep")
      .in(pathLearningpathId / "learningsteps" / pathLearningstepId)
      .out(statusCode(StatusCode.NoContent).and(emptyOutput))
      .errorOut(errorOutputsFor(403, 404, 500))
      .requirePermission()
      .serverLogicPure { user =>
        { case (pathId, stepId) =>
          updateService.updateLearningStepStatusV2(pathId, stepId, StepStatus.DELETED, user) match {
            case Failure(ex) => returnLeftError(ex)
            case Success(_) =>
              logger.info(s"MARKED LearningStep with id: $stepId for LearningPath with id: $pathId as DELETED")
              ().asRight
          }
        }
      }

    def getTags: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch all previously used tags in learningpaths")
      .description("Retrieves a list of all previously used tags in learningpaths")
      .in("tags")
      .in(language)
      .in(fallback)
      .out(jsonBody[LearningPathTagsSummary])
      .errorOut(errorOutputsFor(500))
      .serverLogicPure { case (language, fallback) =>
        val allTags = readService.tags
        converterService.asApiLearningPathTagsSummary(allTags, language, fallback) match {
          case Some(s) => s.asRight
          case None    => notFoundWithMsg(s"Tags with language '$language' not found").asLeft
        }
      }

    def getContributors: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch all previously used contributors in learningpaths")
      .description("Retrieves a list of all previously used contributors in learningpaths")
      .in("contributors")
      .out(jsonBody[List[Author]])
      .errorOut(errorOutputsFor())
      .serverLogicPure { _ =>
        readService.contributors.asRight
      }

    def updateLearningPathTaxonomy: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Update taxonomy for specified learningpath")
      .description("Update taxonomy for specified learningpath")
      .in(pathLearningpathId / "update-taxonomy")
      .in(language)
      .in(fallback)
      .in(createResourceIfMissing)
      .out(jsonBody[LearningPathV2])
      .errorOut(errorOutputsFor(403, 404, 500))
      .requirePermission()
      .serverLogicPure { userInfo =>
        { case (pathId, language, fallback, createResourceIfMissing) =>
          updateService
            .updateTaxonomyForLearningPath(
              pathId,
              createResourceIfMissing,
              language,
              fallback,
              userInfo
            )
            .handleErrorsOrOk
        }
      }

    def fetchLearningPathContainingArticle: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch learningpaths containing specified article")
      .description("Fetch learningpaths containing specified article")
      .in("contains-article" / pathArticleId)
      .out(jsonBody[Seq[LearningPathSummaryV2]])
      .errorOut(errorOutputsFor(400, 500))
      .serverLogicPure { articleId =>
        val nodes = taxonomyApiClient.queryNodes(articleId).getOrElse(List.empty).flatMap(_.paths)
        val plainPaths = List(
          s"/article-iframe/*/$articleId",
          s"/article-iframe/*/$articleId/",
          s"/article-iframe/*/$articleId/\\?*",
          s"/article-iframe/*/$articleId\\?*",
          s"/article/$articleId"
        )
        val paths = nodes ++ plainPaths

        searchService.containsPath(paths) match {
          case Success(result) => result.results.asRight
          case Failure(ex)     => returnLeftError(ex)
        }
      }
  }
}
