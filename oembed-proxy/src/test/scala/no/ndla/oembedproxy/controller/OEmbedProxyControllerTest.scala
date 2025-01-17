/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import no.ndla.network.model.HttpRequestException
import no.ndla.oembedproxy.model.OEmbedDTO
import no.ndla.oembedproxy.{TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import sttp.client3.Response
import sttp.client3.quick.*
import sttp.model.{Header, Method, RequestMetadata, StatusCode, Uri}

import scala.util.{Failure, Success}

class OEmbedProxyControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  val controller: OEmbedProxyController = new OEmbedProxyController

  val oembed: OEmbedDTO = OEmbedDTO(
    `type` = "rich",
    version = "1.0",
    title = Some("Title"),
    description = None,
    authorName = None,
    authorUrl = None,
    providerName = None,
    providerUrl = None,
    cacheAge = None,
    thumbnailUrl = None,
    thumbnailWidth = None,
    thumbnailHeight = None,
    url = None,
    width = Some(800L),
    height = Some(600L),
    html = Some(
      "<div><iframe loading=\"lazy\" width=\"800\" height=\"600\" allowfullscreen=\"allowfullscreen\" src=\"https://h5p-test.ndla.no/resource/bae851c6-0e98-411d-bd92-ec2ab8fce730\"></iframe><script src=\"https://h5p.org/sites/all/modules/h5p/library/js/h5p-resizer.js\"></script></div>\""
    )
  )

  test("h5p url should return ok if found") {
    when(oEmbedService.get(anyString, any[Option[String]], any[Option[String]])).thenReturn(Success(oembed))
    val requestParams = Map("url" -> "https://h5p-test.ndla.no/resource/bae851c6-0e98-411d-bd92-ec2ab8fce730")
    val url           = uri"http://localhost:$serverPort/oembed-proxy/v1/oembed?$requestParams"
    val response      = simpleHttpClient.send(quickRequest.get(url))
    response.code.code should be(200)
  }

  test("h5p url should return 404 if not found") {
    val responseMock = new Response[String](
      body = "",
      code = StatusCode.NotFound,
      statusText = "",
      headers = List.empty,
      history = List.empty,
      request = new RequestMetadata {
        override def method: Method       = Method.GET
        override def uri: Uri             = uri""
        override def headers: Seq[Header] = Seq.empty
      }
    )
    val exception = new HttpRequestException("bad", Some(responseMock))
    when(oEmbedService.get(anyString, any[Option[String]], any[Option[String]])).thenReturn(Failure(exception))
    when(clock.now()).thenCallRealMethod()
    val requestParams = Map("url" -> "https://h5p-test.ndla.no/resource/bae851c6-0e98-411d-bd92-ec2ab8fce730")
    val url           = uri"http://localhost:$serverPort/oembed-proxy/v1/oembed?$requestParams"
    val response      = simpleHttpClient.send(quickRequest.get(url))
    response.code.code should be(404)
  }

  test("h5p url should return 502 if something bad happens during request") {
    val exception = new HttpRequestException("bad", None) {
      override def is404: Boolean = false
    }
    when(oEmbedService.get(anyString, any[Option[String]], any[Option[String]])).thenReturn(Failure(exception))
    when(clock.now()).thenCallRealMethod()
    val requestParams = Map("url" -> "https://h5p-test.ndla.no/resource/bae851c6-0e98-411d-bd92-ec2ab8fce730")
    val url           = uri"http://localhost:$serverPort/oembed-proxy/v1/oembed?$requestParams"
    val response      = simpleHttpClient.send(quickRequest.get(url))
    response.code.code should be(502)
  }

  test("h5p url should return 500 if generic bad") {
    val failure = Failure(new RuntimeException("bad stuff"))
    when(oEmbedService.get(anyString, any[Option[String]], any[Option[String]])).thenReturn(failure)
    when(clock.now()).thenCallRealMethod()
    val requestParams = Map("url" -> "https://h5p-test.ndla.no/resource/bae851c6-0e98-411d-bd92-ec2ab8fce730")
    val url           = uri"http://localhost:$serverPort/oembed-proxy/v1/oembed?$requestParams"
    val response      = simpleHttpClient.send(quickRequest.get(url))
    response.code.code should be(500)
  }
}
