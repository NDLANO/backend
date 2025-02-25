/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.arena.api

import sttp.tapir.Schema.annotations.description

sealed trait CategoryTypeDTO {
  val id: Long
  val subcategories: List[CategoryTypeDTO]
}

@description("Arena category data")
case class CategoryDTO(
    @description("The category's id") id: Long,
    @description("The category's title") title: String,
    @description("The category's description") description: String,
    @description("Count of topics in the category") topicCount: Long,
    @description("Count of posts in the category") postCount: Long,
    @description("Whether the requesting user is following the category") isFollowing: Boolean,
    @description("Whether the category is visible to regular users") visible: Boolean,
    @description("Where the category is sorted when sorting by rank") rank: Int,
    @description("The id of the parent category if any") parentCategoryId: Option[Long],
    @description("Count of subcategories in the category") categoryCount: Long,
    @description("Categories in the category") subcategories: List[CategoryDTO],
    @description("Breadcrumb path of categories") breadcrumbs: List[CategoryBreadcrumbDTO]
) extends CategoryTypeDTO

@description("Arena category data")
case class CategoryWithTopicsDTO(
    @description("The category's id") id: Long,
    @description("The category's title") title: String,
    @description("The category's description") description: String,
    @description("Count of topics in the category") topicCount: Long,
    @description("Count of posts in the category") postCount: Long,
    @description("Which page of topics") topicPage: Long,
    @description("Page size of topics") topicPageSize: Long,
    @description("Topics in the category") topics: List[TopicDTO],
    @description("Whether the requesting user is following the category") isFollowing: Boolean,
    @description("Whether the category is visible to regular users") visible: Boolean,
    @description("Where the category is sorted when sorting by rank") rank: Int,
    @description("The id of the parent category if any") parentCategoryId: Option[Long],
    @description("Count of subcategories in the category") categoryCount: Long,
    @description("Categories in the category") subcategories: List[CategoryDTO],
    @description("Breadcrumb path of categories") breadcrumbs: List[CategoryBreadcrumbDTO]
) extends CategoryTypeDTO
