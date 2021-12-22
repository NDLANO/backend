/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.auth.User
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api._
import no.ndla.draftapi.service.WriteService
import no.ndla.validation.{ValidationException, ValidationMessage}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.NoContent
import org.scalatra.servlet.FileUploadSupport
import org.scalatra.swagger.{ResponseMessage, Swagger}

import scala.util.{Failure, Success}

trait FileController {
  this: WriteService with User =>
  val fileController: FileController

  class FileController(implicit val swagger: Swagger) extends NdlaController with FileUploadSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "API for uploading files to ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val file = Param("file", "File to upload")
    private val filePath = Param[String]("path", "Path to file. Eg: resources/awdW2CaX.png")

    post(
      "/",
      operation(
        apiOperation[api.UploadedFile]("uploadFile")
          .summary("Uploads provided file")
          .description("Uploads provided file")
          .authorizations("oauth2")
          .consumes("multipart/form-data")
          .parameters(
            asHeaderParam(correlationId),
            asFileParam(file)
          )
          .responseMessages(response400, response403, response500))
    ) {
      val userInfo = user.getUser
      doOrAccessDenied(userInfo.canWrite) {
        fileParams.get(file.paramName) match {
          case Some(fileToUpload) =>
            writeService.storeFile(fileToUpload) match {
              case Success(uploadedFile) => uploadedFile
              case Failure(ex)           => errorHandler(ex)
            }
          case None =>
            errorHandler(
              new ValidationException(errors = Seq(ValidationMessage("file", "The request must contain a file"))))
        }
      }
    }

    delete(
      "/",
      operation(
        apiOperation[Unit]("Delete")
          .summary("Deletes provided file")
          .description("Deletes provided file")
          .authorizations("oauth2")
          .parameters(
            asHeaderParam(correlationId),
          )
          .responseMessages(response400, response403, response500))
    ) {
      val userInfo = user.getUser
      doOrAccessDenied(userInfo.canWrite) {
        paramOrNone(this.filePath.paramName) match {
          case Some(filePath) =>
            writeService.deleteFile(filePath) match {
              case Failure(ex) => errorHandler(ex)
              case Success(_)  => NoContent()
            }
          case None =>
            errorHandler(new ValidationException(errors =
              Seq(ValidationMessage(this.filePath.paramName, "The request must contain a file path query parameter"))))
        }
      }
    }

  }
}
