/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import sttp.tapir.model.CommaSeparated
import sttp.tapir.{Codec, CodecFormat, EndpointInput, query}

/** A query parameter that is a comma separated list of values. Wraps the `CommaSeparated` type from `sttp.model`.
  *
  * This is a workaround for the fact that using `CommaSeparated` directly with a default value results in weird
  * behavior in the generated swagger documentation.
  *
  * See: https://github.com/softwaremill/tapir/issues/3581
  */
object CommaSeparatedList {
  type CommaSeparatedList[T] = Option[CommaSeparated[T]]

  def listQuery[T](
      name: String
  )(implicit codec: Codec[String, T, CodecFormat.TextPlain]): EndpointInput.Query[CommaSeparatedList[T]] = {
    query[CommaSeparatedList[T]](name).default(None)
  }

  implicit class sepOps[T](val sep: CommaSeparatedList[T]) extends AnyVal {
    def values: List[T] = sep.map(_.values).getOrElse(List.empty)
  }
}
