/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class DomainDumpResults[T](totalCount: Long, page: Int, pageSize: Int, results: Seq[T])

object DomainDumpResults {
  implicit def encoder[T: Encoder]: Encoder[DomainDumpResults[T]] = deriveEncoder
  implicit def decoder[T: Decoder]: Decoder[DomainDumpResults[T]] = deriveDecoder
}
