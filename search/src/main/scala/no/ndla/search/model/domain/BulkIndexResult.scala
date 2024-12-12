/*
 * Part of NDLA search
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search.model.domain

case class BulkIndexResult(count: Int, totalCount: Int) {
  def failed: Int     = totalCount - count
  def successful: Int = count
}

object BulkIndexResult {
  def empty: BulkIndexResult = BulkIndexResult(0, 0)
}
