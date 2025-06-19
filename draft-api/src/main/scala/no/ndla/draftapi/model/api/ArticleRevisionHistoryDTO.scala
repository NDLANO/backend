/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Information about article revision history")
case class ArticleRevisionHistoryDTO(
    @description("The revisions of an article, with the latest revision being the first in the list") revisions: Seq[
      ArticleDTO
    ],
    @description("Whether or not the current revision is safe to delete") canDeleteCurrentRevision: Boolean
)
