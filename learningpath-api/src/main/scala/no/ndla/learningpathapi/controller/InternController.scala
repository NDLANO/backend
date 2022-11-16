/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import enumeratum.Json4s
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.{SearchIndexService, SearchService}
import no.ndla.learningpathapi.service.{ReadService, UpdateService}
import no.ndla.common.errors.AccessDeniedException
import no.ndla.network.AuthUser
import org.json4s.Formats
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.scalatra._
import org.scalatra.swagger.Swagger

import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success}

trait InternController {
  this: SearchIndexService
    with SearchService
    with LearningPathRepositoryComponent
    with ReadService
    with UpdateService
    with NdlaController
    with Props =>
  val internController: InternController

  class InternController(implicit val swagger: Swagger) extends NdlaController {
    protected val applicationDescription = "API for accessing internal functionality in learningpath API"
    protected implicit override val jsonFormats: Formats =
      org.json4s.DefaultFormats +
        new EnumNameSerializer(LearningPathStatus) +
        new EnumNameSerializer(LearningPathVerificationStatus) +
        new EnumNameSerializer(StepType) +
        Json4s.serializer(StepStatus) +
        new EnumNameSerializer(EmbedType) ++ JavaTimeSerializers.all

    def requireClientId(implicit request: HttpServletRequest): String = {
      AuthUser.getClientId match {
        case Some(clientId) => clientId
        case None => {
          logger.warn(s"Request made to ${request.getRequestURI} without clientId")
          throw AccessDeniedException("You do not have access to the requested resource.")
        }
      }
    }

    get("/id/:external_id") {
      val externalId = params("external_id")
      learningPathRepository.getIdFromExternalId(externalId) match {
        case Some(id) => id.toString
        case None     => NotFound()
      }
    }

    post("/index") {
      searchIndexService.indexDocuments match {
        case Success(reindexResult) =>
          val result =
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        case Failure(f) =>
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
      }
    }

    delete("/index") {
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
      searchIndexService
        .findAllIndexes(props.SearchIndex)
        .map(indexes => {
          indexes.map(index => {
            logger.info(s"Deleting index $index")
            searchIndexService.deleteIndexWithName(Option(index))
          })
        }) match {
        case Failure(ex) => InternalServerError(ex.getMessage)
        case Success(deleteResults) =>
          val (errors, successes) = deleteResults.partition(_.isFailure)
          if (errors.nonEmpty) {
            val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
              s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
              s"${pluralIndex(successes.length)} were deleted successfully."
            InternalServerError(body = message)
          } else {
            Ok(body = s"Deleted ${pluralIndex(successes.length)}")
          }
      }
    }

    get("/dump/learningpath/?") {
      val pageNo               = intOrDefault("page", 1)
      val pageSize             = intOrDefault("page-size", 250)
      val onlyIncludePublished = booleanOrDefault("only-published", true)

      readService.getLearningPathDomainDump(pageNo, pageSize, onlyIncludePublished)
    }

    post("/dump/learningpath/?") {
      val dumpToInsert = extract[domain.LearningPath](request.body)
      updateService.insertDump(dumpToInsert)
    }

    get("/containsArticle") {
      val paths = paramAsListOfString("paths")

      searchService.containsPath(paths) match {
        case Success(result) => result.results
        case Failure(ex)     => errorHandler(ex)
      }
    }

  }
}
