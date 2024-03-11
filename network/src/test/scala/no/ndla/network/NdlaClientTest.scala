/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import no.ndla.common.CorrelationID
import no.ndla.network.model.NdlaRequest
import no.ndla.network.tapir.auth.TokenUser
import org.json4s.DefaultFormats

import org.scalatest.TryValues._
import sttp.client3.{Response, SimpleHttpClient, SpecifyAuthScheme, UriContext}
import sttp.model.StatusCode

class NdlaClientTest extends UnitSuite with NdlaClient {

  case class TestObject(id: String, verdi: String)

  val ParseableContent: String =
    """
      |{
      |  "id": "1",
      |  "verdi": "This is the value"
      |}
    """.stripMargin

  val httpClientMock: SimpleHttpClient = mock[SimpleHttpClient]
  val ndlaClient: NdlaClient = new NdlaClient {
    override val client = httpClientMock
  }

  override def beforeEach(): Unit = {
    CorrelationID.clear()
    reset(httpClientMock)
  }

  test("That a HttpRequestException is returned when receiving an http-error") {
    val httpRequestMock  = mock[NdlaRequest]
    val httpResponseMock = mock[Response[String]]
    when(httpClientMock.send(httpRequestMock)).thenReturn(httpResponseMock)

    when(httpRequestMock.uri).thenReturn(uri"someUrl")
    when(httpResponseMock.isSuccess).thenReturn(false)
    when(httpResponseMock.code).thenReturn(StatusCode(123))
    when(httpResponseMock.statusText).thenReturn("status")
    when(httpResponseMock.body).thenReturn("body-with-error")

    implicit val formats = DefaultFormats
    val result           = ndlaClient.fetch[TestObject](httpRequestMock)

    result.isFailure should be(true)
    result.failure.exception.getMessage should equal(
      "Received error 123 status when calling someUrl. Body was body-with-error"
    )
  }

  test("That a HttpRequestException is returned when response is not parseable") {
    val unparseableResponse = "This string cannot be parsed to a TestObject"
    val httpRequestMock     = mock[NdlaRequest]
    val httpResponseMock    = mock[Response[String]]
    when(httpClientMock.send(httpRequestMock)).thenReturn(httpResponseMock)

    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpResponseMock.body).thenReturn(unparseableResponse)

    implicit val formats = DefaultFormats
    val result           = ndlaClient.fetch[TestObject](httpRequestMock)
    result.isFailure should be(true)
    result.failure.exception.getMessage should equal(s"Could not parse response with body: $unparseableResponse")
  }

  test("That a testObject is returned when no error is returned and content is parseable") {
    val httpRequestMock  = mock[NdlaRequest]
    val httpResponseMock = mock[Response[String]]
    when(httpClientMock.send(httpRequestMock)).thenReturn(httpResponseMock)

    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    implicit val formats = DefaultFormats
    val result           = ndlaClient.fetch[TestObject](httpRequestMock)
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    verify(httpRequestMock, never).header(any[String], any[String])
  }

  test("That CorrelationID is added to request if set on ThreadContext") {
    CorrelationID.set(Some("correlation-id"))

    val httpRequestMock  = mock[NdlaRequest]
    val httpResponseMock = mock[Response[String]]
    when(httpClientMock.send(httpRequestMock)).thenReturn(httpResponseMock)
    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpRequestMock.header(eqTo("X-Correlation-ID"), eqTo("correlation-id"))).thenReturn(httpRequestMock)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    implicit val formats = DefaultFormats
    val result           = ndlaClient.fetch[TestObject](httpRequestMock)
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    verify(httpRequestMock, times(1)).header(eqTo("X-Correlation-ID"), eqTo("correlation-id"))
  }

  test("That BasicAuth header is added to request when user and password is defined") {
    val user     = "user"
    val password = "password"

    val httpRequestMock  = mock[NdlaRequest]
    val httpResponseMock = mock[Response[String]]
    val authMock         = mock[SpecifyAuthScheme[Any, String, Any]]
    when(httpClientMock.send(httpRequestMock)).thenReturn(httpResponseMock)
    doReturn(authMock).when(httpRequestMock).auth

    doReturn(httpRequestMock).when(authMock).basic(any, any)

    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    val result = ndlaClient.fetchWithBasicAuth[TestObject](httpRequestMock, user, password)
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    verify(authMock, times(1)).basic(eqTo(user), eqTo(password))
    verify(httpRequestMock, never).header(any[String], any[String])
  }

  test("That Authorization header is added to request if set on Thread") {
    val httpRequestMock  = mock[NdlaRequest]
    val httpResponseMock = mock[Response[String]]
    when(httpClientMock.send(httpRequestMock)).thenReturn(httpResponseMock)

    val authHeaderKey   = "Authorization"
    val authHeader      = "abc"
    val authHeaderValue = s"Bearer $authHeader"
    val user            = TokenUser("id", Set.empty, Some(authHeader))

    doReturn(httpRequestMock).when(httpRequestMock).header(eqTo(authHeaderKey), eqTo(authHeaderValue), any)
    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    val result = ndlaClient.fetchWithForwardedAuth[TestObject](httpRequestMock, Some(user))
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    verify(httpRequestMock, times(1)).header(eqTo(authHeaderKey), eqTo(authHeaderValue), any)
  }

  test("That fetchRawWithForwardedAuth can handle empty bodies") {
    val httpRequestMock  = mock[NdlaRequest]
    val httpResponseMock = mock[Response[String]]
    when(httpClientMock.send(httpRequestMock)).thenReturn(httpResponseMock)
    val authHeaderKey   = "Authorization"
    val authHeader      = "abc"
    val authHeaderValue = s"Bearer $authHeader"
    val user            = TokenUser("id", Set.empty, Some(authHeader))
    when(httpResponseMock.body).thenReturn("")
    when(httpResponseMock.isSuccess).thenReturn(true)
    when(httpResponseMock.code).thenReturn(StatusCode(204))

    when(httpRequestMock.header(eqTo(authHeaderKey), eqTo(authHeaderValue), any)).thenReturn(httpRequestMock)

    val result = ndlaClient.fetchWithForwardedAuth[TestObject](httpRequestMock, Some(user))
    result.isSuccess should be(false)

    val rawResult = ndlaClient.fetchRawWithForwardedAuth(httpRequestMock, Some(user))
    rawResult.isSuccess should be(true)
    rawResult.get.body should be("")
    rawResult.get.code.code should be(204)
  }
}
