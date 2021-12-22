/*
 * Part of NDLA search-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

case class Status(current: ArticleStatus.Value, other: Set[ArticleStatus.Value])
