package no.ndla.learningpathapi.model.domain

import java.util.UUID

trait Rankable {
  val sortId: UUID
  val sortRank: Option[Int]
}
