/*
 * Part of NDLA concept-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

case class Status(
    current: ConceptStatus.Value,
    other: Set[ConceptStatus.Value]
)

object Status {

  def default = {
    Status(
      current = ConceptStatus.DRAFT,
      other = Set.empty
    )
  }

}
