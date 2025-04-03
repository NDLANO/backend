/*
 * Part of NDLA testbase
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.testbase

import org.scalatest.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.io.IOException
import java.net.ServerSocket
import scala.util.{Failure, Success, Try}

trait UnitTestSuiteBase
    extends AnyFunSuite
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with MockitoSugar
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with TestSuiteLoggingSetup {

  def findFreePort: Int = {
    def closeQuietly(socket: ServerSocket): Unit = {
      try {
        socket.close()
      } catch { case _: Throwable => }
    }
    var socket: ServerSocket = null
    try {
      socket = new ServerSocket(0)
      socket.setReuseAddress(true)
      val port = socket.getLocalPort
      closeQuietly(socket)
      return port;
    } catch {
      case e: IOException =>
        System.err.println(("Failed to open socket", e));
    } finally {
      if (socket != null) {
        closeQuietly(socket)
      }
    }
    throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
  }

  def blockUntil(predicate: () => Boolean): Unit = {
    var backoff = 0
    var done    = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200L * backoff)
      backoff = backoff + 1
      try {
        done = predicate()
      } catch {
        case e: Throwable => println(("problem while testing predicate", e))
      }
    }

    require(done, s"Failed waiting for predicate")
  }

  // Adds method to `Try`s in tests that will fail the test if a `Try` is `Failure`
  // and return the result if it is a `Success`
  implicit class failableTry[T](result: Try[T]) {
    def failIfFailure: T = result match {
      case Success(r) => r
      case Failure(ex) =>
        fail(
          """Failure gotten when Success was expected :^)
            |See cause exception at the bottom of the stack trace.
            |""".stripMargin,
          ex
        )
    }
  }
}
