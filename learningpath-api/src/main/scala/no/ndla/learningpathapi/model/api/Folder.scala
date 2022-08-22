/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import com.scalatsi.{TSIType, TSNamedType, TSType}
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import java.time.LocalDateTime
import scala.annotation.meta.field
import scala.annotation.unused

case class Folder(
    @(ApiModelProperty @field)(description = "UUID of the folder") id: String,
    @(ApiModelProperty @field)(description = "Folder name") name: String,
    @(ApiModelProperty @field)(description = "Folder status") status: String,
    @(ApiModelProperty @field)(description = "UUID of parent folder") parentId: Option[String],
    @(ApiModelProperty @field)(description = "List of parent folders to resource") breadcrumbs: List[Breadcrumb],
    @(ApiModelProperty @field)(description = "List of subfolders") subfolders: List[FolderData],
    @(ApiModelProperty @field)(description = "List of resources") resources: List[Resource],
    @(ApiModelProperty @field)(description = "Where the folder is sorted within its parent") rank: Option[Int]
) extends FolderData

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

sealed trait FolderData {}
object FolderData       {
  // 3: After being redirected here from TSType.external we are manually making the union of FolderData,
  // with Folder. After that we alias it as IFolderData so that scala-tsi can incorporate it.
  implicit val folderDataAlias: TSNamedType[FolderData] = TSType.alias[FolderData]("IFolderData", Folder.folderTSI.get)
}

case class NewFolder(
    @(ApiModelProperty @field)(description = "Folder name") name: String,
    @(ApiModelProperty @field)(description = "Id of parent folder") parentId: Option[String],
    @(ApiModelProperty @field)(description = "Status of the folder (private, public)") status: Option[String]
)

case class UpdatedFolder(
    @(ApiModelProperty @field)(description = "Folder name") name: Option[String],
    @(ApiModelProperty @field)(description = "Status of the folder (private, public)") status: Option[String]
)

// format: off
case class Resource(
    @(ApiModelProperty @field)(description = "Unique ID of the resource") id: String,
    @(ApiModelProperty @field)(description = "Type of the resource. (Article, Learningpath)") resourceType: String,
    @(ApiModelProperty @field)(description = "Relative path of this resource") path: String,
    @(ApiModelProperty @field)(description = "When the resource was created") created: LocalDateTime,
    @(ApiModelProperty @field)(description = "List of tags") tags: List[String],
    @(ApiModelProperty @field)(description = "The id of the resource, useful for fetching metadata for the resource") resourceId: Long,
    @(ApiModelProperty @field)(description = "The which rank the resource appears in a sorted sequence") rank: Option[Int]
)
// format: on

case class NewResource(
    @(ApiModelProperty @field)(description = "Type of the resource. (Article, Learningpath)") resourceType: String,
    @(ApiModelProperty @field)(description = "Relative path of this resource") path: String,
    @(ApiModelProperty @field)(description = "List of tags") tags: Option[List[String]],
    @(ApiModelProperty @field)(
      description = "The id of the resource, useful for fetching metadata for the resource"
    ) resourceId: Long
)

case class UpdatedResource(
    @(ApiModelProperty @field)(description = "List of tags") tags: Option[List[String]],
    @(ApiModelProperty @field)(
      description = "The id of the resource, useful for fetching metadata for the resource"
    ) resourceId: Option[Long]
)
