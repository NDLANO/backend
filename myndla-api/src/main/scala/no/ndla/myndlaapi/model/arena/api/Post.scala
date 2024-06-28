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
import no.ndla.myndlaapi.model.api.ArenaUser
import sttp.tapir.Schema.annotations.description

import scala.annotation.unused

sealed trait PostWrapper

@description("Arena post data")
case class Post(
    @description("The post id") id: Long,
    @description("The post content") content: String,
    @description("The post creation date") created: NDLADate,
    @description("The post edit date") updated: NDLADate,
    @description("The post owner") owner: Option[ArenaUser],
    @description("The flags that have been added to post. Only visible to admins.") flags: Option[List[Flag]],
    @description("The id of the parenting topic") topicId: Long,
    @description("The replies to the post") replies: List[PostWrapper],
    @description("Number of upvotes on the post") upvotes: Int,
    @description("Flag saying if the logged in user has upvoted or not") upvoted: Boolean
) extends PostWrapper

object Post {
  implicit val postTSI: TSIType[Post] = {
    @unused
    implicit val postWrapper: TSNamedType[PostWrapper] = TSType.external[PostWrapper]("IPostWrapper")
    TSType.fromCaseClass[Post]
  }

  implicit val postEncoder: Encoder[Post] = deriveEncoder
  implicit val postDecoder: Decoder[Post] = deriveDecoder

  implicit val postDataEncoder: Encoder[PostWrapper] = Encoder.instance { case post: Post => post.asJson }
  implicit val postDataDecoder: Decoder[PostWrapper] = Decoder[Post].widen
}

object PostWrapper {
  def apply(
      id: Long,
      content: String,
      created: NDLADate,
      updated: NDLADate,
      owner: Option[ArenaUser],
      flags: Option[List[Flag]],
      topicId: Long,
      replies: List[PostWrapper],
      upvotes: Int,
      upvoted: Boolean
  ): PostWrapper = {
    Post(id, content, created, updated, owner, flags, topicId, replies, upvotes, upvoted)
  }

  implicit val wrapperAlias: TSNamedType[PostWrapper] = TSType.alias[PostWrapper]("IPostWrapper", Post.postTSI.get)
}
