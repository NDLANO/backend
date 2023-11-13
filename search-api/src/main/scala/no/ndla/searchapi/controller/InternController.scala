/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.controller

import cats.implicits.toTraverseOps
import no.ndla.common.model.domain.Content
import no.ndla.network.model.RequestInfo
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.{GrepApiClient, TaxonomyApiClient}
import no.ndla.searchapi.model.api.ErrorHelpers
import no.ndla.searchapi.model.domain.ReindexResult
import no.ndla.searchapi.service.search.{ArticleIndexService, DraftIndexService, IndexService, LearningPathIndexService}
import org.scalatra._

import java.util.concurrent.{Executors, TimeUnit}
import javax.servlet.http.HttpServletRequest
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait InternController {
  this: IndexService
    with ArticleIndexService
    with LearningPathIndexService
    with DraftIndexService
    with TaxonomyApiClient
    with GrepApiClient
    with NdlaController
    with Props
    with ErrorHelpers =>
  val internController: InternController

  class InternController extends NdlaController {
    implicit val ec: ExecutionContext =
      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(props.SearchIndexes.size))

    private def resolveResultFutures(indexResults: List[Future[(String, Try[ReindexResult])]]): ActionResult = {

      val futureIndexed    = Future.sequence(indexResults)
      val completedIndexed = Await.result(futureIndexed, Duration(60, TimeUnit.MINUTES))

      completedIndexed.collect { case (name, Failure(ex)) => (name, ex) } match {
        case Nil =>
          val successful = completedIndexed.collect { case (name, Success(r)) => (name, r) }

          val indexResults = successful
            .map({ case (name: String, reindexResult: ReindexResult) =>
              s"${reindexResult.totalIndexed} $name in ${reindexResult.millisUsed} ms"
            })
            .mkString(", and ")
          val resultString = s"Completed indexing of $indexResults"

          logger.info(resultString)
          Ok(resultString)
        case failures =>
          val failedIndexResults = failures
            .map({ case (name: String, failure: Throwable) =>
              logger.error(s"Failed to index $name: ${failure.getMessage}.", failure)
              s"$name: ${failure.getMessage}"
            })
            .mkString(", and ")

          InternalServerError(failedIndexResults)
      }
    }

    delete("/:type/:id") {
      val indexType  = params("type")
      val documentId = long("id")

      indexType match {
        case articleIndexService.documentType      => articleIndexService.deleteDocument(documentId)
        case draftIndexService.documentType        => draftIndexService.deleteDocument(documentId)
        case learningPathIndexService.documentType => learningPathIndexService.deleteDocument(documentId)
        case _                                     =>
      }
    }: Unit

    private def parseBody[T](body: String)(implicit mf: Manifest[T]): Try[T] = {
      Try(parse(body).camelizeKeys.extract[T])
        .recoverWith { case _ => Failure(InvalidIndexBodyException()) }
    }

    private def indexRequestWithService[T <: Content](
        indexService: IndexService[T]
    )(implicit req: HttpServletRequest, mf: Manifest[T]) = {
      parseBody[T](req.body).flatMap(x => indexService.indexDocument(x)) match {
        case Success(doc) => Created(doc)
        case Failure(ex) =>
          logger.error("Could not index document...", ex)
          errorHandler(ex)
      }
    }

    post("/:type/") {
      val indexType = params("type")

      indexType match {
        case articleIndexService.documentType      => indexRequestWithService(articleIndexService)
        case draftIndexService.documentType        => indexRequestWithService(draftIndexService)
        case learningPathIndexService.documentType => indexRequestWithService(learningPathIndexService)
        case _ =>
          BadRequest(
            s"Bad type passed to POST /:type/, must be one of: '${articleIndexService.documentType}', '${draftIndexService.documentType}', '${learningPathIndexService.documentType}'"
          )
      }
    }: Unit

    post("/reindex/:type/:id") {
      val indexType = params("type")
      val id        = long("id")

      val Respond = (indexed: Try[Content]) => {
        indexed match {
          case Success(doc) => Created(doc)
          case Failure(ex) =>
            logger.error("Could not index document...", ex)
            errorHandler(ex)
        }
      }

      indexType match {
        case articleIndexService.documentType      => Respond(articleIndexService.reindexDocument(id))
        case draftIndexService.documentType        => Respond(draftIndexService.reindexDocument(id))
        case learningPathIndexService.documentType => Respond(learningPathIndexService.reindexDocument(id))
        case _ =>
          BadRequest(
            s"Bad type passed to POST /:type/:id, must be one of: '${articleIndexService.documentType}', '${draftIndexService.documentType}', '${learningPathIndexService.documentType}'"
          )
      }

    }: Unit

    post("/index/draft") {
      val requestInfo = RequestInfo.fromThreadContext()
      val numShards   = intOrNone("numShards")
      val draftIndex = Future {
        requestInfo.setThreadContextRequestInfo()
        ("drafts", draftIndexService.indexDocuments(shouldUsePublishedTax = false, numShards))
      }

      resolveResultFutures(List(draftIndex))
    }: Unit

    post("/index/article") {
      val requestInfo = RequestInfo.fromThreadContext()
      val articleIndex = Future {
        val numShards = intOrNone("numShards")
        requestInfo.setThreadContextRequestInfo()
        ("articles", articleIndexService.indexDocuments(shouldUsePublishedTax = true, numShards))
      }

      resolveResultFutures(List(articleIndex))
    }: Unit

    post("/index/learningpath") {
      val requestInfo = RequestInfo.fromThreadContext()
      val numShards   = intOrNone("numShards")
      val learningPathIndex = Future {
        requestInfo.setThreadContextRequestInfo()
        ("learningpaths", learningPathIndexService.indexDocuments(shouldUsePublishedTax = true, numShards))
      }

      resolveResultFutures(List(learningPathIndex))
    }: Unit

    post("/reindex/shards/:num_shards") {
      val startTime = System.currentTimeMillis()
      int("num_shards") match {
        case Failure(ex) => errorHandler(ex)
        case Success(numShards) =>
          logger.info("Cleaning up unreferenced indexes before reindexing...")
          articleIndexService.cleanupIndexes(): Unit
          draftIndexService.cleanupIndexes(): Unit
          learningPathIndexService.cleanupIndexes(): Unit

          val articles      = articleIndexService.reindexWithShards(numShards)
          val drafts        = draftIndexService.reindexWithShards(numShards)
          val learningpaths = learningPathIndexService.reindexWithShards(numShards)
          List(articles, drafts, learningpaths).sequence match {
            case Success(_) =>
              Ok(s"Reindexing with $numShards shards completed in ${System.currentTimeMillis() - startTime}ms")
            case Failure(ex) =>
              logger.error("Could not reindex with shards...", ex)
              errorHandler(ex)
          }
      }
    }: Unit

    post("/reindex/replicas/:num_replicas") {
      int("num_replicas") match {
        case Failure(ex) => errorHandler(ex)
        case Success(numReplicas) =>
          logger.info("Cleaning up unreferenced indexes before updating replications setting...")
          articleIndexService.cleanupIndexes(): Unit
          draftIndexService.cleanupIndexes(): Unit
          learningPathIndexService.cleanupIndexes(): Unit

          val articles      = articleIndexService.updateReplicaNumber(numReplicas)
          val drafts        = draftIndexService.updateReplicaNumber(numReplicas)
          val learningpaths = learningPathIndexService.updateReplicaNumber(numReplicas)
          List(articles, drafts, learningpaths).sequence match {
            case Success(_) =>
              Ok(s"Updated replication setting for indexes to $numReplicas replicas. Populating may take some time.")
            case Failure(ex) =>
              logger.error("Could not update replication settings", ex)
              errorHandler(ex)
          }
      }
    }: Unit

    post("/index") {
      val runInBackground = booleanOrDefault("run-in-background", default = false)
      val numShards       = intOrNone("numShards")
      val bundles = for {
        taxonomyBundleDraft     <- taxonomyApiClient.getTaxonomyBundle(false)
        taxonomyBundlePublished <- taxonomyApiClient.getTaxonomyBundle(true)
        grepBundle              <- grepApiClient.getGrepBundle()
      } yield (taxonomyBundleDraft, taxonomyBundlePublished, grepBundle)

      val start = System.currentTimeMillis()

      bundles match {
        case Failure(ex) => errorHandler(ex)
        case Success((taxonomyBundleDraft, taxonomyBundlePublished, grepBundle)) =>
          logger.info("Cleaning up unreferenced indexes before reindexing...")
          learningPathIndexService.cleanupIndexes(): Unit
          articleIndexService.cleanupIndexes(): Unit
          draftIndexService.cleanupIndexes(): Unit

          val requestInfo = RequestInfo.fromThreadContext()
          val indexes = List(
            Future {
              requestInfo.setThreadContextRequestInfo()
              ("learningpaths", learningPathIndexService.indexDocuments(taxonomyBundlePublished, grepBundle, numShards))
            },
            Future {
              requestInfo.setThreadContextRequestInfo()
              ("articles", articleIndexService.indexDocuments(taxonomyBundlePublished, grepBundle, numShards))
            },
            Future {
              requestInfo.setThreadContextRequestInfo()
              ("drafts", draftIndexService.indexDocuments(taxonomyBundleDraft, grepBundle, numShards))
            }
          )
          if (runInBackground) {
            Accepted("Starting indexing process...")
          } else {
            val out = resolveResultFutures(indexes)
            taxonomyBundleDraft.close()
            taxonomyBundlePublished.close()
            logger.info(s"Reindexing all indexes took ${System.currentTimeMillis() - start} ms...")
            out
          }
      }
    }: Unit

  }
}
