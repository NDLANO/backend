/*
 * Part of NDLA network.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import java.time.LocalDateTime

case class ErrorBody(code: String, description: String, occuredAt: LocalDateTime)
