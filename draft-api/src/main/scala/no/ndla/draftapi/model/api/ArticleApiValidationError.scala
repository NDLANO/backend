/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import no.ndla.validation.ValidationMessage

case class ArticleApiValidationError(code: String = Error.VALIDATION,
                                     description: String = Error.VALIDATION_DESCRIPTION,
                                     messages: Seq[ValidationMessage])
