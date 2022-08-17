/*
 * Part of NDLA draft-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.search

import no.ndla.common.model.domain.draft.ArticleStatus

case class SearchableStatus(current: ArticleStatus.Value, other: Set[ArticleStatus.Value])
