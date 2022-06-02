/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import com.scalatsi.TypescriptType.TSUnion
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import java.util.Date
import scala.annotation.meta.field
import com.scalatsi._

import scala.annotation.unused

case class Folder(
    @(ApiModelProperty @field)(description = "Unique ID of the folder") id: Long,
    @(ApiModelProperty @field)(description = "Folder name") name: String,
    @(ApiModelProperty @field)(description = "Folder status") status: String,
    @(ApiModelProperty @field)(description = "Folder favorite flag") isFavorite: Boolean,
    @(ApiModelProperty @field)(description = "List of other folders and resources") data: List[FolderData]
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
  // with Resource and Folder. After that we alias it as IFolderData so that scala-tsi can incorporate it.
  implicit val folderDataUnion: TSUnion                 = TSUnion.of(Folder.resource.get, Folder.folderTSI.get)
  implicit val folderDataAlias: TSNamedType[FolderData] = TSType.alias[FolderData]("IFolderData", folderDataUnion)
}

case class NewFolder(
    @(ApiModelProperty @field)(description = "Folder name") name: String,
    @(ApiModelProperty @field)(description = "Id of parent folder") parentId: Option[Long],
    @(ApiModelProperty @field)(description = "Status of the folder (private, public)") status: Option[String]
)

case class UpdatedFolder(
    @(ApiModelProperty @field)(description = "Folder name") name: Option[String],
    @(ApiModelProperty @field)(description = "Status of the folder (private, public)") status: Option[String]
)

case class Resource(
    @(ApiModelProperty @field)(description = "Unique ID of the resource") id: Long,
    @(ApiModelProperty @field)(description = "Type of the resource. (Article, Learningpath)") resourceType: String,
    @(ApiModelProperty @field)(description = "Relative path of this resource") path: String,
    @(ApiModelProperty @field)(description = "When the resource was created") created: Date,
    @(ApiModelProperty @field)(description = "List of tags") tags: List[String]
) extends FolderData

case class NewResource(
    @(ApiModelProperty @field)(description = "Type of the resource. (Article, Learningpath)") resourceType: String,
    @(ApiModelProperty @field)(description = "Relative path of this resource") path: String,
    @(ApiModelProperty @field)(description = "List of tags") tags: Option[List[String]]
)

case class UpdatedResource(
    @(ApiModelProperty @field)(description = "List of tags") tags: Option[List[String]]
)
