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
  UpdatedResource,
  UpdatedFolder,
  ValidationError
}
import no.ndla.learningpathapi.service.{ConverterService, ReadService, UpdateService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger._
import org.scalatra.util.NotNothing
import org.scalatra.{Ok, ScalatraServlet}

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
    protected implicit override val jsonFormats: Formats = DefaultFormats

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

    private val folderId   = Param[Long]("folder_id", "Id of the folder.")
    private val resourceId = Param[Long]("resource_id", "Id of the resource.")

    private val feideToken = Param[Option[String]]("FeideAuthorization", "Header containing FEIDE access token.")

    private val excludeResources =
      Param[Option[Boolean]]("exclude-resources", "Choose if resources should be omitted in the response")

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
          .authorizations("FEIDE")
      )
    ) {
      readService.getFolders(requestFeideToken) match {
        case Failure(ex)      => errorHandler(ex)
        case Success(folders) => folders
      }
    }

    get(
      "/:folder_id",
      operation(
        apiOperation[Folder]("fetchFolder")
          .summary("Fetch folder with specific ID")
          .description("Fetch folder with specific ID")
          .parameters(
            asHeaderParam(feideToken),
            asQueryParam(excludeResources)
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("FEIDE")
      )
    ) {
      val id               = long(this.folderId.paramName)
      val excludeResources = booleanOrDefault(this.excludeResources.paramName, default = false)
      readService.getFolder(id, excludeResources, requestFeideToken) match {
        case Failure(ex)     => errorHandler(ex)
        case Success(folder) => folder
      }
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
        case Failure(ex)     => errorHandler(ex)
        case Success(folder) => folder
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
      val updatedFolder = extract[UpdatedFolder](request.body)
      val id            = long(this.folderId.paramName)
      updateService.updateFolder(id, updatedFolder, requestFeideToken) match {
        case Failure(ex)     => errorHandler(ex)
        case Success(folder) => folder
      }
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
      val folderId = long(this.folderId.paramName)
      updateService.deleteFolder(folderId, requestFeideToken) match {
        case Failure(ex) => errorHandler(ex)
        case Success(id) => id
      }
    }

    get(
      "/resources/",
      operation(
        apiOperation[List[Resource]]("fetchAllResources")
          .summary("Fetch all resources that belongs to a user")
          .description("Fetch all resources that belongs to a user")
          .parameters(
            asHeaderParam(feideToken)
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("FEIDE")
      )
    ) {
      readService.getAllResources(requestFeideToken) match {
        case Failure(ex)      => errorHandler(ex)
        case Success(folders) => folders
      }
    }

    post(
      "/:folder_id/resources/",
      operation(
        apiOperation[Resource]("createFolderResource")
          .summary("Creates new folder resource")
          .description("Creates new folder resource")
          .parameters(
            asHeaderParam(feideToken),
            bodyParam[NewResource]
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val folderId    = long(this.folderId.paramName)
      val newResource = extract[NewResource](request.body)
      updateService.newFolderResourceConnection(folderId, newResource, requestFeideToken) match {
        case Failure(ex) => errorHandler(ex)
        case Success(_)  => Ok()
      }
    }

    patch(
      "/resources/:resource_id",
      operation(
        apiOperation[Resource]("UpdateResource")
          .summary("Updated selected resource")
          .description("Updates selected resource")
          .parameters(
            asHeaderParam(feideToken),
            bodyParam[UpdatedResource]
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val resourceId      = long(this.resourceId.paramName)
      val updatedResource = extract[UpdatedResource](request.body)
      updateService.updateResource(resourceId, updatedResource, requestFeideToken) match {
        case Failure(ex)       => errorHandler(ex)
        case Success(resource) => resource
      }
    }
  }
}
