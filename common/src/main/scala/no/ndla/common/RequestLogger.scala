/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

object RequestLogger {

  def beforeRequestLogString(method: String, requestPath: String, queryString: String): String = {
    val query = if (queryString.nonEmpty) s"?${queryString}" else queryString
    s"$method $requestPath$query"
  }

  def afterRequestLogString(
      method: String,
      requestPath: String,
      queryString: String,
      latency: Long,
      responseCode: Int
  ): String = {
    val query = if (queryString.nonEmpty) s"?${queryString}" else queryString
    s"$method $requestPath$query executed in ${latency}ms with code $responseCode"
  }

}
