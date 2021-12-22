/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import javax.servlet.http.HttpServletRequest

import org.mockito.ArgumentMatchers._

class ApplicationUrlTest extends UnitSuite {

  val httpRequest = mock[HttpServletRequest]
  val servername = "unittest.testesen.no"
  val scheme = "testscheme"
  val port = 666
  val path = "dette/er/en/test/path"

  override def beforeEach(): Unit = {
    reset(httpRequest)
    when(httpRequest.getServerName).thenReturn(servername)
    when(httpRequest.getHeader(anyString)).thenReturn(null)
    when(httpRequest.getScheme).thenReturn(scheme)
    when(httpRequest.getServerPort).thenReturn(port)
    when(httpRequest.getServletPath).thenReturn(path)
  }

  test("That applicationUrl returns default wnen header is not defined") {
    withEnv("NDLA_ENVIRONMENT", None) {
      ApplicationUrl.set(httpRequest)
      ApplicationUrl.get should equal(s"${scheme}://${servername}:${port}${path}/")
    }
  }

  test("That applicationUrl returns http wnen header is not defined and port is 80") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getScheme).thenReturn("http")
      when(httpRequest.getServerPort).thenReturn(80)
      ApplicationUrl.set(httpRequest)
      ApplicationUrl.get should equal(s"http://${servername}${path}/")
    }
  }

  test("That applicationUrl returns https wnen header is not defined and port is 443") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getScheme).thenReturn("https")
      when(httpRequest.getServerPort).thenReturn(443)
      ApplicationUrl.set(httpRequest)
      ApplicationUrl.get should equal(s"https://${servername}${path}/")
    }
  }

  test("That applicationUrl returns http when only x-forwarded-proto header and it is http") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getHeader(ApplicationUrl.X_FORWARDED_PROTO_HEADER)).thenReturn("http")
      ApplicationUrl.set(httpRequest)
      ApplicationUrl.get should equal(s"http://${servername}${path}/")
    }
  }

  test("That applicationUrl returns https when only x-forwarded-proto header and it is https") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getHeader(ApplicationUrl.X_FORWARDED_PROTO_HEADER)).thenReturn("https")
      ApplicationUrl.set(httpRequest)
      ApplicationUrl.get should equal(s"https://${servername}${path}/")
    }
  }

  test("That x-forwarded-proto header for https trumps http port") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getHeader(ApplicationUrl.X_FORWARDED_PROTO_HEADER)).thenReturn("https")
      ApplicationUrl.set(httpRequest)
      when(httpRequest.getServerPort).thenReturn(80)
      ApplicationUrl.get should equal(s"https://${servername}${path}/")
    }
  }

  test("That x-forwarded-proto header for http trumps https port") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getHeader(ApplicationUrl.X_FORWARDED_PROTO_HEADER)).thenReturn("http")
      ApplicationUrl.set(httpRequest)
      when(httpRequest.getServerPort).thenReturn(443)
      ApplicationUrl.get should equal(s"http://${servername}${path}/")
    }
  }

  test("That applicationUrl returns default when headers is unrecognized") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getHeader(ApplicationUrl.X_FORWARDED_PROTO_HEADER)).thenReturn("tullogtoys")
      when(httpRequest.getHeader(ApplicationUrl.FORWARDED_HEADER)).thenReturn("for=1.2.3.4;proto=tullogtoys")
      ApplicationUrl.set(httpRequest)
      ApplicationUrl.get should equal(s"${scheme}://${servername}:${port}${path}/")
    }
  }

  test("That forwarded header for https trumps http port and x-forwarded-header") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getHeader(ApplicationUrl.X_FORWARDED_PROTO_HEADER)).thenReturn("http")
      when(httpRequest.getHeader(ApplicationUrl.FORWARDED_HEADER)).thenReturn("for=1.2.3.4;proto=https")
      ApplicationUrl.set(httpRequest)
      when(httpRequest.getServerPort).thenReturn(80)
      ApplicationUrl.get should equal(s"https://${servername}${path}/")
    }
  }

  test("That forwarded header for http trumps https port and x-forwarded-header") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getHeader(ApplicationUrl.X_FORWARDED_PROTO_HEADER)).thenReturn("https")
      when(httpRequest.getHeader(ApplicationUrl.FORWARDED_HEADER)).thenReturn("for=1.2.3.4;proto=http")
      ApplicationUrl.set(httpRequest)
      when(httpRequest.getServerPort).thenReturn(443)
      ApplicationUrl.get should equal(s"http://${servername}${path}/")
    }
  }

  test("That applicationUrl returns x-forwarded-proto-header when forwarded-header is unrecognized format") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getHeader(ApplicationUrl.X_FORWARDED_PROTO_HEADER)).thenReturn("https")
      when(httpRequest.getHeader(ApplicationUrl.FORWARDED_HEADER)).thenReturn("tullogtoys")
      ApplicationUrl.set(httpRequest)
      ApplicationUrl.get should equal(s"https://${servername}${path}/")
    }
  }

  test("That applicationUrl returns x-forwarded-proto-header when forwarded-header is unrecognized") {
    withEnv("NDLA_ENVIRONMENT", None) {
      when(httpRequest.getHeader(ApplicationUrl.X_FORWARDED_PROTO_HEADER)).thenReturn("https")
      when(httpRequest.getHeader(ApplicationUrl.FORWARDED_HEADER)).thenReturn("for=1.2.3.4;proto=tullogtoys")
      ApplicationUrl.set(httpRequest)
      ApplicationUrl.get should equal(s"https://${servername}${path}/")
    }
  }

  test("That domain is set out of environment if available") {
    withEnv("NDLA_ENVIRONMENT", Some("test")) {
      ApplicationUrl.set(httpRequest)
      val gotten = ApplicationUrl.get
      gotten should equal(s"https://api.test.ndla.no${path}/")
    }
  }
}
