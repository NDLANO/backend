/*
 * Part of NDLA backend.network.main
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients.http

import io.circe.Encoder
import no.ndla.common.CirceUtil

import scala.util.Try
import sttp.client3.quick.*
import sttp.model.Method

class HttpClient {}

object HttpClient {
  private val backend = simpleHttpClient
  // TODO: shorthands? def get(url: String): Try[Resp] = Req("GET", url, Map.empty, None).send()

  def send(req: Req): Try[Response] = Try {
    val uri         = uri"${req.url}"
    val method      = Method(req.method)
    val sttpRequest = quickRequest.method(method, uri)
    val response    = backend.send(sttpRequest)
    val headers     = response.headers.map(h => h.name -> h.value).groupMap(_._1)(_._2)
    Response(
      body = response.body,
      statusCode = response.code.code,
      responseHeaders = headers
    )
  }

  def send[T: Encoder](jsonReq: JsonReq[T]): Try[Response] = {
    val newHeaders = jsonReq.headers + ("Content-Type" -> "application/json")
    val stringBody = CirceUtil.toJsonString(jsonReq.body)
    send(Req(jsonReq.method, jsonReq.url, newHeaders, Some(stringBody)))
  }
}
