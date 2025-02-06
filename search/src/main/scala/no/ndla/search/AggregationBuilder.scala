/*
 * Part of NDLA search
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import cats.data.NonEmptySeq
import cats.implicits.toFoldableOps
import com.sksamuel.elastic4s.fields.{ElasticField, NestedField, ObjectField, TextField}
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.aggs.Aggregation
import no.ndla.search.api.{MultiSearchTermsAggregationDTO, TermValueDTO}
import no.ndla.search.model.domain.{Bucket, TermAggregation}

import scala.annotation.tailrec
import scala.util.Try

object AggregationBuilder {
  def buildTermsAggregation(paths: Seq[String], mappings: List[MappingDefinition]): Seq[Aggregation] = {
    val indexRootFields: Seq[ElasticField] = mappings.flatMap(_.properties)
    val aggregationTrees                   = paths.flatMap(p => buildAggregationTreeFromPath(p, indexRootFields).toSeq)
    val initialFakeAggregations            = aggregationTrees.flatMap(FakeAgg.seqAggsToSubAggs(_).toSeq)
    val mergedFakeAggregations             = mergeAllFakeAggregations(initialFakeAggregations)
    mergedFakeAggregations.map(_.convertToReal())
  }

  /** This method merges all the [[FakeAgg]]'s that can be merged together */
  private def mergeAllFakeAggregations(initialFakeAggregations: Seq[FakeAgg]): Seq[FakeAgg] =
    initialFakeAggregations.foldLeft(Seq.empty[FakeAgg])((acc, fakeAgg) => {
      val (hasBeenMerged, merged) = acc.foldLeft((false, Seq.empty[FakeAgg]))((acc, toMerge) => {
        val (curHasBeenMerged, aggs) = acc
        fakeAgg.merge(toMerge) match {
          case Some(merged) => true             -> (aggs :+ merged)
          case None         => curHasBeenMerged -> (aggs :+ toMerge)
        }
      })
      if (hasBeenMerged) merged else merged :+ fakeAgg
    })

  private def buildAggregationTreeFromPath(path: String, fieldsInIndex: Seq[ElasticField]): Option[Seq[FakeAgg]] = {
    @tailrec
    def _buildAggregationRecursive(
        parts: Seq[String],
        fullPath: String,
        fieldsInIndex: Seq[ElasticField],
        remainder: Seq[String],
        parentAgg: Seq[FakeAgg]
    ): Option[(Seq[FakeAgg], Seq[String])] = if (parts.isEmpty) { None }
    else {
      val matchingIndexFields: Seq[ElasticField] = fieldsInIndex.filter(_.name == parts.mkString("."))
      NonEmptySeq.fromSeq(matchingIndexFields) match {
        case None =>
          val (newPath, restOfPath) = parts.splitAt(math.max(parts.size - 1, 1))
          if (parts == newPath) { None }
          else { _buildAggregationRecursive(newPath, fullPath, fieldsInIndex, restOfPath ++ remainder, parentAgg) }
        case Some(fieldsFound) =>
          val fieldTypes    = fieldsFound.map(_.`type`).distinct
          val pathSoFar     = parts.mkString(".")
          val fullPathSoFar = fullPath.split("\\.").reverse.dropWhile(_ != parts.last).reverse.mkString(".")
          val newParent     = newParentAggregation(fullPath, parentAgg, fieldTypes.toList, pathSoFar, fullPathSoFar)

          if (remainder.isEmpty) { Some(newParent -> Seq.empty) }
          else { _buildAggregationRecursive(remainder, fullPath, subfieldsOf(fieldsFound), Seq.empty, newParent) }
      }
    }

    def newParentAggregation(
        fullPath: String,
        parentAgg: Seq[FakeAgg],
        fieldTypes: Seq[String],
        pathSoFar: String,
        fullPathSoFar: String
    ): Seq[FakeAgg] = fieldTypes match {
      case singleType :: Nil if singleType == "nested" =>
        val n = FakeNestedAgg(pathSoFar, fullPathSoFar)
        parentAgg :+ n
      case singleType :: Nil if singleType == "keyword" =>
        val n = FakeTermAgg(pathSoFar).field(fullPath)
        parentAgg :+ n
      case _ => parentAgg
    }

    def subfieldsOf(fieldsFound: NonEmptySeq[ElasticField]): Seq[ElasticField] = fieldsFound.head match {
      case nestedField: NestedField => nestedField.properties
      case objectField: ObjectField => objectField.properties
      case textField: TextField     => textField.fields
      case _                        => Seq.empty
    }

    _buildAggregationRecursive(path.split("\\.").toSeq, path, fieldsInIndex, Seq.empty, Seq.empty).map(_._1)
  }

  def getAggregationsFromResult(response: SearchResponse): Seq[TermAggregation] = {
    getTermsAggregationResults(response.aggs.data)
  }

  private def convertBuckets(buckets: Seq[Map[String, Any]]): Seq[Bucket] = {
    buckets
      .flatMap(bucket => {
        Try {
          val key      = bucket("key").asInstanceOf[String]
          val docCount = bucket("doc_count").asInstanceOf[Int]
          Bucket(key, docCount)
        }.toOption
      })
  }

  private def handleBucketResult(resMap: Map[String, Any], field: Seq[String]): Seq[TermAggregation] = {
    Try {
      val sumOtherDocCount        = resMap("sum_other_doc_count").asInstanceOf[Int]
      val docCountErrorUpperBound = resMap("doc_count_error_upper_bound").asInstanceOf[Int]
      val buckets                 = resMap("buckets").asInstanceOf[Seq[Map[String, Any]]]

      TermAggregation(
        field,
        sumOtherDocCount,
        docCountErrorUpperBound,
        buckets = convertBuckets(buckets)
      )
    }.toOption.toSeq
  }

  private def getTermsAggregationResults(
      aggregationMap: Map[String, Any],
      fields: Seq[String] = Seq.empty,
      foundBuckets: Seq[TermAggregation] = Seq.empty
  ): Seq[TermAggregation] = aggregationMap.toSeq.flatMap { case (key, map) =>
    val newMap = Try(map.asInstanceOf[Map[String, Any]]).getOrElse(Map.empty[String, Any])

    val hasBucketAggregationKeys =
      newMap.contains("buckets") &&
        newMap.contains("sum_other_doc_count") &&
        newMap.contains("doc_count_error_upper_bound")

    if (hasBucketAggregationKeys) {
      handleBucketResult(newMap, fields :+ key)
    } else {
      getTermsAggregationResults(newMap, fields :+ key, foundBuckets)
    }
  }

  def toApiMultiTermsAggregation(agg: TermAggregation): MultiSearchTermsAggregationDTO =
    MultiSearchTermsAggregationDTO(
      field = agg.field.mkString("."),
      sumOtherDocCount = agg.sumOtherDocCount,
      docCountErrorUpperBound = agg.docCountErrorUpperBound,
      values = agg.buckets.map(b =>
        TermValueDTO(
          value = b.value,
          count = b.count
        )
      )
    )

}
