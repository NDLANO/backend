/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import java.time.LocalDateTime

case class ConceptResponsible(responsibleId: String, lastUpdated: LocalDateTime)
