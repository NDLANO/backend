/*
 * Part of NDLA draft-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.domain

case class ArticleIds(articleId: Long, externalId: List[String], importId: Option[String] = None)
