/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import cats.implicits.toFunctorOps
import com.scalatsi.{TSIType, TSNamedType, TSType}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.myndlaapi.model.api.ArenaUserDTO
import sttp.tapir.Schema.annotations.description

import scala.annotation.unused

sealed trait PostWrapperDTO

@description("Arena post data")
case class PostDTO(
    @description("The post id") id: Long,
    @description("The post content") content: String,
    @description("The post creation date") created: NDLADate,
    @description("The post edit date") updated: NDLADate,
    @description("The post owner") owner: Option[ArenaUserDTO],
    @description("The flags that have been added to post. Only visible to admins.") flags: Option[List[FlagDTO]],
    @description("The id of the parenting topic") topicId: Long,
    @description("The replies to the post") replies: List[PostWrapperDTO],
    @description("Number of upvotes on the post") upvotes: Int,
    @description("Flag saying if the logged in user has upvoted or not") upvoted: Boolean
) extends PostWrapperDTO

object PostDTO {
  implicit val postTSI: TSIType[PostDTO] = {
    @unused
    implicit val postWrapper: TSNamedType[PostWrapperDTO] = TSType.external[PostWrapperDTO]("IPostWrapperDTO")
    TSType.fromCaseClass[PostDTO]
  }

  implicit val postEncoder: Encoder[PostDTO] = deriveEncoder
  implicit val postDecoder: Decoder[PostDTO] = deriveDecoder

  implicit val postDataEncoder: Encoder[PostWrapperDTO] = Encoder.instance { case post: PostDTO => post.asJson }
  implicit val postDataDecoder: Decoder[PostWrapperDTO] = Decoder[PostDTO].widen
}

object PostWrapperDTO {
  implicit val wrapperAlias: TSNamedType[PostWrapperDTO] =
    TSType.alias[PostWrapperDTO]("IPostWrapperDTO", PostDTO.postTSI.get)
}
