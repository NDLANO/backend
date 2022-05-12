/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import no.ndla.network.model.HttpRequestException
import no.ndla.oembedproxy.model.OEmbed
import no.ndla.oembedproxy.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.anyString
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.{Failure, Success}

class OEmbedProxyControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val swagger = new OEmbedSwagger
  lazy val controller  = new OEmbedProxyController
  addServlet(controller, props.OembedProxyControllerMountPoint)

  val oembed = OEmbed(
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
    get("/oembed-proxy/v1/oembed?url=https://h5p-test.ndla.no/resource/bae851c6-0e98-411d-bd92-ec2ab8fce730") {
      status should equal(200)
    }
  }

  test("h5p url should return 404 if not found") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(true)
    when(exception.getMessage).thenReturn("")
    when(oEmbedService.get(anyString, any[Option[String]], any[Option[String]])).thenReturn(Failure(exception))
    get("/oembed-proxy/v1/oembed?url=https://h5p-test.ndla.no/resource/bae851c6-0e98-411d-bd92-ec2ab8fce730") {
      status should equal(404)
    }
  }

}
