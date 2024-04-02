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
import no.ndla.learningpathapi.model.api.{ErrorHelpers, LearningPathDomainDump, LearningPathSummaryV2}
import no.ndla.learningpathapi.{Eff, Props}
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.{SearchIndexService, SearchService}
import no.ndla.learningpathapi.service.{ReadService, UpdateService}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success}

trait InternController {
  this: SearchIndexService
    with SearchService
    with LearningPathRepositoryComponent
    with ReadService
    with UpdateService
    with Props
    with ErrorHelpers =>
  val internController: InternController

  class InternController extends Service[Eff] {
    override val prefix: EndpointInput[Unit] = "intern"
    override val enableSwagger               = false
    private val stringInternalServerError    = statusCode(StatusCode.InternalServerError).and(stringBody)
    import ErrorHelpers._

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getByExternalId,
      postIndex,
      deleteIndex,
      dumpLearningpaths,
      dumpSingleLearningPath,
      postLearningPathDump,
      containsArticle
    )

    def getByExternalId: ServerEndpoint[Any, Eff] = endpoint.get
      .in("id" / path[String]("external_id"))
      .out(stringBody)
      .errorOut(errorOutputsFor(404))
      .serverLogicPure { externalId =>
        learningPathRepository.getIdFromExternalId(externalId) match {
          case Some(id) => id.toString.asRight
          case None     => notFound.asLeft
        }

      }

    def postIndex: ServerEndpoint[Any, Eff] = endpoint.post
      .in("index")
      .in(query[Option[Int]]("numShards"))
      .out(stringBody)
      .errorOut(stringInternalServerError)
      .serverLogicPure { numShards =>
        searchIndexService.indexDocuments(numShards) match {
          case Success(reindexResult) =>
            val result =
              s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
            logger.info(result)
            result.asRight
          case Failure(f) =>
            logger.warn(f.getMessage, f)
            f.getMessage.asLeft
        }
      }

    def deleteIndex: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("index")
      .out(stringBody)
      .errorOut(stringInternalServerError)
      .serverLogicPure { _ =>
        def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
        searchIndexService
          .findAllIndexes(props.SearchIndex)
          .map(indexes => {
            indexes.map(index => {
              logger.info(s"Deleting index $index")
              searchIndexService.deleteIndexWithName(Option(index))
            })
          }) match {
          case Failure(ex) => ex.getMessage.asLeft
          case Success(deleteResults) =>
            val (errors, successes) = deleteResults.partition(_.isFailure)
            if (errors.nonEmpty) {
              val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
                s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
                s"${pluralIndex(successes.length)} were deleted successfully."
              message.asLeft
            } else {
              s"Deleted ${pluralIndex(successes.length)}".asRight
            }
        }
      }

    def dumpLearningpaths: ServerEndpoint[Any, Eff] = endpoint.get
      .in("dump" / "learningpath")
      .in(query[Int]("page").default(1))
      .in(query[Int]("page-size").default(250))
      .in(query[Boolean]("only-published").default(true))
      .out(jsonBody[LearningPathDomainDump])
      .serverLogicPure { case (pageNo, pageSize, onlyIncludePublished) =>
        readService.getLearningPathDomainDump(pageNo, pageSize, onlyIncludePublished).asRight
      }

    def dumpSingleLearningPath: ServerEndpoint[Any, Eff] = endpoint.get
      .in("dump" / "learningpath" / path[Long]("learningpath_id"))
      .out(jsonBody[domain.LearningPath])
      .errorOut(errorOutputsFor(404))
      .serverLogicPure { learningpathId =>
        learningPathRepository.withId(learningpathId) match {
          case Some(value) => value.asRight
          case None        => notFound.asLeft
        }
      }

    def postLearningPathDump: ServerEndpoint[Any, Eff] = endpoint.post
      .in("dump" / "learningpath")
      .in(jsonBody[domain.LearningPath])
      .out(jsonBody[domain.LearningPath])
      .errorOut(errorOutputsFor(404))
      .serverLogicPure { dumpToInsert =>
        updateService.insertDump(dumpToInsert).asRight
      }

    def containsArticle: ServerEndpoint[Any, Eff] = endpoint.get
      .in("containsArticle")
      .in(listQuery[String]("paths"))
      .out(jsonBody[Seq[LearningPathSummaryV2]])
      .errorOut(errorOutputsFor(404))
      .serverLogicPure { paths =>
        searchService.containsPath(paths.values) match {
          case Success(result) => result.results.asRight
          case Failure(ex)     => returnLeftError(ex)
        }
      }
  }
}
