/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api

import cats.implicits.toFunctorOps
import com.scalatsi.{TSIType, TSNamedType, TSType}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.domain.{CopyableFolder, CopyableResource, ResourceType}
import sttp.tapir.Schema.annotations.description

import scala.annotation.unused

case class Owner(
    @description("Name of the owner") name: String
)

object Owner {
  implicit val encoder: Encoder[Owner] = deriveEncoder
  implicit val decoder: Decoder[Owner] = deriveDecoder
}

// format: off
case class Folder(
    @description("UUID of the folder") id: String,
    @description("Folder name") name: String,
    @description("Folder status") status: String,
    @description("UUID of parent folder") parentId: Option[String],
    @description("List of parent folders to resource") breadcrumbs: List[Breadcrumb],
    @description("List of subfolders") subfolders: List[FolderData],
    @description("List of resources") resources: List[Resource],
    @description("Where the folder is sorted within its parent") rank: Option[Int],
    @description("When the folder was created") created: NDLADate,
    @description("When the folder was updated") updated: NDLADate,
    @description("When the folder was last shared") shared: Option[NDLADate],
    @description("Description of the folder") description: Option[String],
    @description("Owner of the folder, if the owner have opted in to share their name") owner: Option[Owner],
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

  implicit val folderEncoder: Encoder[Folder] = deriveEncoder
  implicit val folderDecoder: Decoder[Folder] = deriveDecoder

  implicit val folderDataEncoder: Encoder[FolderData] = Encoder.instance { case folder: Folder => folder.asJson }
  implicit val folderDataDecoder: Decoder[FolderData] = Decoder[Folder].widen
}

sealed trait FolderData extends CopyableFolder {}
object FolderData {

//  implicit val encoder: Encoder[FolderData] = Encoder.instance { case data: Folder => data.asJson }
//  implicit val decoder: Decoder[FolderData] = Decoder[Folder].widen

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
    @description("Folder name") name: String,
    @description("Id of parent folder") parentId: Option[String],
    @description("Status of the folder (private, shared)") status: Option[String],
    @description("Description of the folder") description: Option[String]
)

case class UpdatedFolder(
    @description("Folder name") name: Option[String],
    @description("Status of the folder (private, shared)") status: Option[String],
    @description("Description of the folder") description: Option[String]
)

// format: off
case class Resource(
    @description("Unique ID of the resource") id: String,
    @description("Type of the resource. (Article, Learningpath)") resourceType: ResourceType,
    @description("Relative path of this resource") path: String,
    @description("When the resource was created") created: NDLADate,
    @description("List of tags") tags: List[String],
    @description("The id of the resource, useful for fetching metadata for the resource") resourceId: String,
    @description("The which rank the resource appears in a sorted sequence") rank: Option[Int]
) extends CopyableResource
// format: on

object Resource {
  implicit val encoder: Encoder[Resource] = deriveEncoder[Resource]
  implicit val decoder: Decoder[Resource] = deriveDecoder[Resource]
}

case class NewResource(
    @description("Type of the resource. (Article, Learningpath)") resourceType: ResourceType,
    @description("Relative path of this resource") path: String,
    @description("List of tags") tags: Option[List[String]],
    @description("The id of the resource, useful for fetching metadata for the resource") resourceId: String
)

case class UpdatedResource(
    @description("List of tags") tags: Option[List[String]],
    @description("The id of the resource, useful for fetching metadata for the resource") resourceId: Option[String]
)
