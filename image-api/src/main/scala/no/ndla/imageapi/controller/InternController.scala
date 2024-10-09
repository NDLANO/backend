/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.imageapi.model.api.{ErrorHelpers, ImageMetaDomainDump, ImageMetaInformationV2}
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageIndexService, TagIndexService}
import no.ndla.imageapi.service.{ConverterService, ReadService}
import no.ndla.imageapi.Props
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

trait InternController {
  this: ImageRepository
    with ReadService
    with ConverterService
    with ImageIndexService
    with TagIndexService
    with ImageRepository
    with Props
    with ErrorHelpers
    with TapirController =>
  val internController: InternController

  class InternController extends TapirController with StrictLogging {
    import ErrorHelpers._

    override val prefix: EndpointInput[Unit] = "intern"
    override val enableSwagger               = false
    private val stringInternalServerError    = statusCode(StatusCode.InternalServerError).and(stringBody)

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      postIndex,
      deleteIndex,
      getExternImageId,
      getDomainImageFromUrl,
      dumpImages,
      dumpSingleImage,
      postDump
    )

    def postIndex: ServerEndpoint[Any, Eff] = endpoint.post
      .in("index")
      .in(query[Option[Int]]("numShards"))
      .out(stringBody)
      .errorOut(stringInternalServerError)
      .serverLogicPure { numShards =>
        implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

        val indexResults = for {
          imageIndex <- Future { imageIndexService.indexDocuments(numShards) }
          tagIndex   <- Future { tagIndexService.indexDocuments(numShards) }
        } yield (imageIndex, tagIndex)

        Await.result(indexResults, Duration(60, TimeUnit.MINUTES)) match {
          case (Success(imageIndex), Success(tagIndex)) =>
            val indexTime = math.max(tagIndex.millisUsed, imageIndex.millisUsed)
            val result =
              s"Completed indexing of ${imageIndex.totalIndexed} images in $indexTime ms."
            logger.info(result)
            result.asRight
          case (Failure(imageFail), _) =>
            logger.warn(imageFail.getMessage, imageFail)
            imageFail.getMessage.asLeft
          case (_, Failure(tagFail)) =>
            logger.warn(tagFail.getMessage, tagFail)
            tagFail.getMessage.asLeft
        }
      }

    def pluralIndex(n: Int): String = if (n == 1) "1 index" else s"$n indexes"

    def deleteIndex: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("index")
      .out(stringBody)
      .errorOut(stringInternalServerError)
      .serverLogicPure { _ =>
        imageIndexService.findAllIndexes(props.SearchIndex) match {
          case Failure(f) => f.getMessage.asLeft
          case Success(indexes) =>
            val deleteResults = indexes.map(index => {
              logger.info(s"Deleting index $index")
              imageIndexService.deleteIndexWithName(Option(index))
            })
            val (errors, successes) = deleteResults.partition(_.isFailure)
            if (errors.nonEmpty) {
              val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
                s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
                s"${pluralIndex(successes.length)} were deleted successfully."
              message.asLeft
            } else { s"Deleted ${pluralIndex(successes.length)}".asRight }
        }
      }

    def getExternImageId: ServerEndpoint[Any, Eff] = endpoint.get
      .in("extern" / path[String]("image_id"))
      .in(query[Option[String]]("language"))
      .out(jsonBody[ImageMetaInformationV2])
      .errorOut(errorOutputsFor(404))
      .withOptionalUser
      .serverLogicPure { user =>
        { case (externalId, language) =>
          imageRepository.withExternalId(externalId) match {
            case Some(image) =>
              converterService.asApiImageMetaInformationWithDomainUrlV2(image, language, user)
            case None => notFoundWithMsg(s"Image with external id $externalId not found").asLeft
          }
        }
      }

    val urlQueryParam: EndpointInput.Query[Option[String]] = query[Option[String]]("url")
    def getDomainImageFromUrl: ServerEndpoint[Any, Eff] = endpoint.get
      .in("domain_image_from_url")
      .in(urlQueryParam)
      .out(jsonBody[ImageMetaInformation])
      .errorOut(errorOutputsFor(400))
      .serverLogicPure {
        case Some(p) => readService.getDomainImageMetaFromUrl(p)
        case None    => badRequest(s"Query param '$urlQueryParam' needs to be specified to return an image").asLeft
      }

    def dumpImages: ServerEndpoint[Any, Eff] = endpoint.get
      .in("dump" / "image")
      .in(query[Int]("page").default(1))
      .in(query[Int]("page-size").default(250))
      .out(jsonBody[ImageMetaDomainDump])
      .serverLogicPure { case (page, pageSize) =>
        readService.getMetaImageDomainDump(page, pageSize).asRight
      }

    def dumpSingleImage: ServerEndpoint[Any, Eff] = endpoint.get
      .in("dump" / "image" / path[Long]("image_id"))
      .out(jsonBody[ImageMetaInformation])
      .errorOut(errorOutputsFor(400))
      .serverLogicPure { imageId =>
        imageRepository.withId(imageId) match {
          case Some(image) => image.asRight
          case None        => notFoundWithMsg(s"Could not find image with id: '$imageId'").asLeft
        }
      }

    def postDump: ServerEndpoint[Any, Eff] = endpoint.post
      .in("dump" / "image")
      .in(jsonBody[ImageMetaInformation])
      .out(jsonBody[ImageMetaInformation])
      .errorOut(errorOutputsFor(400))
      .serverLogicPure { imageMeta =>
        imageRepository.insert(imageMeta).asRight
      }
  }

}
