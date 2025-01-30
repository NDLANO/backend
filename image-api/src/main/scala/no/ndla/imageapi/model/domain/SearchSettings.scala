/*
 * Part of NDLA image-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.domain

case class SearchSettings(
    query: Option[String],
    minimumSize: Option[Int],
    language: String,
    fallback: Boolean,
    license: Option[String],
    sort: Sort,
    page: Option[Int],
    pageSize: Option[Int],
    podcastFriendly: Option[Boolean],
    includeCopyrighted: Boolean,
    shouldScroll: Boolean,
    modelReleased: Seq[ModelReleasedStatus.Value],
    userFilter: List[String]
)
