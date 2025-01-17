/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import sttp.tapir.Schema.annotations.description

@description("Model to describe pagination of Topic")
case class PaginatedTopicsDTO(
    @description("How many items across all pages") totalCount: Long,
    @description("Which page number this is") page: Long,
    @description("How many items per page") pageSize: Long,
    @description("The paginated items") items: List[TopicDTO]
)

@description("Model to describe pagination of Post")
case class PaginatedPostsDTO(
    @description("How many items across all pages") totalCount: Long,
    @description("Which page number this is") page: Long,
    @description("How many items per page") pageSize: Long,
    @description("The paginated items") items: List[PostDTO]
)

@description("Model to describe pagination of Flag")
case class PaginatedNewPostNotificationsDTO(
    @description("How many items across all pages") totalCount: Long,
    @description("Which page number this is") page: Long,
    @description("How many items per page") pageSize: Long,
    @description("The paginated items") items: List[NewPostNotificationDTO]
)
