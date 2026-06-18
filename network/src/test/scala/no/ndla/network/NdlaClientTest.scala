/*
 * Part of NDLA network
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import io.prometheus.metrics.model.snapshots.CounterSnapshot
import no.ndla.common.CorrelationID
import no.ndla.network.model.NdlaRequest
import no.ndla.network.tapir.NdlaPrometheusRegistry
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.TryValues.*
import sttp.client4.{Response, UriContext, WebSocketSyncBackend, basicRequest}
import sttp.model.{Method, RequestMetadata, StatusCode}

import scala.jdk.CollectionConverters.*

class NdlaClientTest extends UnitSuite {

  case class TestObject(id: String, verdi: String)

  val ParseableContent: String = """
      |{
      |  "id": "1",
      |  "verdi": "This is the value"
      |}
    """.stripMargin

  val httpClientMock: WebSocketSyncBackend = mock[WebSocketSyncBackend]
  lazy val ndlaClient: NdlaClient          = new NdlaClient {
    override val client: WebSocketSyncBackend = httpClientMock
  }

  override def beforeEach(): Unit = {
    CorrelationID.clear()
    reset(httpClientMock)
  }

  private def newRequest: NdlaRequest    = basicRequest.get(uri"someUrl").response(sttp.client4.asStringAlways)
  private def captureSent(): NdlaRequest = {
    val captor = ArgumentCaptor.forClass(classOf[NdlaRequest])
    verify(httpClientMock).send(captor.capture())
    captor.getValue
  }

  test("That a HttpRequestException is returned when receiving an http-error") {
    val httpRequest      = newRequest
    val httpResponseMock = new Response(
      body = "body-with-error",
      code = StatusCode(123),
      statusText = "status",
      headers = Seq.empty,
      history = List.empty,
      request = RequestMetadata(Method.GET, uri"someUrl", List.empty),
    )

    when(httpClientMock.send(any[NdlaRequest])).thenReturn(httpResponseMock)

    import io.circe.generic.auto._
    val result = ndlaClient.fetch[TestObject](httpRequest)

    result.isFailure should be(true)
    result.failure.exception.getMessage should equal(
      "Received error 123 status when calling someUrl. Body was body-with-error"
    )
  }

  test("That a HttpRequestException is returned when response is not parseable") {
    val unparseableResponse = "This string cannot be parsed to a TestObject"
    val httpRequest         = newRequest
    val httpResponseMock    = mock[Response[String]]
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(httpResponseMock)

    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpResponseMock.body).thenReturn(unparseableResponse)

    import io.circe.generic.auto._
    val result = ndlaClient.fetch[TestObject](httpRequest)
    result.isFailure should be(true)
    result.failure.exception.getMessage should equal(s"Could not parse response with body: $unparseableResponse")
  }

  test("That a testObject is returned when no error is returned and content is parseable") {
    val httpRequest      = newRequest
    val httpResponseMock = mock[Response[String]]
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(httpResponseMock)

    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    import io.circe.generic.auto._
    val result = ndlaClient.fetch[TestObject](httpRequest)
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    val sent = captureSent()
    sent.headers.exists(_.is("X-Correlation-ID")) should be(false)
    sent.headers.exists(_.is("Authorization")) should be(false)
  }

  test("That CorrelationID is added to request if set on ThreadContext") {
    CorrelationID.set(Some("correlation-id"))

    val httpRequest      = newRequest
    val httpResponseMock = mock[Response[String]]
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(httpResponseMock)
    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    import io.circe.generic.auto._
    val result = ndlaClient.fetch[TestObject](httpRequest)
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    val sent = captureSent()
    sent.headers.find(_.is("X-Correlation-ID")).map(_.value) should equal(Some("correlation-id"))
  }

  test("That BasicAuth header is added to request when user and password is defined") {
    val user     = "user"
    val password = "password"

    val httpRequest      = newRequest
    val httpResponseMock = mock[Response[String]]
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(httpResponseMock)
    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    import io.circe.generic.auto._
    val result = ndlaClient.fetchWithBasicAuth[TestObject](httpRequest, user, password)
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    val sent       = captureSent()
    val authHeader = sent.headers.find(_.is("Authorization")).map(_.value)
    val expected   = "Basic " + java.util.Base64.getEncoder.encodeToString(s"$user:$password".getBytes("UTF-8"))
    authHeader should equal(Some(expected))
  }

  test("That Authorization header is added to request if set on Thread") {
    val httpRequest      = newRequest
    val httpResponseMock = mock[Response[String]]
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(httpResponseMock)

    val authHeader      = "abc"
    val authHeaderValue = s"Bearer $authHeader"
    val user            = TokenUser("id", Set.empty, Some(authHeader))

    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    import io.circe.generic.auto._
    val result = ndlaClient.fetchWithForwardedAuth[TestObject](httpRequest, Some(user))
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    val sent = captureSent()
    sent.headers.find(_.is("Authorization")).map(_.value) should equal(Some(authHeaderValue))
  }

  private def clientRequestCount(host: String, method: String, status: String): Double = NdlaPrometheusRegistry
    .registry
    .scrape()
    .iterator()
    .asScala
    .collectFirst {
      case cs: CounterSnapshot if cs.getMetadata.getPrometheusName.startsWith("ndla_http_client_requests") => cs
    }
    .flatMap { cs =>
      cs.getDataPoints
        .asScala
        .find { dp =>
          val l = dp.getLabels
          l.get("host") == host && l.get("method") == method && l.get("status") == status
        }
    }
    .map(_.getValue)
    .getOrElse(0.0d)

  test("That metrics are recorded for outgoing requests") {
    val targetUri        = uri"http://my-service/path"
    val httpRequest      = basicRequest.get(targetUri).response(sttp.client4.asStringAlways)
    val httpResponseMock = new Response(
      body = ParseableContent,
      code = StatusCode(200),
      statusText = "OK",
      headers = Seq.empty,
      history = List.empty,
      request = RequestMetadata(Method.GET, targetUri, List.empty),
    )
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(httpResponseMock)

    val before = clientRequestCount("my-service", "GET", "200")
    ndlaClient.fetchRaw(httpRequest).isSuccess should be(true)
    val after = clientRequestCount("my-service", "GET", "200")
    after should be(before + 1.0d)
  }

  test("That fetchRawWithForwardedAuth can handle empty bodies") {
    val httpRequest      = newRequest
    val httpResponseMock = new Response(
      body = "",
      code = StatusCode(204),
      statusText = "status",
      headers = Seq.empty,
      history = List.empty,
      request = RequestMetadata(Method.GET, uri"someUrl", List.empty),
    )
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(httpResponseMock)
    val authHeader = "abc"
    val user       = TokenUser("id", Set.empty, Some(authHeader))

    import io.circe.generic.auto._
    val result = ndlaClient.fetchWithForwardedAuth[TestObject](httpRequest, Some(user))
    result.isSuccess should be(false)

    val rawResult = ndlaClient.fetchRawWithForwardedAuth(httpRequest, Some(user))
    rawResult.isSuccess should be(true)
    rawResult.get.body should be("")
    rawResult.get.code.code should be(204)
  }
}
