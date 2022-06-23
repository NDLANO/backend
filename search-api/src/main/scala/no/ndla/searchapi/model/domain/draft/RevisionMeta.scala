package no.ndla.searchapi.model.domain.draft

import java.time.LocalDateTime

case class RevisionMeta(
    id: String,
    revisionDate: LocalDateTime,
    note: String,
    status: String
)
