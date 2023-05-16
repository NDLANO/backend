/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import cats.effect.IO
import cats.implicits._
import io.circe.generic.auto._
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api
import no.ndla.audioapi.model.api.{AudioMetaDomainDump, ErrorHelpers, NotFoundException}
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.api.AudioMetaDomainDump._
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{AudioIndexService, SeriesIndexService, TagIndexService}
import no.ndla.audioapi.service.{ConverterService, ReadService}
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.{OneOf, OneOfVariant}
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody

import scala.util.{Failure, Success}

trait InternController {
  this: AudioIndexService
    with ConverterService
    with AudioRepository
    with AudioIndexService
    with SeriesIndexService
    with TagIndexService
    with ReadService
    with Props
    with Service
    with ErrorHelpers =>
  val internController: InternController

  class InternController extends SwaggerService {
    override val prefix                 = "intern"
    override val enableSwagger          = false
    private val internalErrorStringBody = statusCode(StatusCode.InternalServerError).and(stringBody)

    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      endpoint.get
        .in("external")
        .in(path[String]("external_id"))
        .in(query[Option[String]]("language"))
        .out(jsonBody[Option[api.AudioMetaInformation]])
        .serverLogicPure { case (externalId, language) =>
          readService.withExternalId(externalId, language).asRight
        },
      endpoint.post
        .in("index")
        .out(stringBody)
        .errorOut(internalErrorStringBody)
        .serverLogicPure { _ =>
          (audioIndexService.indexDocuments, tagIndexService.indexDocuments, seriesIndexService.indexDocuments) match {
            case (Success(audioReindexResult), Success(tagReindexResult), Success(seriesReIndexResult)) =>
              val result =
                s"""Completed indexing of ${audioReindexResult.totalIndexed} documents in ${audioReindexResult.millisUsed} (audios) ms.
                   |Completed indexing of ${tagReindexResult.totalIndexed} documents in ${tagReindexResult.millisUsed} (tags) ms.
                   |Completed indexing of ${seriesReIndexResult.totalIndexed} documents in ${seriesReIndexResult.millisUsed} (series) ms.""".stripMargin
              logger.info(result)
              result.asRight
            case (Failure(f), _, _) =>
              logger.warn(f.getMessage, f)
              f.getMessage.asLeft
            case (_, Failure(f), _) =>
              logger.warn(f.getMessage, f)
              f.getMessage.asLeft
            case (_, _, Failure(f)) =>
              logger.warn(f.getMessage, f)
              f.getMessage.asLeft
          }
        },
      endpoint.delete
        .in("index")
        .errorOut(internalErrorStringBody)
        .out(stringBody)
        .serverLogicPure { _ =>
          def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
          audioIndexService.findAllIndexes(props.SearchIndex) match {
            case Failure(f) => f.getMessage.asLeft
            case Success(indexes) =>
              val deleteResults = indexes.map(index => {
                logger.info(s"Deleting index $index")
                audioIndexService.deleteIndexWithName(Option(index))
              })
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
        },
      endpoint.get
        .in("dump" / "audio")
        .in(query[Int]("page").default(1))
        .in(query[Int]("page-size").default(250))
        .out(jsonBody[AudioMetaDomainDump])
        .errorOut(errorOutputsFor(400, 500))
        .serverLogicPure { case (pageNo, pageSize) =>
          readService.getMetaAudioDomainDump(pageNo, pageSize).asRight
        },
      endpoint.get
        .in("dump" / "audio")
        .in(path[Long]("id"))
        .errorOut(errorOutputsFor(400, 404))
        .out(jsonBody[AudioMetaInformation])
        .serverLogicPure { id =>
          audioRepository.withId(id) match {
            case Some(image) => image.asRight
            case None        => returnError(new NotFoundException(s"Could not find audio with id: '$id'")).asLeft
          }
        },
      endpoint.post
        .in("dump" / "audio")
        .in(jsonBody[AudioMetaInformation])
        .out(jsonBody[AudioMetaInformation])
        .serverLogicPure { domainMeta => audioRepository.insert(domainMeta).asRight }
    )
  }
}
