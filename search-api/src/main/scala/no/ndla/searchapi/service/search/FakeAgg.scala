/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.searches.aggs.Aggregation
import com.sksamuel.elastic4s.http.ElasticDsl._

/**
  * [[FakeAgg]] and inheriting classes are an abstraction to easier work with Elastic4s' Aggregations
  * They are usually used by calling `convertToReal()` which returns the real Elasitc4s [[Aggregation]]
  * And then passing them to an aggregating search.
  */
sealed trait FakeAgg {
  val name: String
  val subAggregations: Seq[FakeAgg]
  def withSubs(subs: Seq[FakeAgg]): FakeAgg
  def addSubAggs(agg: FakeAgg): FakeAgg = withSubs(subAggregations :+ agg)
  def convertToReal(): Aggregation

  /** Attempts to merge `toMerge` into `this` [[FakeAgg]].
    * @return Some(FakeAgg) if merge was successful, None if not.
    */
  def merge(toMerge: FakeAgg): Option[FakeAgg] = {
    if (toMerge.name == this.name && toMerge.getClass == this.getClass) {
      val subIdx = toMerge.subAggregations.zipWithIndex

      val (mergedLSubIdxes, mergedSubs) =
        this.subAggregations.foldLeft(Seq.empty[Int], Seq.empty[FakeAgg])((acc, thisSub) => {
          val (mergedIdxes, result) = acc

          // Attempts to merge all sub-aggregations from `toMerge` and keeps track of the merged ids
          val merged = subIdx
            .filterNot(s => mergedIdxes.contains(s._2))
            .view
            .map { case (subToMerge, idx) => (thisSub.merge(subToMerge), idx) }
            .collectFirst { case (Some(mergedAgg), idx) => (mergedAgg, idx) }

          val newMergedLSubs = mergedIdxes ++ merged.map(_._2).toSeq
          val mergedAggs = result :+ merged.map(_._1).getOrElse(thisSub)

          newMergedLSubs -> mergedAggs
        })

      // All unmerged sub-aggregations gets appended to the regular sub-aggregations
      val extras = subIdx.filterNot { case (_, idx) => mergedLSubIdxes.contains(idx) }.map(_._1)

      Some(this.withSubs(mergedSubs ++ extras))
    } else { None }
  }
}

object FakeAgg {

  /**
    * Converts sequence of aggregations into one aggregation with subaggregations
    * @example    `Seq(FakeNestedAgg("a"), FakeNestedAgg("b"), "FakeTermAgg("c"))` will become:
    *             Some(
    *               FakeNestedAgg("a", subAggregations = Seq(
    *                 FakeNestedAgg("b", subAggregations = Seq(
    *                   FakeTermAgg("c")
    *                 ))
    *               ))
    *             )
    */
  def seqAggsToSubAggs(aggs: Seq[FakeAgg]): Option[FakeAgg] =
    aggs.reverse.foldLeft(None: Option[FakeAgg])((acc, cur) => {
      acc match {
        case None => Some(cur)
        case Some(subs) =>
          val x = cur.addSubAggs(subs)
          Some(x)
      }
    })
}

case class FakeTermAgg(name: String, subAggregations: Seq[FakeAgg] = Seq.empty, field: String = "") extends FakeAgg {
  def field(path: String): FakeTermAgg = this.copy(field = path)

  override def withSubs(subs: Seq[FakeAgg]): FakeAgg = this.copy(subAggregations = subs)
  override def convertToReal(): Aggregation = {
    val subs = this.subAggregations.map(_.convertToReal())
    termsAggregation(this.name).field(this.field).subAggregations(subs).size(50)
  }
}

case class FakeNestedAgg(name: String, path: String, subAggregations: Seq[FakeAgg] = Seq.empty) extends FakeAgg {
  override def withSubs(subs: Seq[FakeAgg]): FakeAgg = this.copy(subAggregations = subs)
  override def convertToReal(): Aggregation = {
    val subs = this.subAggregations.map(_.convertToReal())
    nestedAggregation(name, path).subAggregations(subs)
  }
}
