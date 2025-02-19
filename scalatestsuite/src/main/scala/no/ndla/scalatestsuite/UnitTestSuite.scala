/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.scalatest.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.io.IOException
import java.net.ServerSocket
import scala.util.Properties.{propOrNone, setProp}
import scala.util.{Failure, Success, Try}

abstract class UnitTestSuite
    extends AnyFunSuite
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with MockitoSugar
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with TestSuiteLoggingSetup {

  setPropEnv("DISABLE_LICENSE", "true"): Unit

  def setPropEnv(key: String, value: String): String = setProp(key, value)

  def setPropEnv(map: Map[String, String]): Unit = {
    map.foreach { case (key, value) => setPropEnv(key, value) }
  }

  def setPropEnv(keyValueTuples: (String, String)*): Unit = setPropEnv(keyValueTuples.toMap)

  def getPropEnv(key: String): Option[String] = {
    propOrNone(key)
  }

  def getPropEnvs(keys: String*): Map[String, String] = getPropEnvsFromSeq(keys)

  def getPropEnvsFromSeq(keys: Seq[String]): Map[String, String] = {
    keys.flatMap(key => getPropEnv(key).map(value => key -> value)).toMap
  }

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
