/*
 * Part of NDLA common.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import no.ndla.common.model.domain.draft.ArticleStatus

case class Status(current: ArticleStatus.Value, other: Set[ArticleStatus.Value])
