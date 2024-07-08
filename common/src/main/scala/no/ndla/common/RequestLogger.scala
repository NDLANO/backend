/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import sttp.tapir.model.ServerRequest

object RequestLogger {

  def pathWithQueryParams(requestPath: String, queryString: String): String = {
    val query = if (queryString.nonEmpty) s"?${queryString}" else queryString
    s"$requestPath$query"
  }

  def pathWithQueryParams(req: ServerRequest): String = {
    RequestLogger.pathWithQueryParams(
      requestPath = s"/${req.uri.path.mkString("/")}",
      queryString = req.queryParameters.toString(false)
    )
  }

  def beforeRequestLogString(req: ServerRequest): String =
    beforeRequestLogString(
      method = req.method.toString(),
      requestPath = s"/${req.uri.path.mkString("/")}",
      queryString = req.queryParameters.toString(false)
    )

  def beforeRequestLogString(method: String, requestPath: String, queryString: String): String = {
    val path = pathWithQueryParams(requestPath, queryString)
    s"$method $path"
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
