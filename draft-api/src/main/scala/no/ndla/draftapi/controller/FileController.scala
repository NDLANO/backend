/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.common.errors.{FileTooBigException, ValidationException}
import no.ndla.draftapi.{Eff, Props}
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api._
import no.ndla.draftapi.service.WriteService
import no.ndla.network.tapir.NoNullJsonPrinter._
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import sttp.model.{Part, StatusCode}
import sttp.tapir.EndpointInput
import sttp.tapir._
import io.circe.generic.auto._
import no.ndla.common.model.domain
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint

import java.io.File
import scala.util.{Failure, Success, Try}

trait FileController {
  this: WriteService with ErrorHelpers with Props =>
  val fileController: FileController

  class FileController extends Service[Eff] {
    import ErrorHelpers._
    override val serviceName: String         = "files"
    override val prefix: EndpointInput[Unit] = "draft-api" / "v1" / serviceName

    private val filePath = query[Option[String]]("path").description("Path to file. Eg: resources/awdW2CaX.png")

    val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      uploadFile,
      deleteFile
    )

    case class FileForm(file: Part[File])

    def doWithStream[T](filePart: Part[File])(f: domain.UploadedFile => Try[T]): Try[T] = {
      val file = domain.UploadedFile.fromFilePart(filePart)
      if (file.fileSize > props.multipartFileSizeThresholdBytes) Failure(FileTooBigException())
      else file.doWithStream(f)
    }

    def uploadFile: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Uploads provided file")
      .description("Uploads provided file")
      .in(multipartBody[FileForm])
      .out(jsonBody[api.UploadedFile])
      .errorOut(errorOutputsFor(400, 401, 403))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure {
        _ =>
          { formData =>
            doWithStream(formData.file) { uploadedFile =>
              writeService.storeFile(uploadedFile)
            }.handleErrorsOrOk
          }
      }

    def deleteFile: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Deletes provided file")
      .description("Deletes provided file")
      .out(statusCode(StatusCode.NoContent).and(emptyOutput))
      .errorOut(errorOutputsFor(400, 401, 403))
      .in(filePath)
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ =>
        {
          case Some(fp) =>
            writeService.deleteFile(fp) match {
              case Failure(ex) => returnLeftError(ex)
              case Success(_)  => Right(())
            }
          case None =>
            returnLeftError(
              ValidationException(
                this.filePath.name,
                "The request must contain a file path query parameter"
              )
            )
        }
      }
  }
}
