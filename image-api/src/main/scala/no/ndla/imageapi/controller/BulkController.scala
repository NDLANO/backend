/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.UploadedFile
import no.ndla.imageapi.Props
import no.ndla.imageapi.controller.multipart.BatchMetaDataAndFileForm
import no.ndla.imageapi.model.api.bulk.{
  BulkUploadStartedDTO,
  BulkUploadStateDTO,
  BulkUploadStatus,
  UploadStatusEventType,
}
import no.ndla.imageapi.service.{ReadService, WriteService}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.NoNullJsonPrinter.*
import no.ndla.network.tapir.StreamingSSE
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.IMAGE_API_BATCH
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, TapirController}
import sttp.model.Part
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import java.io.File
import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.UUID
import scala.util.{Failure, Success, Try}

class BulkController(using
    readService: ReadService,
    writeService: WriteService,
    props: Props,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
    myNDLAApiClient: MyNDLAApiClient,
) extends TapirController
    with BaseImageController
    with StrictLogging {

  override val serviceName: String         = "bulk"
  override val prefix: EndpointInput[Unit] = "image-api" / "v1" / serviceName

  private def validateSameLength(metas: List[?], files: List[?]): Try[Unit] =
    if (metas.length == files.length) Success(())
    else Failure(
      ValidationException(errors =
        Seq(ValidationMessage("files", s"Got ${files.length} files but ${metas.length} meta entries"))
      )
    )

  /** Move tapir's multipart temp files into a per-upload staging dir we own. Tapir deletes the original temp files once
    * the request handler returns, so the background worker would otherwise be reading from missing inodes.
    */
  private def stageFiles(uploadId: UUID, parts: List[Part[File]]): Try[(Path, List[UploadedFile])] = Try {
    val stagingDir = Files.createTempDirectory(s"image-bulk-upload-$uploadId-")
    val staged     = parts.map { part =>
      val source = part.body.toPath
      val target = stagingDir.resolve(s"${UUID.randomUUID()}-${part.body.getName}")
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
      val stagedPart = part.copy(body = target.toFile)
      UploadedFile.fromFilePart(stagedPart)
    }
    stagingDir -> staged
  }.recoverWith { case ex =>
    logger.error(s"Bulk upload $uploadId: failed to stage uploaded files", ex)
    Failure(ex)
  }

  def batchInsertImages: ServerEndpoint[Any, Eff] = endpoint
    .post
    .summary("Start a bulk image upload session")
    .description("Stages all uploaded images and starts an asynchronous bulk upload.")
    .in(multipartBody[BatchMetaDataAndFileForm])
    .out(jsonBody[BulkUploadStartedDTO])
    .errorOut(errorOutputsFor(400))
    .requirePermission(IMAGE_API_BATCH)
    .serverLogicPure(user =>
      formData => {
        val metas    = formData.metadatas.map(_.body)
        val uploadId = UUID.randomUUID()
        val result   = for {
          _                   <- validateSameLength(metas, formData.files)
          _                   <- formData.files.traverseSizeCheck
          (stagingDir, files) <- stageFiles(uploadId, formData.files)
          _                   <- writeService.batchStoreImages(uploadId, metas.zip(files), stagingDir, user)
        } yield BulkUploadStartedDTO(uploadId)
        result.handleErrorsOrOk
      }
    )

  extension (parts: List[Part[File]]) {
    private def traverseSizeCheck: Try[Unit] = parts.foldLeft[Try[Unit]](Success(())) {
      case (acc @ Failure(_), _) => acc
      case (Success(()), part)   => doWithStream(part)(_ => Success(()))
    }
  }

  private def eventTypeFor(state: BulkUploadStateDTO): String = {
    state.status match {
      case BulkUploadStatus.Complete                           => UploadStatusEventType.Complete.toString
      case BulkUploadStatus.Failed                             => UploadStatusEventType.Failed.toString
      case BulkUploadStatus.Pending | BulkUploadStatus.Running => UploadStatusEventType.Progress.toString
    }
  }

  def checkUploadStatus: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Stream the status of a bulk upload session")
    .description("Returns Server-Sent Events with progress updates for the given upload-id.")
    .in("status" / path[UUID]("upload-id"))
    .out(StreamingSSE.jsonSSEBody[BulkUploadStateDTO](eventTypeFor).toEndpointIO)
    .errorOut(errorOutputsFor(400, 404, 500))
    .requirePermission(IMAGE_API_BATCH)
    .serverLogicPure(_ => uploadId => readService.getStatusStreamOfBulkUpload(uploadId).handleErrorsOrOk)

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(batchInsertImages, checkUploadStatus)

}
