/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import enumeratum.Json4s
import no.ndla.common.model.NDLADate
import no.ndla.learningpathapi.model.api.{Error, ValidationError}
import no.ndla.learningpathapi.service.{ConverterService, ReadService, UpdateService}
import no.ndla.myndla.model.api.{
  Folder,
  FolderSortRequest,
  NewFolder,
  NewResource,
  Resource,
  UpdatedFolder,
  UpdatedResource
}
import no.ndla.myndla.model.domain.FolderSortObject.{FolderSorting, ResourceSorting, RootFolderSorting}
import no.ndla.myndla.model.domain.ResourceType
import no.ndla.myndla.service.{FolderReadService, FolderWriteService}
import no.ndla.network.scalatra.NdlaSwaggerSupport
import org.json4s.ext.{JavaTimeSerializers, JavaTypesSerializers}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger._
import org.scalatra.NoContent

import java.util.UUID
import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success}

trait FolderController {
  this: ReadService
    with UpdateService
    with ConverterService
    with NdlaController
    with NdlaSwaggerSupport
    with FolderReadService
    with FolderWriteService =>
  val folderController: FolderController

  class FolderController(implicit val swagger: Swagger) extends NdlaController with NdlaSwaggerSupport {
    protected implicit override val jsonFormats: Formats =
      DefaultFormats ++
        JavaTimeSerializers.all ++
        JavaTypesSerializers.all +
        NDLADate.Json4sSerializer +
        Json4s.serializer(ResourceType)

    protected val applicationDescription = "API for accessing My NDLA from ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response204 = ResponseMessage(204, "No content", None)
    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access not granted", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))
    val response502 = ResponseMessage(502, "Remote error", Some("Error"))

    private val folderId      = Param[String]("folder_id", "UUID of the folder.")
    private val folderIdQuery = Param[Option[String]]("folder-id", "UUID of the folder.")
    private val folderStatus  = Param[String]("folder-status", "Status of the folder")
    private val resourceId    = Param[String]("resource_id", "UUID of the resource.")
    private val size          = Param[Option[Int]]("size", "Limit the number of results to this many elements")
    private val feideToken    = Param[Option[String]]("FeideAuthorization", "Header containing FEIDE access token.")

    private val includeResources =
      Param[Option[Boolean]]("include-resources", "Choose if resources should be included in the response")
    private val includeSubfolders =
      Param[Option[Boolean]]("include-subfolders", "Choose if sub-folders should be included in the response")

    private val sourceId = Param[String]("source_folder_id", "Source UUID of the folder.")
    private val destinationId = Param[Option[String]](
      "destination-folder-id",
      "Destination UUID of the folder. If None it will be cloned as a root folder."
    )

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
            asHeaderParam(feideToken),
            asQueryParam(includeSubfolders),
            asQueryParam(includeResources)
          )
          .deprecated(true)
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val includeSubfolders = booleanOrDefault(this.includeSubfolders.paramName, default = false)
      val includeResources  = booleanOrDefault(this.includeResources.paramName, default = false)
      folderReadService.getFolders(includeSubfolders, includeResources, requestFeideToken) match {
        case Failure(ex)      => errorHandler(ex)
        case Success(folders) => folders
      }
    }: Unit

    get(
      "/:folder_id",
      operation(
        apiOperation[Folder]("fetchFolder")
          .summary("Fetch a folder and all its content")
          .description("Fetch a folder and all its content")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(folderId),
            asQueryParam(includeResources),
            asQueryParam(includeSubfolders)
          )
          .deprecated(true)
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName).flatMap(id => {
        val includeResources  = booleanOrDefault(this.includeResources.paramName, default = false)
        val includeSubfolders = booleanOrDefault(this.includeSubfolders.paramName, default = false)
        folderReadService.getSingleFolder(id, includeSubfolders, includeResources, requestFeideToken)
      })
    }: Unit

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
          .deprecated(true)
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val newFolder = extract[NewFolder](request.body)
      folderWriteService.newFolder(newFolder, requestFeideToken) match {
        case Failure(ex)     => errorHandler(ex)
        case Success(folder) => folder
      }
    }: Unit

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
          .deprecated(true)
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName).flatMap(id => {
        val updatedFolder = extract[UpdatedFolder](request.body)
        folderWriteService.updateFolder(id, updatedFolder, requestFeideToken)
      })
    }: Unit

    delete(
      "/:folder_id",
      operation(
        apiOperation[Unit]("removeFolder")
          .summary("Remove folder from user folders")
          .description("Remove folder from user folders")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(folderId)
          )
          .deprecated(true)
          .responseMessages(response204, response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName)
        .flatMap(id => {
          folderWriteService.deleteFolder(id, requestFeideToken)
        })
        .map(_ => NoContent())
    }: Unit

    get(
      "/resources/?",
      operation(
        apiOperation[List[Resource]]("fetchAllResources")
          .summary("Fetch all resources that belongs to a user")
          .description("Fetch all resources that belongs to a user")
          .parameters(
            asHeaderParam(feideToken),
            asQueryParam(size)
          )
          .deprecated(true)
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val defaultSize = 5
      val size = intOrDefault("size", defaultSize) match {
        case tooSmall if tooSmall < 1 => defaultSize
        case x                        => x
      }
      folderReadService.getAllResources(size, requestFeideToken) match {
        case Failure(ex)      => errorHandler(ex)
        case Success(folders) => folders
      }
    }: Unit

    post(
      "/:folder_id/resources/?",
      operation(
        apiOperation[Resource]("createFolderResource")
          .summary("Creates new folder resource")
          .description("Creates new folder resource")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(folderId),
            bodyParam[NewResource]
          )
          .deprecated(true)
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName).flatMap(id => {
        val newResource = extract[NewResource](request.body)
        folderWriteService.newFolderResourceConnection(id, newResource, requestFeideToken)
      })
    }: Unit

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
          .deprecated(true)
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.resourceId.paramName).flatMap(id => {
        val updatedResource = extract[UpdatedResource](request.body)
        folderWriteService.updateResource(id, updatedResource, requestFeideToken)
      })
    }: Unit

    delete(
      "/:folder_id/resources/:resource_id",
      operation(
        apiOperation[Unit]("DeleteResource")
          .summary("Delete selected resource")
          .description("Delete selected resource")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(folderId),
            asPathParam(resourceId)
          )
          .deprecated(true)
          .responseMessages(response204, response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName)
        .flatMap(folderId => {
          uuidParam(this.resourceId.paramName).flatMap(resourceId => {
            folderWriteService.deleteConnection(folderId, resourceId, requestFeideToken)
          })
        })
        .map(_ => NoContent())
    }: Unit

    get(
      "/shared/:folder_id",
      operation(
        apiOperation[Folder]("fetchSharedFolder")
          .summary("Fetch a shared folder and all its content")
          .description("Fetch a shared folder and all its content")
          .parameters(
            asPathParam(folderId)
          )
          .deprecated(true)
          .responseMessages(response400, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      uuidParam(this.folderId.paramName).flatMap(id => folderReadService.getSharedFolder(id, requestFeideToken))
    }: Unit

    patch(
      "/shared/:folder_id",
      operation(
        apiOperation[List[UUID]]("ChangeStatusForFolderAndSubFolders")
          .summary("Change status for given folder and all its subfolders")
          .description("Change status for given folder and all its subfolders")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(folderId),
            asQueryParam(folderStatus)
          )
          .deprecated(true)
          .responseMessages(response204, response400, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      for {
        folderId   <- uuidParam(this.folderId.paramName)
        status     <- folderStatusParam(this.folderStatus.paramName)
        updatedIds <- folderWriteService.changeStatusOfFolderAndItsSubfolders(folderId, status, requestFeideToken)
      } yield updatedIds
    }: Unit

    post(
      "/clone/:source_folder_id/?",
      operation(
        apiOperation[Folder]("cloneFolder")
          .summary("Creates new folder structure based on source folder structure")
          .description("Creates new folder structure based on source folder structure")
          .parameters(
            asHeaderParam(feideToken),
            asPathParam(sourceId),
            asQueryParam(destinationId)
          )
          .deprecated(true)
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      for {
        source      <- uuidParam(this.sourceId.paramName)
        destination <- uuidParamOrNone(this.destinationId.paramName)
        cloned      <- folderWriteService.cloneFolder(source, destination, requestFeideToken)
      } yield cloned
    }: Unit

    put(
      "/sort-resources/:folder_id",
      operation(
        apiOperation[Folder]("sortFolderResources")
          .summary("Decide order of resource ids in a folder")
          .description("Decide order of resource ids in a folder")
          .parameters(
            asHeaderParam(feideToken),
            bodyParam[FolderSortRequest]
          )
          .deprecated(true)
      )
    ) {
      for {
        folderId    <- uuidParam(this.folderId.paramName)
        sortRequest <- tryExtract[FolderSortRequest](request.body)
        sortObject = ResourceSorting(folderId)
        sorted <- folderWriteService.sortFolder(sortObject, sortRequest, requestFeideToken)
      } yield sorted
    }: Unit

    put(
      "/sort-subfolders",
      operation(
        apiOperation[Folder]("sortFolderFolders")
          .summary("Decide order of subfolder ids in a folder")
          .description("Decide order of subfolder ids in a folder")
          .parameters(
            asHeaderParam(feideToken),
            bodyParam[FolderSortRequest],
            asQueryParam(folderIdQuery)
          )
          .deprecated(true)
      )
    ) {
      for {
        folderId    <- uuidParamOrNone(this.folderIdQuery.paramName)
        sortRequest <- tryExtract[FolderSortRequest](request.body)
        sortObject = folderId.map(id => FolderSorting(id)).getOrElse(RootFolderSorting())
        sorted <- folderWriteService.sortFolder(sortObject, sortRequest, requestFeideToken)
      } yield sorted
    }: Unit
  }
}
