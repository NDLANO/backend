package no.ndla.searchapi.model.search.filtering

import com.sksamuel.elastic4s.requests.searches.queries.Query


sealed trait FilterBase {
  def toE4s: Query
}

case class Filters(filters: List[FilterBase])
