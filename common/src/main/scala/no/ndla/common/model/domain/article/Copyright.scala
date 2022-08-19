/*
 * Part of NDLA common.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.article

import no.ndla.common.model.domain.Author
import java.time.LocalDateTime

case class Copyright(
    license: String,
    origin: String,
    creators: Seq[Author],
    processors: Seq[Author],
    rightsholders: Seq[Author],
    agreementId: Option[Long],
    validFrom: Option[LocalDateTime],
    validTo: Option[LocalDateTime]
)
