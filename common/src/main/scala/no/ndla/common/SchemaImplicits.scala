/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.common

import sttp.tapir.Schema

trait SchemaImplicits {
  implicit def requiredSeq[T](implicit s: Schema[T]): Schema[Seq[T]] =
    s.asIterable[Seq].copy(isOptional = false)
  implicit def requiredList[T](implicit s: Schema[T]): Schema[List[T]] =
    s.asIterable[List].copy(isOptional = false)
}
