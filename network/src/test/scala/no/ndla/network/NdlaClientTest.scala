/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import javax.servlet.http.HttpServletRequest
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.TryValues._
import scalaj.http.{HttpRequest, HttpResponse}

class NdlaClientTest extends UnitSuite with NdlaClient {

  case class TestObject(id: String, verdi: String)

  val ParseableContent =
    """
      |{
      |  "id": "1",
      |  "verdi": "This is the value"
      |}
    """.stripMargin

  val ndlaClient: NdlaClient = new NdlaClient

  override def beforeEach() = {
    CorrelationID.clear()
  }

  test("That a HttpRequestException is returned when receiving an http-error") {
    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpRequestMock.url).thenReturn("someUrl")

    when(httpResponseMock.isError).thenReturn(true)
    when(httpResponseMock.code).thenReturn(123)
    when(httpResponseMock.statusLine).thenReturn("status")
    when(httpResponseMock.body).thenReturn("body-with-error")

    val result = ndlaClient.fetch[TestObject](httpRequestMock)

    result.isFailure should be(true)
    result.failure.exception.getMessage should equal(
      "Received error 123 status when calling someUrl. Body was body-with-error")
  }

  test("That a HttpRequestException is returned when response is not parseable") {
    val unparseableResponse = "This string cannot be parsed to a TestObject"
    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpResponseMock.isError).thenReturn(false)
    when(httpResponseMock.body).thenReturn(unparseableResponse)

    val result = ndlaClient.fetch[TestObject](httpRequestMock)
    result.isFailure should be(true)
    result.failure.exception.getMessage should equal(s"Could not parse response with body: $unparseableResponse")
  }

  test("That a testObject is returned when no error is returned and content is parseable") {
    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpResponseMock.isError).thenReturn(false)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    val result = ndlaClient.fetch[TestObject](httpRequestMock)
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    verify(httpRequestMock, never).header(any[String], any[String])
  }

  test("That CorrelationID is added to request if set on ThreadContext") {
    CorrelationID.set(Some("correlation-id"))

    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.header(eqTo("X-Correlation-ID"), eqTo("correlation-id"))).thenReturn(httpRequestMock)
    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpResponseMock.isError).thenReturn(false)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    val result = ndlaClient.fetch[TestObject](httpRequestMock)
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    verify(httpRequestMock, times(1)).header(eqTo("X-Correlation-ID"), eqTo("correlation-id"))
  }

  test("That BasicAuth header is added to request when user and password is defined") {
    val user = "user"
    val password = "password"

    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.auth(eqTo(user), eqTo(password))).thenReturn(httpRequestMock)
    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpResponseMock.isError).thenReturn(false)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    val result = ndlaClient.fetchWithBasicAuth[TestObject](httpRequestMock, user, password)
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    verify(httpRequestMock, times(1)).auth(eqTo(user), eqTo(password))
    verify(httpRequestMock, never).header(any[String], any[String])
  }

  test("That Authorization header is added to request if set on Thread") {
    val servletRequestMock = mock[HttpServletRequest]
    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]
    val authHeaderKey = "Authorization"
    val authHeader = "abc"

    when(servletRequestMock.getHeader(eqTo(authHeaderKey))).thenReturn(authHeader)
    AuthUser.set(servletRequestMock)

    when(httpRequestMock.header(eqTo(authHeaderKey), eqTo(authHeader))).thenReturn(httpRequestMock)
    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpResponseMock.isError).thenReturn(false)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    val result = ndlaClient.fetchWithForwardedAuth[TestObject](httpRequestMock)
    result.isSuccess should be(true)
    result.get.id should equal("1")
    result.get.verdi should equal("This is the value")

    verify(httpRequestMock, times(1)).header(eqTo(authHeaderKey), eqTo(authHeader))
  }

  test("That fetchRawWithForwardedAuth can handle empty bodies") {
    val servletRequestMock = mock[HttpServletRequest]
    val httpRequestMock = mock[HttpRequest]
    val authHeaderKey = "Authorization"
    val authHeader = "abc"

    when(servletRequestMock.getHeader(eqTo(authHeaderKey))).thenReturn(authHeader)
    AuthUser.set(servletRequestMock)

    val response = HttpResponse("", 204, Map.empty)

    when(httpRequestMock.header(eqTo(authHeaderKey), eqTo(authHeader))).thenReturn(httpRequestMock)
    when(httpRequestMock.asString).thenReturn(response)

    val result = ndlaClient.fetchWithForwardedAuth[TestObject](httpRequestMock)
    result.isSuccess should be(false)

    val rawResult = ndlaClient.fetchRawWithForwardedAuth(httpRequestMock)
    rawResult.isSuccess should be(true)
    rawResult.get.body should be("")
    rawResult.get.code should be(204)
  }
}
