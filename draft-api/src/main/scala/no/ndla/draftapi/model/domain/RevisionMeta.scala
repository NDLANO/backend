package no.ndla.draftapi.model.domain

import java.time.{LocalDateTime, ZoneOffset}

case class RevisionMeta(
    revisionDate: LocalDateTime,
    notes: Seq[String]
)

object RevisionMeta {
  def default: RevisionMeta =
    RevisionMeta(
      revisionDate = LocalDateTime.now(ZoneOffset.UTC),
      notes = Seq.empty
    )
}
