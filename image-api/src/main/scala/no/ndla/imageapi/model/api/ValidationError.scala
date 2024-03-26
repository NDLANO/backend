/*
 * Part of NDLA article_api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import no.ndla.common.errors.ValidationMessage
import sttp.tapir.Schema.annotations.description

import java.time.LocalDateTime

@description("Information about validation errors")
case class ValidationError(
    @description("Code stating the type of error") code: String,
    @description("Description of the error") description: String = "Validation error",
    @description("List of validation messages") messages: Seq[ValidationMessage],
    @description("When the error occurred") occurredAt: LocalDateTime = LocalDateTime.now()
)
