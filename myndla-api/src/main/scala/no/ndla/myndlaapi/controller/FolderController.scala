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
import no.ndla.myndlaapi.integration.InternalMyNDLAApiClient
import no.ndla.myndlaapi.model.api.{
  FolderDTO,
  FolderSortRequestDTO,
  NewFolderDTO,
  NewResourceDTO,
  ResourceDTO,
  UpdatedFolderDTO,
  UpdatedResourceDTO,
  UserFolderDTO,
  ResourceConnectionDTO,
}
import no.ndla.myndlaapi.model.domain.FolderSortObject.{
  FolderSorting,
  ResourceSorting,
  RootFolderSorting,
  SharedFolderSorting,
}
import no.ndla.myndlaapi.service.{FolderReadService, FolderWriteService}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.{ErrorHelpers, TapirController}
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID

class FolderController(using
    folderReadService: FolderReadService,
    folderWriteService: FolderWriteService,
    errorHandling: ControllerErrorHandling,
    errorHelpers: ErrorHelpers,
    myNDLAApiClient: InternalMyNDLAApiClient,
) extends TapirController {
  override val serviceName: String = "folders"

  override val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

  private val includeResources = query[Boolean]("include-resources")
    .description("Choose if resources should be included in the response")
    .default(false)

  private val includeSubfolders = query[Boolean]("include-subfolders")
    .description("Choose if sub-folders should be included in the response")
    .default(false)

  private val pathFolderId        = path[UUID]("folder-id").description("The UUID of the folder")
  private val sourceFolderId      = path[UUID]("source-folder-id").description("Source UUID of the folder.")
  private val queryResourcePath   = query[String]("path").description("The path of the resource to check")
  private val destinationFolderId = query[Option[UUID]]("destination-folder-id").description(
    "Destination UUID of the folder. If None it will be cloned as a root folder."
  )
  private val pathResourceId            = path[UUID]("resource-id").description("The UUID of the resource")
  private val queryFolderId             = query[Option[UUID]]("folder-id").description("The UUID of the folder")
  private val queryRecentSize           = query[Option[Int]]("size").description("How many latest favorited resources to return")
  private val queryExcludeResourceTypes = listQuery[ResourceType]("exclude").description(
    s"Which resource types to exclude. If None all resource types are included. To provide multiple resource types, separate by comma (,)."
  )

  import io.circe.generic.auto.*

  private def getAllFolders: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch top folders that belongs to a user")
    .description("Fetch top folders that belongs to a user")
    .in(includeResources)
    .in(includeSubfolders)
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[UserFolderDTO])
    .withFeideUser
    .serverLogicPure { feide =>
      { case (includeResources, includeSubfolders) =>
        folderReadService.getFolders(includeSubfolders, includeResources, feide)
      }
    }

  private def getSingleFolder: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch a folder and all its content")
    .description("Fetch a folder and all its content")
    .in(pathFolderId)
    .in(includeResources)
    .in(includeSubfolders)
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[FolderDTO])
    .withFeideUser
    .serverLogicPure { feide =>
      { case (folderId, includeResources, includeSubfolders) =>
        folderReadService.getSingleFolder(folderId, includeSubfolders, includeResources, feide)
      }
    }

  private def createNewFolder: ServerEndpoint[Any, Eff] = endpoint
    .post
    .summary("Creates new folder")
    .description("Creates new folder")
    .in(jsonBody[NewFolderDTO])
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[FolderDTO])
    .withFeideUser
    .serverLogicPure { feide => newFolder =>
      folderWriteService.newFolder(newFolder, feide)
    }

  private def updateFolder(): ServerEndpoint[Any, Eff] = endpoint
    .patch
    .summary("Update folder with new data")
    .description("Update folder with new data")
    .in(pathFolderId)
    .in(jsonBody[UpdatedFolderDTO])
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[FolderDTO])
    .withFeideUser
    .serverLogicPure { feide =>
      { case (folderId, updatedFolder) =>
        folderWriteService.updateFolder(folderId, updatedFolder, feide)
      }
    }

  private def removeFolder(): ServerEndpoint[Any, Eff] = endpoint
    .delete
    .summary("Remove folder from user folders")
    .description("Remove folder from user folders")
    .in(pathFolderId)
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(noContent)
    .withFeideUser
    .serverLogicPure { feide => folderId =>
      folderWriteService.deleteFolder(folderId, feide).map(_ => ())
    }

  private val defaultSize: Int       = 5
  val size: EndpointInput.Query[Int] = query[Int]("size")
    .description("Limit the number of results to this many elements")
    .default(defaultSize)

  private def fetchAllResources: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch all resources that belongs to a user")
    .description("Fetch all resources that belongs to a user")
    .in("resources")
    .in(size)
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[List[ResourceDTO]])
    .withFeideUser
    .serverLogicPure { feide => inputSize =>
      val size =
        if (inputSize < 1) defaultSize
        else inputSize
      folderReadService.getAllResources(size, feide)
    }

  private def fetchRecent: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch the most recent favorited resource across all users")
    .description("Fetch the most recent favorited resource across all users")
    .in("resources")
    .in("recent")
    .in(queryRecentSize)
    .in(queryExcludeResourceTypes)
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[Seq[ResourceDTO]])
    .serverLogicPure { case (queryRecentSize, queryExcludeResourceTypes) =>
      folderReadService.getRecentFavorite(queryRecentSize, queryExcludeResourceTypes.values)

    }

  private def hasFavoritedResource: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Check if a resource has been favorited by the user")
    .description("Check if a resource has been favorited by the user")
    .in("resources" / "has-favorited")
    .in(queryResourcePath)
    .out(jsonBody[Boolean])
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide => resourcePath =>
      folderReadService.hasFavoritedResource(resourcePath, feide)
    }

  private def getRootResources: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch root resources")
    .description("Fetch root resources")
    .in("resources" / "root")
    .out(jsonBody[List[ResourceDTO]])
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .withFeideUser
    .serverLogicPure { feide => _ =>
      folderReadService.getRootResources(feide)

    }

  private def getResourceByPath: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch resource by path")
    .description("Fetch resource by path")
    .in("resources" / "path")
    .in(queryResourcePath)
    .out(jsonBody[Option[ResourceDTO]])
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .withFeideUser
    .serverLogicPure { feide => resourcePath =>
      folderReadService.getResourceByPath(resourcePath, feide)
    }

  private def createFolderResource: ServerEndpoint[Any, Eff] = endpoint
    .post
    .summary("Creates new folder resource")
    .description("Creates new folder resource")
    .in(pathFolderId / "resources")
    .in(jsonBody[NewResourceDTO])
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[ResourceDTO])
    .withFeideUser
    .serverLogicPure { feide =>
      { case (folderId, newResource) =>
        folderWriteService.newResourceConnection(Some(folderId), newResource, feide)
      }
    }

  private def createRootResource: ServerEndpoint[Any, Eff] = endpoint
    .post
    .summary("Creates a resource at root level")
    .description("Creates a resource at root level")
    .in("resources" / "root")
    .in(jsonBody[NewResourceDTO])
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[ResourceDTO])
    .withFeideUser
    .serverLogicPure { feide =>
      { case (newResource) =>
        folderWriteService.newResourceConnection(None, newResource, feide)
      }

    }

  private def updateResource(): ServerEndpoint[Any, Eff] = endpoint
    .patch
    .summary("Updated selected resource")
    .description("Updates selected resource")
    .in("resources" / pathResourceId)
    .in(jsonBody[UpdatedResourceDTO])
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[ResourceDTO])
    .withFeideUser
    .serverLogicPure { feide =>
      { case (resourceId, updatedResource) =>
        folderWriteService.updateResource(resourceId, updatedResource, feide)
      }
    }

  private def deleteResource(): ServerEndpoint[Any, Eff] = endpoint
    .delete
    .summary("Delete selected resource")
    .description("Delete selected resource")
    .in(pathFolderId / "resources" / pathResourceId)
    .out(noContent)
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide =>
      { case (folderId, resourceId) =>
        folderWriteService.deleteConnection(Some(folderId), resourceId, feide).map(_ => ())
      }
    }

  private def deleteRootResource(): ServerEndpoint[Any, Eff] = endpoint
    .delete
    .summary("Delete selected root resource")
    .description("Delete selected root resource")
    .in("resources" / "root" / pathResourceId)
    .out(noContent)
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide => resourceId =>
      folderWriteService.deleteConnection(None, resourceId, feide).map(_ => ())
    }

  private def fetchSharedFolder: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch a shared folder and all its content")
    .description("Fetch a shared folder and all its content")
    .in("shared" / pathFolderId)
    .out(jsonBody[FolderDTO])
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .serverLogicPure { folderId =>
      folderReadService.getSharedFolder(folderId)
    }

  private def getResourceConnectionsByPath: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch resource connections by resource path")
    .description("Fetch resource connections by resource path")
    .in("resources" / "connections")
    .in(queryResourcePath)
    .out(jsonBody[List[ResourceConnectionDTO]])
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide => resourcePath =>
      folderReadService.getResourceConnectionsByPath(resourcePath, feide)
    }

  private def getTags: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Fetch all tags that belongs to a user")
    .description("Fetch all tags that belongs to a user")
    .in("resources" / "tags")
    .out(jsonBody[List[String]])
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .withFeideUser
    .serverLogicPure { feide => _ =>
      folderReadService.getAllTags(feide)
    }

  private val folderStatus: EndpointInput.Query[FolderStatus.Value] = query[FolderStatus.Value]("folder-status")
    .description("Status of the folder")
  private def changeStatusForFolderAndSubFolders: ServerEndpoint[Any, Eff] = endpoint
    .patch
    .summary("Change status for given folder and all its subfolders")
    .description("Change status for given folder and all its subfolders")
    .in("shared" / pathFolderId)
    .in(folderStatus)
    .out(jsonBody[List[UUID]])
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide =>
      { case (folderId, status) =>
        folderWriteService.changeStatusOfFolderAndItsSubfolders(folderId, status, feide)
      }
    }

  private def cloneFolder: ServerEndpoint[Any, Eff] = endpoint
    .post
    .summary("Creates new folder structure based on source folder structure")
    .description("Creates new folder structure based on source folder structure")
    .in("clone" / sourceFolderId)
    .in(destinationFolderId)
    .out(jsonBody[FolderDTO])
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide =>
      { case (sourceFolderId, destinationFolderId) =>
        folderWriteService.cloneFolder(sourceFolderId, destinationFolderId, feide)
      }
    }

  private def sortRootResources: ServerEndpoint[Any, Eff] = endpoint
    .put
    .summary("Decide order of root resource ids")
    .description("Decide order of root resource ids")
    .in("sort-resources" / "root")
    .in(jsonBody[FolderSortRequestDTO])
    .out(noContent)
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide => sortRequest =>
      val sortObject = ResourceSorting(None)
      folderWriteService.sortFolder(sortObject, sortRequest, feide)
    }

  private def sortFolderResources: ServerEndpoint[Any, Eff] = endpoint
    .put
    .summary("Decide order of resource ids in a folder")
    .description("Decide order of resource ids in a folder")
    .in("sort-resources" / pathFolderId)
    .in(jsonBody[FolderSortRequestDTO])
    .out(noContent)
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide =>
      { case (folderId, sortRequest) =>
        val sortObject = ResourceSorting(Some(folderId))
        folderWriteService.sortFolder(sortObject, sortRequest, feide)
      }
    }

  private def sortFolderFolders: ServerEndpoint[Any, Eff] = endpoint
    .put
    .summary("Decide order of subfolder ids in a folder")
    .description("Decide order of subfolder ids in a folder")
    .in("sort-subfolders")
    .in(jsonBody[FolderSortRequestDTO])
    .in(queryFolderId)
    .out(noContent)
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide =>
      { case (sortRequest, folderId) =>
        val sortObject = folderId.map(id => FolderSorting(id)).getOrElse(RootFolderSorting())
        folderWriteService.sortFolder(sortObject, sortRequest, feide)
      }
    }

  private def sortSavedSharedFolders: ServerEndpoint[Any, Eff] = endpoint
    .put
    .summary("Decide order of saved shared folders")
    .description("Decide order of saved shared folders")
    .in("sort-saved")
    .in(jsonBody[FolderSortRequestDTO])
    .out(noContent)
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide => sortRequest =>
      val sortObject = SharedFolderSorting()
      folderWriteService.sortFolder(sortObject, sortRequest, feide)
    }

  private def createFolderUserConnection: ServerEndpoint[Any, Eff] = endpoint
    .post
    .summary("Saves a shared folder")
    .description("Saves a shared folder")
    .in("shared" / pathFolderId / "save")
    .out(noContent)
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide => folderId =>
      folderWriteService.newSaveSharedFolder(folderId, feide)
    }

  private def deleteFolderUserConnection(): ServerEndpoint[Any, Eff] = endpoint
    .delete
    .summary("Deletes a saved shared folder")
    .description("Deletes a saved shared folder")
    .in("shared" / pathFolderId / "save")
    .out(noContent)
    .errorOut(errorOutputsFor(400, 401, 403, 404, 502))
    .withFeideUser
    .serverLogicPure { feide => folderId =>
      folderWriteService.deleteSavedSharedFolder(folderId, feide)
    }

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
    getAllFolders,
    fetchAllResources,
    fetchRecent,
    getSingleFolder,
    getResourceConnectionsByPath,
    getResourceByPath,
    getTags,
    hasFavoritedResource,
    createNewFolder,
    updateFolder(),
    removeFolder(),
    createFolderResource,
    createRootResource,
    getRootResources,
    updateResource(),
    deleteResource(),
    deleteRootResource(),
    fetchSharedFolder,
    changeStatusForFolderAndSubFolders,
    cloneFolder,
    sortRootResources,
    sortFolderResources,
    sortFolderFolders,
    sortSavedSharedFolders,
    createFolderUserConnection,
    deleteFolderUserConnection(),
  )
}
