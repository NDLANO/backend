/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.controller

import cats.implicits._
import no.ndla.conceptapi.Eff
import no.ndla.conceptapi.model.api.{ConceptDomainDump, ConceptImportResults, ErrorHelpers, NotFoundException}
import no.ndla.conceptapi.model.domain.Concept
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.search.{DraftConceptIndexService, IndexService, PublishedConceptIndexService}
import no.ndla.conceptapi.service.{ConverterService, ImportService, ReadService}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import sttp.tapir._
import sttp.tapir.generic.auto._

trait InternController {
  this: IndexService
    with DraftConceptIndexService
    with PublishedConceptIndexService
    with ImportService
    with ConverterService
    with ReadService
    with DraftConceptRepository
    with PublishedConceptRepository
    with ErrorHelpers =>
  val internController: InternController

  class InternController extends Service[Eff] {
    import ErrorHelpers._

    override val prefix: EndpointInput[Unit] = "intern"
    override val enableSwagger               = false

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      postIndex,
      deleteIndex,
      dumpDraftConcept,
      dumpSingleDraftConcept,
      dumpPublishedConcept,
      dumpSinglePublishedConcept,
      postDraftConcept,
      importConcept
    )

    def postIndex: ServerEndpoint[Any, Eff] = endpoint.post
      .in("index")
      .in(query[Option[Int]]("numShards"))
      .out(stringBody)
      .errorOut(errorOutputsFor(400))
      .serverLogicPure { numShards =>
        implicit val ec: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

        val aggregateFuture = for {
          draftFuture     <- Future(draftConceptIndexService.indexDocuments(numShards))
          publishedFuture <- Future(publishedConceptIndexService.indexDocuments(numShards))
        } yield (draftFuture, publishedFuture)

        Await.result(aggregateFuture, 10 minutes) match {
          case (Success(draftReindex), Success(publishedReindex)) =>
            val msg =
              s"""Completed indexing of ${draftReindex.totalIndexed} draft concepts in ${draftReindex.millisUsed} ms.
                 |Completed indexing of ${publishedReindex.totalIndexed} published concepts in ${publishedReindex.millisUsed} ms.
                 |""".stripMargin
            logger.info(msg)
            msg.asRight
          case (Failure(ex), _) =>
            logger.error(s"Reindexing draft concepts failed with ${ex.getMessage}", ex)
            returnLeftError(ex)
          case (_, Failure(ex)) =>
            logger.error(s"Reindexing published concepts failed with ${ex.getMessage}", ex)
            returnLeftError(ex)
        }
      }

    def deleteIndexes[T <: IndexService](indexService: T): Try[String] = {
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
      indexService.findAllIndexes match {
        case Failure(ex) =>
          logger.error("Could not find indexes to delete.")
          Failure(ex)
        case Success(indexesToDelete) =>
          val deleted             = indexesToDelete.map(index => indexService.deleteIndexWithName(Some(index)))
          val (successes, errors) = deleted.partition(_.isSuccess)
          if (errors.nonEmpty) {
            val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
              s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
              s"${pluralIndex(successes.length)} were deleted successfully."
            Failure(new RuntimeException(message))
          } else {
            Success(s"Deleted ${pluralIndex(successes.length)}")
          }
      }
    }

    def deleteIndex: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("index")
      .out(stringBody)
      .errorOut(statusCode(StatusCode.InternalServerError).and(stringBody))
      .serverLogicPure { _ =>
        def logDeleteResult(t: Try[String]) = {
          t match {
            case Failure(ex) =>
              logger.error(ex.getMessage)
              ex.getMessage
            case Success(msg) =>
              logger.info(msg)
              msg
          }
        }

        val result1 = deleteIndexes(draftConceptIndexService)
        val result2 = deleteIndexes(publishedConceptIndexService)

        val msg =
          s"""${logDeleteResult(result1)}
             |${logDeleteResult(result2)}""".stripMargin

        if (result1.isFailure || result2.isFailure) msg.asLeft
        else msg.asRight
      }

    def dumpDraftConcept: ServerEndpoint[Any, Eff] = endpoint.get
      .in("dump" / "draft-concept")
      .out(jsonBody[ConceptDomainDump])
      .in(query[Int]("page").default(1))
      .in(query[Int]("page-size").default(250))
      .serverLogicPure { case (pageNo, pageSize) =>
        readService.getDraftConceptDomainDump(pageNo, pageSize).asRight
      }

    def dumpSingleDraftConcept: ServerEndpoint[Any, Eff] = endpoint.get
      .in("dump" / "draft-concept" / path[Long]("id"))
      .out(jsonBody[Concept])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { id =>
        draftConceptRepository.withId(id) match {
          case Some(concept) => concept.asRight
          case None          => returnLeftError(NotFoundException(s"Could not find draft concept with id '$id'"))
        }
      }

    def dumpPublishedConcept: ServerEndpoint[Any, Eff] = endpoint.get
      .in("dump" / "concept")
      .out(jsonBody[ConceptDomainDump])
      .in(query[Int]("page").default(1))
      .in(query[Int]("page-size").default(250))
      .serverLogicPure { case (pageNo, pageSize) =>
        readService.getPublishedConceptDomainDump(pageNo, pageSize).asRight
      }

    def dumpSinglePublishedConcept: ServerEndpoint[Any, Eff] = endpoint.get
      .in("dump" / "concept" / path[Long]("id"))
      .out(jsonBody[Concept])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { id =>
        publishedConceptRepository.withId(id) match {
          case Some(concept) => concept.asRight
          case None          => returnLeftError(NotFoundException(s"Could not find published concept with id '$id'"))
        }
      }

    def postDraftConcept: ServerEndpoint[Any, Eff] = endpoint.post
      .in("dump" / "draft-concept")
      .in(jsonBody[Concept])
      .out(jsonBody[Concept])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { concept =>
        draftConceptRepository.insert(concept).asRight
      }

    def importConcept: ServerEndpoint[Any, Eff] = endpoint.post
      .in("import" / "concept")
      .out(jsonBody[ConceptImportResults])
      .errorOut(statusCode(StatusCode.InternalServerError).and(jsonBody[ConceptImportResults]))
      .errorOutEither(statusCode(StatusCode.InternalServerError).and(stringBody))
      .in(query[Boolean]("forceUpdate").default(false))
      .withOptionalUser
      .serverLogicPure {
        user =>
          { forceUpdate =>
            val start = System.currentTimeMillis
            importService.importConcepts(forceUpdate, user.get) match {
              case Success(result) =>
                if (result.numSuccessfullyImportedConcepts < result.totalAttemptedImportedConcepts) {
                  result.asLeft.asLeft
                } else {
                  result.asRight
                }
              case Failure(ex) =>
                val errMsg =
                  s"Import of concepts failed after ${System.currentTimeMillis - start} ms with error: ${ex.getMessage}\n"
                logger.warn(errMsg, ex)
                errMsg.asRight.asLeft
            }
          }
      }
  }
}
