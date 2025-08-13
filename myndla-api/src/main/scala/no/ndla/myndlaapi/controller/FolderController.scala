/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.myndla.FolderStatus
import no.ndla.myndlaapi.model.api.{
  FolderDTO,
  FolderSortRequestDTO,
  NewFolderDTO,
  NewResourceDTO,
  ResourceDTO,
  UpdatedFolderDTO,
  UpdatedResourceDTO,
  UserFolderDTO
}
import no.ndla.myndlaapi.model.domain.FolderSortObject.{
  FolderSorting,
  ResourceSorting,
  RootFolderSorting,
  SharedFolderSorting
}
import no.ndla.myndlaapi.service.{FolderReadService, FolderWriteService}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Parameters.feideHeader
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID

trait FolderController {
  this: FolderReadService & FolderWriteService & ErrorHandling & TapirController =>
  val folderController: FolderController

  class FolderController extends TapirController {
    override val serviceName: String = "folders"

    override val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

    private val includeResources =
      query[Boolean]("include-resources")
        .description("Choose if resources should be included in the response")
        .default(false)

    private val includeSubfolders =
      query[Boolean]("include-subfolders")
        .description("Choose if sub-folders should be included in the response")
        .default(false)

    private val pathFolderId        = path[UUID]("folder-id").description("The UUID of the folder")
    private val sourceFolderId      = path[UUID]("source-folder-id").description("Source UUID of the folder.")
    private val destinationFolderId = query[Option[UUID]]("destination-folder-id")
      .description("Destination UUID of the folder. If None it will be cloned as a root folder.")
    private val pathResourceId  = path[UUID]("resource-id").description("The UUID of the resource")
    private val queryFolderId   = query[Option[UUID]]("folder-id").description("The UUID of the folder")
    private val queryRecentSize =
      query[Option[Int]]("size")
        .description("How many latest favorited resources to return")
    private val queryExcludeResourceTypes =
      listQuery[ResourceType]("exclude")
        .description(
          s"Which resource types to exclude. If None all resource types are included. To provide multiple resource types, separate by comma (,)."
        )

    import io.circe.generic.auto.*

    private def getAllFolders: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch top folders that belongs to a user")
      .description("Fetch top folders that belongs to a user")
      .in(feideHeader)
      .in(includeResources)
      .in(includeSubfolders)
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[UserFolderDTO])
      .serverLogicPure { case (feideHeader, includeResources, includeSubfolders) =>
        folderReadService.getFolders(includeSubfolders, includeResources, feideHeader)
      }

    private def getSingleFolder: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch a folder and all its content")
      .description("Fetch a folder and all its content")
      .in(pathFolderId)
      .in(feideHeader)
      .in(includeResources)
      .in(includeSubfolders)
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[FolderDTO])
      .serverLogicPure { case (folderId, feideHeader, includeResources, includeSubfolders) =>
        folderReadService
          .getSingleFolder(folderId, includeSubfolders, includeResources, feideHeader)

      }

    private def createNewFolder: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Creates new folder")
      .description("Creates new folder")
      .in(feideHeader)
      .in(jsonBody[NewFolderDTO])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[FolderDTO])
      .serverLogicPure { case (feideHeader, newFolder) =>
        folderWriteService.newFolder(newFolder, feideHeader)
      }

    private def updateFolder(): ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update folder with new data")
      .description("Update folder with new data")
      .in(feideHeader)
      .in(pathFolderId)
      .in(jsonBody[UpdatedFolderDTO])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[FolderDTO])
      .serverLogicPure { case (feideHeader, folderId, updatedFolder) =>
        folderWriteService.updateFolder(folderId, updatedFolder, feideHeader)
      }

    private def removeFolder(): ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Remove folder from user folders")
      .description("Remove folder from user folders")
      .in(feideHeader)
      .in(pathFolderId)
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(noContent)
      .serverLogicPure { case (feideHeader, folderId) =>
        folderWriteService.deleteFolder(folderId, feideHeader).map(_ => ())
      }

    private val defaultSize: Int       = 5
    val size: EndpointInput.Query[Int] = query[Int]("size")
      .description("Limit the number of results to this many elements")
      .default(defaultSize)

    private def fetchAllResources: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch all resources that belongs to a user")
      .description("Fetch all resources that belongs to a user")
      .in("resources")
      .in(feideHeader)
      .in(size)
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[List[ResourceDTO]])
      .serverLogicPure { case (feideHeader, inputSize) =>
        val size = if (inputSize < 1) defaultSize else inputSize
        folderReadService.getAllResources(size, feideHeader)
      }

    private def fetchRecent: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch the most recent favorited resource")
      .description("Fetch the most recent favorited resource")
      .in("resources")
      .in("recent")
      .in(queryRecentSize)
      .in(queryExcludeResourceTypes)
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[Seq[ResourceDTO]])
      .serverLogicPure { case (queryRecentSize, queryExcludeResourceTypes) =>
        folderReadService
          .getRecentFavorite(queryRecentSize, queryExcludeResourceTypes.values)

      }

    private def createFolderResource: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Creates new folder resource")
      .description("Creates new folder resource")
      .in(feideHeader)
      .in(pathFolderId / "resources")
      .in(jsonBody[NewResourceDTO])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[ResourceDTO])
      .serverLogicPure { case (feideHeader, folderId, newResource) =>
        folderWriteService.newFolderResourceConnection(folderId, newResource, feideHeader)
      }

    private def updateResource(): ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Updated selected resource")
      .description("Updates selected resource")
      .in("resources" / pathResourceId)
      .in(feideHeader)
      .in(jsonBody[UpdatedResourceDTO])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[ResourceDTO])
      .serverLogicPure { case (resourceId, feideHeader, updatedResource) =>
        folderWriteService.updateResource(resourceId, updatedResource, feideHeader)
      }

    private def deleteResource(): ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Delete selected resource")
      .description("Delete selected resource")
      .in(feideHeader)
      .in(pathFolderId / "resources" / pathResourceId)
      .out(noContent)
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .serverLogicPure { case (feideHeader, folderId, resourceId) =>
        folderWriteService
          .deleteConnection(folderId, resourceId, feideHeader)
          .map(_ => ())
      }

    private def fetchSharedFolder: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch a shared folder and all its content")
      .description("Fetch a shared folder and all its content")
      .in("shared" / pathFolderId)
      .in(feideHeader)
      .out(jsonBody[FolderDTO])
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .serverLogicPure { case (folderId, feideHeader) =>
        folderReadService.getSharedFolder(folderId, feideHeader)
      }

    private val folderStatus: EndpointInput.Query[FolderStatus.Value] =
      query[FolderStatus.Value]("folder-status").description("Status of the folder")
    private def changeStatusForFolderAndSubFolders: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Change status for given folder and all its subfolders")
      .description("Change status for given folder and all its subfolders")
      .in("shared" / pathFolderId)
      .in(folderStatus)
      .in(feideHeader)
      .out(jsonBody[List[UUID]])
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .serverLogicPure { case (folderId, status, feideHeader) =>
        folderWriteService.changeStatusOfFolderAndItsSubfolders(folderId, status, feideHeader)
      }

    private def cloneFolder: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Creates new folder structure based on source folder structure")
      .description("Creates new folder structure based on source folder structure")
      .in("clone" / sourceFolderId)
      .in(destinationFolderId)
      .in(feideHeader)
      .out(jsonBody[FolderDTO])
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .serverLogicPure { case (sourceFolderId, destinationFolderId, feideId) =>
        folderWriteService.cloneFolder(sourceFolderId, destinationFolderId, feideId)
      }

    private def sortFolderResources: ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Decide order of resource ids in a folder")
      .description("Decide order of resource ids in a folder")
      .in("sort-resources" / pathFolderId)
      .in(feideHeader)
      .in(jsonBody[FolderSortRequestDTO])
      .out(noContent)
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .serverLogicPure { case (folderId, feideHeader, sortRequest) =>
        val sortObject = ResourceSorting(folderId)
        folderWriteService.sortFolder(sortObject, sortRequest, feideHeader)
      }

    private def sortFolderFolders: ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Decide order of subfolder ids in a folder")
      .description("Decide order of subfolder ids in a folder")
      .in("sort-subfolders")
      .in(feideHeader)
      .in(jsonBody[FolderSortRequestDTO])
      .in(queryFolderId)
      .out(noContent)
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .serverLogicPure { case (feideHeader, sortRequest, folderId) =>
        val sortObject = folderId.map(id => FolderSorting(id)).getOrElse(RootFolderSorting())
        folderWriteService.sortFolder(sortObject, sortRequest, feideHeader)
      }

    private def sortSavedSharedFolders: ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Decide order of saved shared folders")
      .description("Decide order of saved shared folders")
      .in("sort-saved")
      .in(feideHeader)
      .in(jsonBody[FolderSortRequestDTO])
      .out(noContent)
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .serverLogicPure { case (feideHeader, sortRequest) =>
        val sortObject = SharedFolderSorting()
        folderWriteService.sortFolder(sortObject, sortRequest, feideHeader)
      }

    private def createFolderUserConnection: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Saves a shared folder")
      .description("Saves a shared folder")
      .in("shared" / pathFolderId / "save")
      .in(feideHeader)
      .out(noContent)
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .serverLogicPure { case (folderId, feideHeader) =>
        folderWriteService.newSaveSharedFolder(folderId, feideHeader)
      }

    private def deleteFolderUserConnection(): ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Deletes a saved shared folder")
      .description("Deletes a saved shared folder")
      .in("shared" / pathFolderId / "save")
      .in(feideHeader)
      .out(noContent)
      .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
      .serverLogicPure { case (folderId, feideHeader) =>
        folderWriteService.deleteSavedSharedFolder(folderId, feideHeader)
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getAllFolders,
      fetchAllResources,
      fetchRecent,
      getSingleFolder,
      createNewFolder,
      updateFolder(),
      removeFolder(),
      createFolderResource,
      updateResource(),
      deleteResource(),
      fetchSharedFolder,
      changeStatusForFolderAndSubFolders,
      cloneFolder,
      sortFolderResources,
      sortFolderFolders,
      sortSavedSharedFolders,
      createFolderUserConnection,
      deleteFolderUserConnection()
    )
  }
}
