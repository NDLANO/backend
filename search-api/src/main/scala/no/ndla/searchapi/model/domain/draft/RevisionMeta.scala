package no.ndla.searchapi.model.domain.draft

import java.time.LocalDateTime

case class RevisionMeta(
    revisionDate: LocalDateTime,
    note: String,
    status: String
)
