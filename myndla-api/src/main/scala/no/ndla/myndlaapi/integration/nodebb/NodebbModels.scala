/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.integration.nodebb

case class CategoryInList(
    cid: Long,
    name: String,
    description: String,
    children: List[CategoryInList]
)

case class Categories(
    categories: List[CategoryInList]
)

case class Owner(
    uid: Long,
    username: String,
    userslug: String,
    displayname: String
)

case class TopicInList(
    cid: Long,
    tid: Long,
    uid: Long,
    title: String,
    user: Owner,
    deleted: Long,
    locked: Long,
    pinned: Long
)

case class Pagination(
    currentPage: Long,
    pageCount: Long
)

case class Post(
    pid: Long,
    tid: Long,
    uid: Long,
    content: String,
    edited: Long,
    timestamp: Long,
    user: Owner,
    deleted: Long,
    upvotes: Int,
    upvoted: Boolean
)

case class SinglePostResponse(response: SinglePost)
case class SinglePost(
    pid: Long,
    tid: Long,
    uid: Long,
    content: String
)

case class SingleTopic(
    cid: Long,
    tid: Long,
    uid: Long,
    title: String,
    timestamp: Long,
    pagination: Pagination,
    posts: List[Post]
)

case class SingleCategory(
    cid: Long,
    name: String,
    description: String,
    pagination: Pagination,
    topics: List[TopicInList]
)

case class ImportException(message: String) extends RuntimeException(message)
