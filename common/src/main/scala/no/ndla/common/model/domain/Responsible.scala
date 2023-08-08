/*
 * Part of NDLA common.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import no.ndla.common.model.NDLADate

case class Responsible(
    responsibleId: String,
    lastUpdated: NDLADate
)
