/*
 * Part of NDLA network.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import sttp.tapir.Schema.annotations.description
import java.time.LocalDateTime

@description("Information about an error")
case class ErrorBody(
    @description("Code stating the type of error") code: String,
    @description("Description of the error") description: String,
    @description("When the error occured") occurredAt: LocalDateTime,
    @description("Numeric http status code") statusCode: Int
)
