/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import com.scalatsi.{TSIType, TSNamedType, TSType}
import no.ndla.common.model.NDLADate
import no.ndla.learningpathapi.model.domain.{CopyableFolder, CopyableResource}
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field
import scala.annotation.unused

case class Owner(
    @(ApiModelProperty @field)(description = "Name of the owner") name: String
)

// format: off
case class Folder(
    @(ApiModelProperty @field)(description = "UUID of the folder") id: String,
    @(ApiModelProperty @field)(description = "Folder name") name: String,
    @(ApiModelProperty @field)(description = "Folder status") status: String,
    @(ApiModelProperty @field)(description = "UUID of parent folder") parentId: Option[String],
    @(ApiModelProperty @field)(description = "List of parent folders to resource") breadcrumbs: List[Breadcrumb],
    @(ApiModelProperty @field)(description = "List of subfolders") subfolders: List[FolderData],
    @(ApiModelProperty @field)(description = "List of resources") resources: List[Resource],
    @(ApiModelProperty @field)(description = "Where the folder is sorted within its parent") rank: Option[Int],
    @(ApiModelProperty @field)(description = "When the folder was created") created: NDLADate,
    @(ApiModelProperty @field)(description = "When the folder was updated") updated: NDLADate,
    @(ApiModelProperty @field)(description = "When the folder was last shared") shared: Option[NDLADate],
    @(ApiModelProperty @field)(description = "Description of the folder") description: Option[String],
    @(ApiModelProperty @field)(description = "Owner of the folder, if the owner have opted in to share their name") owner: Option[Owner],
) extends FolderData with CopyableFolder
// format: on

// 1: This object is needed for generating recursive Folder typescript type.
object Folder {
  implicit val resource: TSIType[Resource] = TSType.fromCaseClass[Resource]
  implicit val folderTSI: TSIType[Folder] = {
    @unused
    // 2: We are saying here that there is no need for scala-tsi to generate IFolderData type automatically.
    // We assure scala-tsi that we are getting IFolderData from external source. We point that source to FolderData.
    implicit val folderData: TSNamedType[FolderData] = TSType.external[FolderData]("IFolderData")
    TSType.fromCaseClass[Folder]
  }
}

sealed trait FolderData extends CopyableFolder {}
object FolderData {
  def apply(
      id: String,
      name: String,
      status: String,
      parentId: Option[String],
      breadcrumbs: List[Breadcrumb],
      subfolders: List[FolderData],
      resources: List[Resource],
      rank: Option[Int],
      created: NDLADate,
      updated: NDLADate,
      shared: Option[NDLADate],
      description: Option[String],
      username: Option[String]
  ): FolderData = {
    Folder(
      id,
      name,
      status,
      parentId,
      breadcrumbs,
      subfolders,
      resources,
      rank,
      created,
      updated,
      shared,
      description,
      username.map(name => Owner(name))
    )
  }

  // 3: After being redirected here from TSType.external we are manually making the union of FolderData,
  // with Folder. After that we alias it as IFolderData so that scala-tsi can incorporate it.
  implicit val folderDataAlias: TSNamedType[FolderData] = TSType.alias[FolderData]("IFolderData", Folder.folderTSI.get)
}

case class NewFolder(
    @(ApiModelProperty @field)(description = "Folder name") name: String,
    @(ApiModelProperty @field)(description = "Id of parent folder") parentId: Option[String],
    @(ApiModelProperty @field)(description = "Status of the folder (private, shared)") status: Option[String],
    @(ApiModelProperty @field)(description = "Description of the folder") description: Option[String]
)

case class UpdatedFolder(
    @(ApiModelProperty @field)(description = "Folder name") name: Option[String],
    @(ApiModelProperty @field)(description = "Status of the folder (private, shared)") status: Option[String],
    @(ApiModelProperty @field)(description = "Description of the folder") description: Option[String]
)

// format: off
case class Resource(
    @(ApiModelProperty @field)(description = "Unique ID of the resource") id: String,
    @(ApiModelProperty @field)(description = "Type of the resource. (Article, Learningpath)") resourceType: String,
    @(ApiModelProperty @field)(description = "Relative path of this resource") path: String,
    @(ApiModelProperty @field)(description = "When the resource was created") created: NDLADate,
    @(ApiModelProperty @field)(description = "List of tags") tags: List[String],
    @(ApiModelProperty @field)(description = "The id of the resource, useful for fetching metadata for the resource") resourceId: String,
    @(ApiModelProperty @field)(description = "The which rank the resource appears in a sorted sequence") rank: Option[Int]
) extends CopyableResource
// format: on

case class NewResource(
    @(ApiModelProperty @field)(description = "Type of the resource. (Article, Learningpath)") resourceType: String,
    @(ApiModelProperty @field)(description = "Relative path of this resource") path: String,
    @(ApiModelProperty @field)(description = "List of tags") tags: Option[List[String]],
    @(ApiModelProperty @field)(
      description = "The id of the resource, useful for fetching metadata for the resource"
    ) resourceId: String
)

case class UpdatedResource(
    @(ApiModelProperty @field)(description = "List of tags") tags: Option[List[String]],
    @(ApiModelProperty @field)(
      description = "The id of the resource, useful for fetching metadata for the resource"
    ) resourceId: Option[String]
)
