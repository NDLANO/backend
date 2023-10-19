/*
 * Part of NDLA concept-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

case class Status(
    current: ConceptStatus,
    other: Set[ConceptStatus]
)

object Status {

  def default = {
    Status(
      current = ConceptStatus.IN_PROGRESS,
      other = Set.empty
    )
  }

}
