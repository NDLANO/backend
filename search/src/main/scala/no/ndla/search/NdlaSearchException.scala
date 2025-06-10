/*
 * Part of NDLA search
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import com.sksamuel.elastic4s.RequestFailure

case class NdlaSearchException[T](
    message: String,
    rf: Option[RequestFailure] = None,
    ex: Option[Throwable] = None,
    request: Option[T] = None
) extends RuntimeException(message)

object NdlaSearchException {
  def apply[T](request: T, rf: RequestFailure): NdlaSearchException[T] = {
    val msg =
      s"""Got error from elasticsearch:
        |  Status: ${rf.status}
        |  Error: ${rf.error}
        |  Caused by request: $request
        |""".stripMargin

    new NdlaSearchException(msg, Some(rf))
  }

  def apply[T](msg: String, ex: Throwable): NdlaSearchException[T] = {
    new NdlaSearchException(msg, ex = Some(ex))
  }
}
