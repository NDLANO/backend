/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import scala.deriving.Mirror
import sttp.tapir.Schema
import scala.annotation.unused

object DeriveHelpers extends SchemaImplicits {
  inline def getSchema[T](using m: Mirror.Of[T]): Schema[T] = {

    @unused
    implicit def _requiredSeq[I](implicit s: Schema[I]): Schema[Seq[I]] = requiredSeq[I]
    @unused
    implicit def _requiredList[I](implicit s: Schema[I]): Schema[List[I]] = requiredList[I]
    @unused
    implicit def _requiredSet[I](implicit s: Schema[I]): Schema[Set[I]] = requiredSet[I]

    import sttp.tapir.generic.*
    Schema.derived[T]
  }
}
