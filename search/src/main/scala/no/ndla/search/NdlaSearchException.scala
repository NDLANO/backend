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
  private def message(
      errorType: String,
      reason: String,
      index: Option[String],
      shard: Option[String],
      requestString: String
  ): String = {
    val indexError = index.map(idx => s"index: $idx").getOrElse("")
    val shardError = shard.map(s => s" shard: $s").getOrElse("")
    s"""SearchError with following content occurred:
       |Error type: $errorType
       |reason: $reason
       |$indexError
       |$shardError
       |Caused by request: $requestString
       |""".stripMargin
  }

  def apply[T](request: T, rf: RequestFailure): NdlaSearchException[T] = {
    if (rf.status != 409) {
      val msg = message(
        rf.error.`type`,
        rf.error.reason,
        rf.error.index,
        rf.error.shard,
        request.toString
      )
      new NdlaSearchException(msg, Some(rf))
    } else {
      new NdlaSearchException("Conflict when indexing document", Some(rf))
    }
  }

  def apply[T](msg: String, ex: Throwable): NdlaSearchException[T] = {
    new NdlaSearchException(msg, ex = Some(ex))
  }
}
