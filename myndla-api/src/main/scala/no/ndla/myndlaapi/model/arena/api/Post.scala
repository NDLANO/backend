/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import com.scalatsi.{TSIType, TSNamedType, TSType}
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
}

object PostWrapper {
  implicit val wrapperAlias: TSNamedType[PostWrapper] = TSType.alias[PostWrapper]("IPostWrapper", Post.postTSI.get)
}
