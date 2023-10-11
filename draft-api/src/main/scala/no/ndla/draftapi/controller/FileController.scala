/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.common.errors.ValidationException
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api._
import no.ndla.draftapi.service.WriteService
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import org.scalatra.NoContent
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig}
import org.scalatra.swagger.{ResponseMessage, Swagger}

import scala.util.{Failure, Success}

trait FileController {
  this: WriteService with NdlaController =>
  val fileController: FileController

  class FileController(implicit val swagger: Swagger) extends NdlaController with FileUploadSupport {
    protected val applicationDescription        = "API for uploading files to ndla.no."
    private val multipartFileSizeThresholdBytes = 1024 * 1024 * 30 // 30MB
    configureMultipartHandling(MultipartConfig(fileSizeThreshold = Some(multipartFileSizeThresholdBytes)))

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val file     = Param("file", "File to upload")
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
          .responseMessages(response400, response403, response500)
      )
    ) {
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        fileParams.get(file.paramName) match {
          case Some(fileToUpload) =>
            writeService.storeFile(fileToUpload) match {
              case Success(uploadedFile) => uploadedFile
              case Failure(ex)           => errorHandler(ex)
            }
          case None =>
            errorHandler(
              ValidationException("file", "The request must contain a file")
            )
        }
      }
    }: Unit

    delete(
      "/",
      operation(
        apiOperation[Unit]("Delete")
          .summary("Deletes provided file")
          .description("Deletes provided file")
          .authorizations("oauth2")
          .parameters(
            asHeaderParam(correlationId)
          )
          .responseMessages(response400, response403, response500)
      )
    ) {
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        paramOrNone(this.filePath.paramName) match {
          case Some(filePath) =>
            writeService.deleteFile(filePath) match {
              case Failure(ex) => errorHandler(ex)
              case Success(_)  => NoContent()
            }
          case None =>
            errorHandler(
              ValidationException(
                this.filePath.paramName,
                "The request must contain a file path query parameter"
              )
            )
        }
      }
    }: Unit

  }
}
