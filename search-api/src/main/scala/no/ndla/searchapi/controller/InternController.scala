/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.controller

import cats.implicits.{catsSyntaxEitherId, toTraverseOps}
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.Content
import no.ndla.common.model.domain.concept.Concept
import no.ndla.network.model.RequestInfo
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.{AllErrors, Service}
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.searchapi.{Eff, Props}
import no.ndla.searchapi.integration.{GrepApiClient, MyNDLAApiClient, TaxonomyApiClient}
import no.ndla.searchapi.model.api.ErrorHelpers
import no.ndla.searchapi.model.domain.{IndexingBundle, ReindexResult}
import no.ndla.searchapi.model.domain.learningpath.LearningPath
import no.ndla.searchapi.model.search.SearchType
import no.ndla.searchapi.service.search.{
  ArticleIndexService,
  DraftConceptIndexService,
  DraftIndexService,
  IndexService,
  LearningPathIndexService
}
import sttp.model.StatusCode

import java.util.concurrent.{Executors, TimeUnit}
import sttp.tapir.generic.auto.*

import scala.concurrent.*
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

trait InternController {
  this: IndexService
    with ArticleIndexService
    with LearningPathIndexService
    with DraftIndexService
    with DraftConceptIndexService
    with TaxonomyApiClient
    with GrepApiClient
    with Props
    with ErrorHelpers
    with MyNDLAApiClient =>
  val internController: InternController

  class InternController extends Service[Eff] with StrictLogging {
    import ErrorHelpers._

    implicit val ec: ExecutionContext =
      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(SearchType.values.size))

    override val prefix: EndpointInput[Unit] = "intern"
    override val enableSwagger               = false
    private val stringInternalServerError    = statusCode(StatusCode.InternalServerError).and(stringBody)

    private def resolveResultFutures(
        indexResults: List[Future[(String, Try[ReindexResult])]]
    ): Either[String, String] = {

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
          resultString.asRight
        case failures =>
          val failedIndexResults = failures
            .map({ case (name: String, failure: Throwable) =>
              logger.error(s"Failed to index $name: ${failure.getMessage}.", failure)
              s"$name: ${failure.getMessage}"
            })
            .mkString(", and ")

          failedIndexResults.asLeft
      }
    }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      reindexShards,
      reindexReplicas,
      postIndex,
      deleteDocument,
      indexSingleDocument,
      reindexById,
      reindexArticle,
      reindexDraft,
      reindexLearningpath,
      reindexConcept
    )

    def deleteDocument: ServerEndpoint[Any, Eff] = endpoint.delete
      .in(path[String]("type") / path[Long]("id"))
      .out(emptyOutput)
      .errorOut(errorOutputsFor(400))
      .serverLogicPure { case (indexType, documentId) =>
        (indexType match {
          case articleIndexService.documentType      => articleIndexService.deleteDocument(documentId)
          case draftIndexService.documentType        => draftIndexService.deleteDocument(documentId)
          case learningPathIndexService.documentType => learningPathIndexService.deleteDocument(documentId)
          case draftConceptIndexService.documentType => draftConceptIndexService.deleteDocument(documentId)
          case _                                     => Success(())
        }).map(_ => ()).handleErrorsOrOk
      }

    private def parseBody[T: Decoder](body: String): Try[T] = {
      CirceUtil
        .tryParseAs[T](body)
        .recoverWith { case _ => Failure(InvalidIndexBodyException()) }
    }

    private def indexRequestWithService[T <: Content: Decoder](
        indexService: IndexService[T],
        body: String
    ): Either[AllErrors, T] = {
      parseBody[T](body).flatMap(x => indexService.indexDocument(x)) match {
        case Success(doc) => doc.asRight
        case Failure(ex) =>
          logger.error("Could not index document...", ex)
          returnLeftError(ex)
      }
    }

    def indexSingleDocument: ServerEndpoint[Any, Eff] = endpoint.post
      .in(path[String]("type"))
      .in(stringBody)
      .out(
        oneOf[Content](
          oneOfVariant(jsonBody[Article]),
          oneOfVariant(jsonBody[Draft]),
          oneOfVariant(jsonBody[LearningPath]),
          oneOfVariant(jsonBody[Concept])
        )
      )
      .errorOut(errorOutputsFor(400))
      .serverLogicPure { case (indexType, body) =>
        indexType match {
          case articleIndexService.documentType      => indexRequestWithService(articleIndexService, body)
          case draftIndexService.documentType        => indexRequestWithService(draftIndexService, body)
          case learningPathIndexService.documentType => indexRequestWithService(learningPathIndexService, body)
          case draftConceptIndexService.documentType => indexRequestWithService(draftConceptIndexService, body)
          case _ =>
            badRequest(
              s"Bad type passed to POST /:type/, must be one of: '${articleIndexService.documentType}', '${draftIndexService.documentType}', '${learningPathIndexService.documentType}', '${draftConceptIndexService.documentType}'"
            ).asLeft
        }
      }

    def reindexById: ServerEndpoint[Any, Eff] = endpoint.post
      .in("reindex" / path[String]("type") / path[Long]("id"))
      .errorOut(errorOutputsFor(400))
      .out(
        oneOf[Content](
          oneOfVariant(jsonBody[Article]),
          oneOfVariant(jsonBody[Draft]),
          oneOfVariant(jsonBody[LearningPath]),
          oneOfVariant(jsonBody[Concept])
        )
      )
      .serverLogicPure { case (indexType, id) =>
        indexType match {
          case articleIndexService.documentType      => articleIndexService.reindexDocument(id).handleErrorsOrOk
          case draftIndexService.documentType        => draftIndexService.reindexDocument(id).handleErrorsOrOk
          case learningPathIndexService.documentType => learningPathIndexService.reindexDocument(id).handleErrorsOrOk
          case draftConceptIndexService.documentType => draftConceptIndexService.reindexDocument(id).handleErrorsOrOk
          case _ =>
            badRequest(
              s"Bad type passed to POST /:type/:id, must be one of: '${articleIndexService.documentType}', '${draftIndexService.documentType}', '${learningPathIndexService.documentType}', '${draftConceptIndexService.documentType}'"
            ).asLeft
        }
      }

    def reindexDraft: ServerEndpoint[Any, Eff] = endpoint.post
      .in("index" / "draft")
      .in(query[Option[Int]]("numShards"))
      .errorOut(stringInternalServerError)
      .out(stringBody)
      .serverLogicPure { numShards =>
        val requestInfo = RequestInfo.fromThreadContext()
        val draftIndex = Future {
          requestInfo.setThreadContextRequestInfo()
          ("drafts", draftIndexService.indexDocuments(shouldUsePublishedTax = false, numShards))
        }

        resolveResultFutures(List(draftIndex))
      }

    def reindexConcept: ServerEndpoint[Any, Eff] = endpoint.post
      .in("index" / "concept")
      .in(query[Option[Int]]("numShards"))
      .errorOut(stringInternalServerError)
      .out(stringBody)
      .serverLogicPure { numShards =>
        val requestInfo = RequestInfo.fromThreadContext()
        val conceptIndex = Future {
          requestInfo.setThreadContextRequestInfo()
          ("concepts", draftConceptIndexService.indexDocuments(shouldUsePublishedTax = false, numShards))
        }

        resolveResultFutures(List(conceptIndex))
      }

    def reindexArticle: ServerEndpoint[Any, Eff] = endpoint.post
      .in("index" / "article")
      .in(query[Option[Int]]("numShards"))
      .errorOut(stringInternalServerError)
      .out(stringBody)
      .serverLogicPure { numShards =>
        val requestInfo = RequestInfo.fromThreadContext()
        val articleIndex = Future {
          requestInfo.setThreadContextRequestInfo()
          ("articles", articleIndexService.indexDocuments(shouldUsePublishedTax = true, numShards))
        }

        resolveResultFutures(List(articleIndex))
      }

    def reindexLearningpath: ServerEndpoint[Any, Eff] = endpoint.post
      .in("index" / "learningpath")
      .in(query[Option[Int]]("numShards"))
      .errorOut(stringInternalServerError)
      .out(stringBody)
      .serverLogicPure { numShards =>
        val requestInfo = RequestInfo.fromThreadContext()
        val learningPathIndex = Future {
          requestInfo.setThreadContextRequestInfo()
          ("learningpaths", learningPathIndexService.indexDocuments(shouldUsePublishedTax = true, numShards))
        }

        resolveResultFutures(List(learningPathIndex))
      }

    def reindexShards: ServerEndpoint[Any, Eff] = endpoint.post
      .in("reindex" / "shards" / path[Int]("num_shards"))
      .errorOut(errorOutputsFor(400))
      .out(stringBody)
      .serverLogicPure { case (numShards) =>
        val startTime = System.currentTimeMillis()
        logger.info("Cleaning up unreferenced indexes before reindexing...")
        articleIndexService.cleanupIndexes(): Unit
        draftIndexService.cleanupIndexes(): Unit
        learningPathIndexService.cleanupIndexes(): Unit
        draftConceptIndexService.cleanupIndexes(): Unit

        val articles      = articleIndexService.reindexWithShards(numShards)
        val drafts        = draftIndexService.reindexWithShards(numShards)
        val learningpaths = learningPathIndexService.reindexWithShards(numShards)
        val concept       = draftConceptIndexService.reindexWithShards(numShards)
        List(articles, drafts, learningpaths, concept).sequence match {
          case Success(_) =>
            s"Reindexing with $numShards shards completed in ${System.currentTimeMillis() - startTime}ms".asRight
          case Failure(ex) =>
            logger.error("Could not reindex with shards...", ex)
            returnLeftError(ex)
        }
      }

    def reindexReplicas: ServerEndpoint[Any, Eff] = endpoint.post
      .in("reindex" / "replicas" / path[Int]("num_replicas"))
      .errorOut(errorOutputsFor(400))
      .out(stringBody)
      .serverLogicPure { case (numReplicas) =>
        logger.info("Cleaning up unreferenced indexes before updating replications setting...")
        articleIndexService.cleanupIndexes(): Unit
        draftIndexService.cleanupIndexes(): Unit
        learningPathIndexService.cleanupIndexes(): Unit
        draftConceptIndexService.cleanupIndexes(): Unit

        val articles      = articleIndexService.updateReplicaNumber(numReplicas)
        val drafts        = draftIndexService.updateReplicaNumber(numReplicas)
        val learningpaths = learningPathIndexService.updateReplicaNumber(numReplicas)
        val concepts      = draftConceptIndexService.updateReplicaNumber(numReplicas)
        List(articles, drafts, learningpaths, concepts).sequence match {
          case Success(_) =>
            s"Updated replication setting for indexes to $numReplicas replicas. Populating may take some time.".asRight
          case Failure(ex) =>
            logger.error("Could not update replication settings", ex)
            returnLeftError(ex)
        }
      }

    def postIndex: ServerEndpoint[Any, Eff] = endpoint.post
      .in("index")
      .in(query[Boolean]("run-in-background").default(false))
      .in(query[Option[Int]]("numShards"))
      .errorOut(errorOutputsFor(400))
      .out(
        oneOf[Option[String]](
          oneOfVariantValueMatcher(statusCode(StatusCode.Ok).and(jsonBody[Option[String]])) { case Some(_) => true },
          oneOfVariantValueMatcher(statusCode(StatusCode.Accepted).and(emptyOutputAs[Option[String]](None))) {
            case None =>
              true
          }
        )
      )
      .serverLogicPure { case (runInBackground, numShards) =>
        val bundles = for {
          taxonomyBundleDraft     <- taxonomyApiClient.getTaxonomyBundle(false)
          taxonomyBundlePublished <- taxonomyApiClient.getTaxonomyBundle(true)
          grepBundle              <- grepApiClient.getGrepBundle()
          myndlaBundle            <- myndlaapiClient.getMyNDLABundle
        } yield (taxonomyBundleDraft, taxonomyBundlePublished, grepBundle, myndlaBundle)

        val start = System.currentTimeMillis()

        bundles match {
          case Failure(ex) => returnLeftError(ex)
          case Success((taxonomyBundleDraft, taxonomyBundlePublished, grepBundle, myndlaBundle)) =>
            logger.info("Cleaning up unreferenced indexes before reindexing...")
            learningPathIndexService.cleanupIndexes(): Unit
            articleIndexService.cleanupIndexes(): Unit
            draftIndexService.cleanupIndexes(): Unit
            draftConceptIndexService.cleanupIndexes(): Unit

            val publishedIndexingBundle = IndexingBundle(
              grepBundle = Some(grepBundle),
              taxonomyBundle = Some(taxonomyBundlePublished),
              myndlaBundle = Some(myndlaBundle)
            )

            val draftIndexingBundle = IndexingBundle(
              grepBundle = Some(grepBundle),
              taxonomyBundle = Some(taxonomyBundleDraft),
              myndlaBundle = Some(myndlaBundle)
            )

            val requestInfo = RequestInfo.fromThreadContext()
            val indexes = List(
              Future {
                requestInfo.setThreadContextRequestInfo()
                ("learningpaths", learningPathIndexService.indexDocuments(numShards, publishedIndexingBundle))
              },
              Future {
                requestInfo.setThreadContextRequestInfo()
                ("articles", articleIndexService.indexDocuments(numShards, publishedIndexingBundle))
              },
              Future {
                requestInfo.setThreadContextRequestInfo()
                ("drafts", draftIndexService.indexDocuments(numShards, draftIndexingBundle))
              },
              Future {
                requestInfo.setThreadContextRequestInfo()
                ("concepts", draftConceptIndexService.indexDocuments(numShards, draftIndexingBundle))
              }
            )
            if (runInBackground) {
              None.asRight
            } else {
              val out = resolveResultFutures(indexes)
              logger.info(s"Reindexing all indexes took ${System.currentTimeMillis() - start} ms...")
              out match {
                case Left(value)  => errorBody("GENERIC", value, 500).asLeft
                case Right(value) => Some(value).asRight
              }
            }
        }
      }
  }
}
