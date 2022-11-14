package no.ndla.searchapi.model.taxonomy

trait Connection {
  val id: String
}

trait ConnectionPage {
  val totalCount: Long
  val page: List[Connection]
}
