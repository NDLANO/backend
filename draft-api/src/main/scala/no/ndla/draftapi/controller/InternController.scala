/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.draftapi.Props
import no.ndla.draftapi.integration.ArticleApiClient
import no.ndla.draftapi.model.api.{ContentId, NotFoundException}
import no.ndla.draftapi.model.domain.{DBArticle, ReindexResult}
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.draftapi.service._
import no.ndla.draftapi.service.search._
import no.ndla.language.Language
import org.json4s.Formats
import org.scalatra.swagger.Swagger
import org.scalatra.{InternalServerError, NotFound, Ok}
import cats.implicits._
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import scalikejdbc.ReadOnlyAutoSession

import java.util.concurrent.{Executors, TimeUnit}
import scala.annotation.tailrec
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait InternController {
  this: ReadService
    with WriteService
    with ConverterService
    with DraftRepository
    with IndexService
    with ArticleIndexService
    with TagIndexService
    with GrepCodesIndexService
    with ArticleApiClient
    with NdlaController
    with DBArticle
    with Props =>
  val internController: InternController

  class InternController(implicit val swagger: Swagger) extends NdlaController {
    import props.{DraftSearchIndex, DraftTagSearchIndex, DraftGrepCodesSearchIndex}

    protected val applicationDescription                 = "API for accessing internal functionality in draft API"
    protected implicit override val jsonFormats: Formats = Draft.jsonEncoder

    def createIndexFuture(
        indexService: IndexService[_, _],
        numShards: Option[Int]
    )(implicit ec: ExecutionContext): Future[Try[ReindexResult]] = {

      val fut = Future { indexService.indexDocuments(numShards) }

      val logEx = (ex: Throwable) =>
        logger.error(s"Something went wrong when indexing ${indexService.documentType}:", ex)

      fut.onComplete {
        case Success(Success(result)) =>
          logger.info(
            s"Successfully indexed ${result.totalIndexed} ${indexService.documentType}'s in ${result.millisUsed}ms"
          )
        case Failure(ex)          => logEx(ex)
        case Success(Failure(ex)) => logEx(ex)
      }

      fut
    }

    post("/index") {
      val numShards = intOrNone("numShards")
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
      val articleIndex = createIndexFuture(articleIndexService, numShards)
      val tagIndex     = createIndexFuture(tagIndexService, numShards)
      val grepIndex    = createIndexFuture(grepCodesIndexService, numShards)
      val indexResults = Future.sequence(List(articleIndex, tagIndex, grepIndex))

      Await.result(indexResults, Duration.Inf).sequence match {
        case Failure(ex) =>
          logger.warn(ex.getMessage, ex)
          InternalServerError(ex.getMessage)
        case Success(results) =>
          val maxTime = results.map(rr => rr.millisUsed).max
          val result =
            s"Completed all indexes in ${maxTime} ms."
          logger.info(result)
          Ok(result)
      }
    }: Unit

    delete("/index") {
      implicit val ec         = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"

      val indexes = for {
        articleIndex <- Future { articleIndexService.findAllIndexes(DraftSearchIndex) }
        tagIndex     <- Future { tagIndexService.findAllIndexes(DraftTagSearchIndex) }
        grepIndex    <- Future { grepCodesIndexService.findAllIndexes(DraftGrepCodesSearchIndex) }
      } yield (articleIndex, tagIndex, grepIndex)

      val deleteResults: Seq[Try[_]] = Await.result(indexes, Duration(10, TimeUnit.MINUTES)) match {
        case (Failure(articleFail), _, _) => halt(status = 500, body = articleFail.getMessage)
        case (_, Failure(tagFail), _)     => halt(status = 500, body = tagFail.getMessage)
        case (_, _, Failure(grepFail))    => halt(status = 500, body = grepFail.getMessage)
        case (Success(articleIndexes), Success(tagIndexes), Success(grepIndexes)) =>
          val articleDeleteResults = articleIndexes.map(index => {
            logger.info(s"Deleting article index $index")
            articleIndexService.deleteIndexWithName(Option(index))
          })
          val tagDeleteResults = tagIndexes.map(index => {
            logger.info(s"Deleting tag index $index")
            tagIndexService.deleteIndexWithName(Option(index))
          })
          val grepDeleteResults = grepIndexes.map(index => {
            logger.info(s"Deleting grep index $index")
            grepCodesIndexService.deleteIndexWithName(Option(index))
          })
          articleDeleteResults ++ tagDeleteResults ++ grepDeleteResults
      }

      val (errors, successes) = deleteResults.partition(_.isFailure)
      if (errors.nonEmpty) {
        val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
          s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
          s"${pluralIndex(successes.length)} were deleted successfully."
        halt(status = 500, body = message)
      } else {
        Ok(body = s"Deleted ${pluralIndex(successes.length)}")
      }

    }: Unit

    get("/ids") {
      paramOrNone("status").map(DraftStatus.valueOfOrError) match {
        case Some(Success(status)) => draftRepository.idsWithStatus(status)(ReadOnlyAutoSession).getOrElse(List.empty)
        case Some(Failure(ex))     => errorHandler(ex)
        case None                  => draftRepository.getAllIds(ReadOnlyAutoSession)
      }
    }: Unit

    get("/import-id/:external_id") {
      val articleId = params("external_id")
      readService.importIdOfArticle(articleId) match {
        case Some(ids) => Ok(ids)
        case _         => NotFound()
      }
    }: Unit

    get("/id/:external_id") {
      val externalId = params("external_id")
      draftRepository.getIdFromExternalId(externalId)(ReadOnlyAutoSession) match {
        case Some(id) => id
        case None     => NotFound()
      }
    }: Unit

    get("/articles") {
      val pageNo   = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)
      val lang     = paramOrDefault("language", Language.AllLanguages)
      val fallback = booleanOrDefault("fallback", default = false)

      readService.getArticlesByPage(pageNo, pageSize, lang, fallback)
    }: Unit

    @tailrec
    private def deleteArticleWithRetries(
        id: Long,
        user: TokenUser,
        maxRetries: Int = 10,
        retries: Int = 0
    ): Try[ContentId] = {
      articleApiClient.deleteArticle(id, user) match {
        case Failure(_) if retries <= maxRetries => deleteArticleWithRetries(id, user, maxRetries, retries + 1)
        case Failure(ex)                         => Failure(ex)
        case Success(x)                          => Success(x)
      }
    }

    delete("/article/:id/") {
      requirePermissionOrAccessDeniedWithUser(DRAFT_API_WRITE) { user =>
        val id = long("id")
        deleteArticleWithRetries(id, user).flatMap(id => writeService.deleteArticle(id.id)) match {
          case Success(a)  => a
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit

    get("/dump/article/?") {
      // Dumps all domain articles
      val pageNo   = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)

      readService.getArticleDomainDump(pageNo, pageSize)
    }: Unit

    get("/dump/article/:id") {
      // Dumps one domain article
      val id = long("id")
      draftRepository.withId(id)(ReadOnlyAutoSession) match {
        case Some(article) => Ok(article)
        case None          => errorHandler(NotFoundException(s"Could not find draft with id: '$id"))
      }
    }: Unit

    post("/dump/article/?") {
      tryExtract[Draft](request.body) match {
        case Failure(ex) => errorHandler(ex)
        case Success(article) =>
          writeService.insertDump(article) match {
            case Failure(ex)       => errorHandler(ex)
            case Success(inserted) => Ok(inserted)
          }
      }

    }: Unit

  }
}
