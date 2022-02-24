package no.ndla.conceptapi.model.search

import no.ndla.conceptapi.model.domain.Author

case class SearchableCopyright(
    origin: Option[String],
    creators: Seq[Author],
    processors: Seq[Author],
    rightsholders: Seq[Author]
)
