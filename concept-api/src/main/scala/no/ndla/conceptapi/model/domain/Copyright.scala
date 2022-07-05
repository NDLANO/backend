/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import java.time.LocalDateTime

case class Copyright(
    license: Option[String],
    origin: Option[String],
    creators: Seq[Author],
    processors: Seq[Author],
    rightsholders: Seq[Author],
    agreementId: Option[Long],
    validFrom: Option[LocalDateTime],
    validTo: Option[LocalDateTime]
)
