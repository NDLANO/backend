/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.model.api.{
  Folder,
  NewFolder,
  NewResource,
  Resource,
  UpdatedFolder,
  UpdatedResource,
  ValidationError
}
import no.ndla.learningpathapi.service.{ConverterService, ReadService, UpdateService}
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger._
import org.scalatra.util.NotNothing
import org.scalatra.ScalatraServlet

import java.util.UUID
import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success}

trait FolderController {
  this: ReadService with UpdateService with ConverterService with NdlaController =>
  val folderController: FolderController

  class FolderController(implicit val swagger: Swagger)
      extends NdlaController
      with ScalatraServlet
      with NativeJsonSupport
      with SwaggerSupport
      with LazyLogging {
    protected implicit override val jsonFormats: Formats = DefaultFormats ++ JavaTimeSerializers.all

    protected val applicationDescription = "API for accessing My NDLA from ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access not granted", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))
    val response502 = ResponseMessage(502, "Remote error", Some("Error"))

    case class Param[T](paramName: String, description: String)

    private val folderId   = Param[UUID]("folder_id", "UUID of the folder.")
    private val resourceId = Param[UUID]("resource_id", "UUID of the resource.")
    private val size       = Param[Option[Int]]("size", "Limit the number of results to this many elements")
    private val feideToken = Param[Option[String]]("FeideAuthorization", "Header containing FEIDE access token.")

    private val includeResources =
      Param[Option[Boolean]]("include-resources", "Choose if resources should be included in the response")
    private val includeSubfolders =
      Param[Option[Boolean]]("include-subfolders", "Choose if sub-folders should be included in the response")

    private def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
      headerParam[T](param.paramName).description(param.description)

    private def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
      queryParam[T](param.paramName).description(param.description)

    private def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
      pathParam[T](param.paramName).description(param.description)

    private def requestFeideToken(implicit request: HttpServletRequest): Option[String] = {
      request.header(this.feideToken.paramName).map(_.replaceFirst("Bearer ", ""))
    }

    get(
      "/",
      operation(
        apiOperation[List[Folder]]("fetchAllFolders")
          .summary("Fetch top folders that belongs to a user")
          .description("Fetch top folders that belongs to a user")
          .parameters(
            asHeaderParam(feideToken)
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val includeSubfolders = booleanOrDefault(this.includeSubfolders.paramName, default = false)
      val includeResources  = booleanOrDefault(this.includeResources.paramName, default = false)
      readService.getFolders(includeSubfolders, includeResources, requestFeideToken) match {
        case Failure(ex)      => errorHandler(ex)
        case Success(folders) => folders
      }
    }

    get(
      "/:folder_id",
      operation(
        apiOperation[Folder]("fetchFolder")
          .summary("Fetch a folder and all its content")
          .description("Fetch a folder and all its content")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(folderId),
            asQueryParam(includeResources)
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName).flatMap(id => {
        val includeResources = booleanOrDefault(this.includeResources.paramName, default = false)
        readService.getFolder(id, includeResources, requestFeideToken)
      })
    }

    post(
      "/",
      operation(
        apiOperation[Folder]("createNewFolder")
          .summary("Creates new folder")
          .description("Creates new folder")
          .parameters(
            asHeaderParam(feideToken),
            bodyParam[NewFolder]
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val newFolder = extract[NewFolder](request.body)
      updateService.newFolder(newFolder, requestFeideToken) match {
        case Failure(ex) => errorHandler(ex)
        case Success(folder) =>
          folder
      }
    }

    patch(
      "/:folder_id",
      operation(
        apiOperation[Folder]("updateFolder")
          .summary("Update folder with new data")
          .description("Update folder with new data")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(folderId),
            bodyParam[UpdatedFolder]
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName).flatMap(id => {
        val updatedFolder = extract[UpdatedFolder](request.body)
        updateService.updateFolder(id, updatedFolder, requestFeideToken)
      })
    }

    delete(
      "/:folder_id",
      operation(
        apiOperation[Long]("removeFolder")
          .summary("Remove folder from user folders")
          .description("Remove folder from user folders")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(folderId)
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName).flatMap(id => {
        updateService.deleteFolder(id, requestFeideToken)
      })
    }

    get(
      "/resources/",
      operation(
        apiOperation[List[Resource]]("fetchAllResources")
          .summary("Fetch all resources that belongs to a user")
          .description("Fetch all resources that belongs to a user")
          .parameters(
            asHeaderParam(feideToken),
            asQueryParam(size)
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val defaultSize = 5
      val size = intOrDefault("size", defaultSize) match {
        case tooSmall if tooSmall < 1 => defaultSize
        case x                        => x
      }
      readService.getAllResources(size, requestFeideToken) match {
        case Failure(ex)      => errorHandler(ex)
        case Success(folders) => folders
      }
    }

    post(
      "/:folder_id/resources/",
      operation(
        apiOperation[Unit]("createFolderResource")
          .summary("Creates new folder resource")
          .description("Creates new folder resource")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(folderId),
            bodyParam[NewResource]
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName).flatMap(id => {
        val newResource = extract[NewResource](request.body)
        updateService.newFolderResourceConnection(id, newResource, requestFeideToken)
      })
    }

    patch(
      "/resources/:resource_id",
      operation(
        apiOperation[Resource]("UpdateResource")
          .summary("Updated selected resource")
          .description("Updates selected resource")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(resourceId),
            bodyParam[UpdatedResource]
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.resourceId.paramName).flatMap(id => {
        val updatedResource = extract[UpdatedResource](request.body)
        updateService.updateResource(id, updatedResource, requestFeideToken)
      })
    }

    delete(
      "/:folder_id/resources/:resource_id",
      operation(
        apiOperation[Resource]("UpdateResource")
          .summary("Updated selected resource")
          .description("Updates selected resource")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(folderId),
            asPathParam(resourceId)
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName).flatMap(folderId => {
        uuidParam(this.resourceId.paramName).flatMap(resourceId => {
          updateService.deleteConnection(folderId, resourceId, requestFeideToken)
        })
      })
    }
  }
}
