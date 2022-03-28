/*
 * Part of NDLA draft-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.search

import no.ndla.draftapi.model.domain.ArticleStatus

case class SearchableStatus(current: ArticleStatus.Value, other: Set[ArticleStatus.Value])
