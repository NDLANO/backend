package no.ndla.searchapi.model.search.filtering

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.searches.aggs.Aggregation
import com.sksamuel.elastic4s.requests.searches.queries.Query

sealed trait FilterBase {
  def path: String
  def toE4s: Query
}

case class TermFilter[T](path: String, filterValue: T) extends FilterBase {
  override def toE4s: Query = termQuery(path, filterValue)
}
case class TermsFilter[T](path: String, filterValues: List[T]) extends FilterBase {
  override def toE4s: Query = termsQuery(path, filterValues)
}

case class Filters(filters: List[FilterBase]) {
  def toAggregations(paths: List[String]): Seq[Aggregation] = {

    ???
  }
}
