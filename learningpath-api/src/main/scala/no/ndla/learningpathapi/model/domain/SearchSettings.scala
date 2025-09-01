/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.common.model.domain.learningpath.LearningPathStatus

case class SearchSettings(
    query: Option[String],
    withIdIn: List[Long],
    withPaths: List[String],
    taggedWith: Option[String],
    language: Option[String],
    sort: Sort,
    page: Option[Int],
    pageSize: Option[Int],
    fallback: Boolean,
    verificationStatus: Option[String],
    shouldScroll: Boolean,
    status: List[LearningPathStatus],
    articleId: Option[Long],
    grepCodes: List[String]
)
