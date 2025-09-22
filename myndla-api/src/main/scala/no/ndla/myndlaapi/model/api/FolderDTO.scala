/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.api

import cats.implicits.toFunctorOps
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ResourceType
import no.ndla.myndlaapi.model.domain.{CopyableFolder, CopyableResource}
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

import java.util.UUID
import no.ndla.common.DeriveHelpers

case class OwnerDTO(
    @description("Name of the owner") name: String
)

object OwnerDTO {
  implicit val encoder: Encoder[OwnerDTO] = deriveEncoder
  implicit val decoder: Decoder[OwnerDTO] = deriveDecoder
}

case class FolderDTO(
    @description("UUID of the folder") id: UUID,
    @description("Folder name") name: String,
    @description("Folder status") status: String,
    @description("UUID of parent folder") parentId: Option[UUID],
    @description("List of parent folders to resource") breadcrumbs: List[BreadcrumbDTO],
    @description("List of subfolders") subfolders: List[FolderDataDTO],
    @description("List of resources") resources: List[ResourceDTO],
    @description("Where the folder is sorted within its parent") rank: Int,
    @description("When the folder was created") created: NDLADate,
    @description("When the folder was updated") updated: NDLADate,
    @description("When the folder was last shared") shared: Option[NDLADate],
    @description("Description of the folder") description: Option[String],
    @description("Owner of the folder, if the owner have opted in to share their name") owner: Option[OwnerDTO]
) extends FolderDataDTO
    with CopyableFolder

object FolderDTO {
  implicit val folderEncoder: Encoder[FolderDTO] = deriveEncoder
  implicit val folderDecoder: Decoder[FolderDTO] = deriveDecoder
  import sttp.tapir.generic.auto.*
  implicit def schema: Schema[FolderDTO] = DeriveHelpers.getSchema

  implicit val folderDataEncoder: Encoder[FolderDataDTO] = Encoder.instance { case folder: FolderDTO => folder.asJson }
  implicit val folderDataDecoder: Decoder[FolderDataDTO] = Decoder[FolderDTO].widen
}

sealed trait FolderDataDTO extends CopyableFolder {}
object FolderDataDTO {

//  implicit val encoder: Encoder[FolderData] = Encoder.instance { case data: Folder => data.asJson }
//  implicit val decoder: Decoder[FolderData] = Decoder[Folder].widen

  def apply(
      id: UUID,
      name: String,
      status: String,
      parentId: Option[UUID],
      breadcrumbs: List[BreadcrumbDTO],
      subfolders: List[FolderDataDTO],
      resources: List[ResourceDTO],
      rank: Int,
      created: NDLADate,
      updated: NDLADate,
      shared: Option[NDLADate],
      description: Option[String],
      username: Option[String]
  ): FolderDataDTO = {
    FolderDTO(
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
      username.map(name => OwnerDTO(name))
    )
  }
}

case class NewFolderDTO(
    @description("Folder name") name: String,
    @description("Id of parent folder") parentId: Option[String],
    @description("Status of the folder (private, shared)") status: Option[String],
    @description("Description of the folder") description: Option[String]
)

case class UpdatedFolderDTO(
    @description("Folder name") name: Option[String],
    @description("Status of the folder (private, shared)") status: Option[String],
    @description("Description of the folder") description: Option[String]
)

case class ResourceDTO(
    @description("Unique ID of the resource") id: UUID,
    @description("Type of the resource. (Article, Learningpath)") resourceType: ResourceType,
    @description("Relative path of this resource") path: String,
    @description("When the resource was created") created: NDLADate,
    @description("List of tags") tags: List[String],
    @description("The id of the resource, useful for fetching metadata for the resource") resourceId: String,
    @description("The which rank the resource appears in a sorted sequence") rank: Option[Int]
) extends CopyableResource

object ResourceDTO {
  implicit val encoder: Encoder[ResourceDTO] = deriveEncoder[ResourceDTO]
  implicit val decoder: Decoder[ResourceDTO] = deriveDecoder[ResourceDTO]
}

case class NewResourceDTO(
    @description("Type of the resource. (Article, Learningpath)") resourceType: ResourceType,
    @description("Relative path of this resource") path: String,
    @description("List of tags") tags: Option[List[String]],
    @description("The id of the resource, useful for fetching metadata for the resource") resourceId: String
)

case class UpdatedResourceDTO(
    @description("List of tags") tags: Option[List[String]],
    @description("The id of the resource, useful for fetching metadata for the resource") resourceId: Option[String]
)
