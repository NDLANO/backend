/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.controller

import java.util.concurrent.Executors

import no.ndla.conceptapi.auth.{User, UserInfo}
import no.ndla.conceptapi.model.api.NotFoundException
import no.ndla.conceptapi.model.domain.Concept
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.search.{DraftConceptIndexService, IndexService, PublishedConceptIndexService}
import no.ndla.conceptapi.service.{ConverterService, ImportService, ReadService}
import org.json4s.Formats
import org.scalatra.swagger.Swagger
import org.scalatra.{InternalServerError, Ok, Unauthorized}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait InternController {
  this: IndexService
    with DraftConceptIndexService
    with PublishedConceptIndexService
    with ImportService
    with ConverterService
    with ReadService
    with User
    with DraftConceptRepository
    with PublishedConceptRepository =>
  val internController: InternController

  class InternController(implicit val swagger: Swagger) extends NdlaController {
    protected val applicationDescription = "API for accessing internal functionality in draft API"
    protected implicit override val jsonFormats: Formats = Concept.jsonEncoder

    post("/index") {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

      val aggregateFuture = for {
        draftFuture <- Future(draftConceptIndexService.indexDocuments)
        publishedFuture <- Future(publishedConceptIndexService.indexDocuments)
      } yield (draftFuture, publishedFuture)

      Await.result(aggregateFuture, 10 minutes) match {
        case (Success(draftReindex), Success(publishedReindex)) =>
          val msg =
            s"""Completed indexing of ${draftReindex.totalIndexed} draft concepts in ${draftReindex.millisUsed} ms.
               |Completed indexing of ${publishedReindex.totalIndexed} published concepts in ${publishedReindex.millisUsed} ms.
               |""".stripMargin
          logger.info(msg)
          Ok(msg)
        case (Failure(ex), _) =>
          logger.error(s"Reindexing draft concepts failed with ${ex.getMessage}", ex)
          errorHandler(ex)
        case (_, Failure(ex)) =>
          logger.error(s"Reindexing published concepts failed with ${ex.getMessage}", ex)
          errorHandler(ex)
      }
    }

    def deleteIndexes[T <: IndexService[_]](indexService: T) = {
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
      indexService.findAllIndexes match {
        case Failure(ex) =>
          logger.error("Could not find indexes to delete.")
          Failure(ex)
        case Success(indexesToDelete) =>
          val deleted = indexesToDelete.map(index => indexService.deleteIndexWithName(Some(index)))
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

    delete("/index") {
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

      if (result1.isFailure || result2.isFailure) InternalServerError(msg)
      else Ok(msg)
    }

    get("/dump/draft-concept/") {
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)

      readService.getDraftConceptDomainDump(pageNo, pageSize)
    }

    get("/dump/draft-concept/:id") {
      val id = long("id")
      draftConceptRepository.withId(id) match {
        case Some(concept) => Ok(concept)
        case None          => errorHandler(NotFoundException(s"Could not find draft concept with id '$id'"))
      }
    }

    get("/dump/concept/") {
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)

      readService.getPublishedConceptDomainDump(pageNo, pageSize)
    }

    get("/dump/concept/:id") {
      val id = long("id")
      publishedConceptRepository.withId(id) match {
        case Some(concept) => Ok(concept)
        case None          => errorHandler(NotFoundException(s"Could not find published concept with id '$id'"))
      }
    }

    post("/dump/draft-concept/") {
      val concept = extract[Concept](request.body)
      concept match {
        case Success(c) => Ok(draftConceptRepository.insert(c))
        case Failure(f) => errorHandler(f)
      }
    }

    post("/import/concept") {
      UserInfo.get match {
        case Some(user) if user.canWrite =>
          val start = System.currentTimeMillis
          val forceUpdate = booleanOrDefault("forceUpdate", default = false)

          importService.importConcepts(forceUpdate) match {
            case Success(result) =>
              if (result.numSuccessfullyImportedConcepts < result.totalAttemptedImportedConcepts) {
                InternalServerError(result)
              } else {
                Ok(result)
              }
            case Failure(ex) =>
              val errMsg =
                s"Import of concepts failed after ${System.currentTimeMillis - start} ms with error: ${ex.getMessage}\n"
              logger.warn(errMsg, ex)
              InternalServerError(body = errMsg)
          }
        case _ => Unauthorized("You do not have access to perform this action")
      }

    }

  }
}
