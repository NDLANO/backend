/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import no.ndla.scalatra.error.ValidationMessage

case class ArticleApiValidationError(
    code: String,
    description: String,
    messages: Seq[ValidationMessage]
)
