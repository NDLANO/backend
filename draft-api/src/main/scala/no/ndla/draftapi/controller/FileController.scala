/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.controller

import no.ndla.common.errors.{FileTooBigException, ValidationException}
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api.*
import no.ndla.draftapi.service.WriteService
import no.ndla.network.tapir.NoNullJsonPrinter.*
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, TapirController}
import no.ndla.network.clients.MyNDLAApiClient
import sttp.model.Part
import sttp.tapir.EndpointInput
import sttp.tapir.*
import no.ndla.common.model.domain
import sttp.tapir.server.ServerEndpoint

import java.io.File
import scala.util.{Failure, Success, Try}

class FileController(using
    writeService: WriteService,
    props: DraftApiProperties,
    errorHandling: ErrorHandling,
    errorHelpers: ErrorHelpers,
    myNDLAApiClient: MyNDLAApiClient,
) extends TapirController {
  override val serviceName: String         = "files"
  override val prefix: EndpointInput[Unit] = "draft-api" / "v1" / serviceName

  private val filePath = query[Option[String]]("path").description("Path to file. Eg: resources/awdW2CaX.png")

  val endpoints: List[ServerEndpoint[Any, Eff]] = List(uploadFile, deleteFile)

  def doWithStream[T](filePart: Part[File])(f: domain.UploadedFile => Try[T]): Try[T] = {
    val file = domain.UploadedFile.fromFilePart(filePart)
    if (file.fileSize > props.multipartFileSizeThresholdBytes) Failure(FileTooBigException())
    else f(file)
  }

  def uploadFile: ServerEndpoint[Any, Eff] = endpoint
    .post
    .summary("Uploads provided file")
    .description("Uploads provided file")
    .in(multipartBody[FileForm])
    .out(jsonBody[api.UploadedFileDTO])
    .errorOut(errorOutputsFor(400, 401, 403))
    .requirePermission(DRAFT_API_WRITE)
    .serverLogicPure { _ =>
      { formData =>
        doWithStream(formData.file) { uploadedFile =>
          writeService.storeFile(uploadedFile)
        }
      }
    }

  def deleteFile: ServerEndpoint[Any, Eff] = endpoint
    .delete
    .summary("Deletes provided file")
    .description("Deletes provided file")
    .out(noContent)
    .errorOut(errorOutputsFor(400, 401, 403))
    .in(filePath)
    .requirePermission(DRAFT_API_WRITE)
    .serverLogicPure { _ =>
      {
        case Some(fp) => writeService.deleteFile(fp) match {
            case Failure(ex) => errorHandling.returnLeftError(ex)
            case Success(_)  => Right(())
          }
        case None => errorHandling.returnLeftError(
            ValidationException(this.filePath.name, "The request must contain a file path query parameter")
          )
      }
    }
}
