/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import scala.annotation.unused

case class DomainDumpResults[T](totalCount: Long, page: Int, pageSize: Int, results: Seq[T])

object DomainDumpResults {
  implicit def encoder[T](implicit @unused e: Encoder[T]): Encoder[DomainDumpResults[T]] = deriveEncoder
  implicit def decoder[T](implicit @unused d: Decoder[T]): Decoder[DomainDumpResults[T]] = deriveDecoder
}
