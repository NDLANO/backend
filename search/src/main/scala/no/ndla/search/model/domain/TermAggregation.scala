/*
 * Part of NDLA search
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.search.model.domain

case class Bucket(value: String, count: Int)
case class TermAggregation(
    field: Seq[String],
    sumOtherDocCount: Int,
    docCountErrorUpperBound: Int,
    buckets: Seq[Bucket]
)
