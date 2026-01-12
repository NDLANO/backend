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
import scala.collection.mutable
import scala.concurrent.duration.{Duration, DurationInt}
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
      } catch {
        case _: Throwable =>
      }
    }
    var socket: ServerSocket = null
    try {
      socket = new ServerSocket(0)
      socket.setReuseAddress(true)
      val port = socket.getLocalPort
      closeQuietly(socket)
      return port
    } catch {
      case e: IOException =>
        System.err.println(("Failed to open socket", e))
        cancel("Could not open a local server socket to allocate a test port.")
    } finally {
      if (socket != null) {
        closeQuietly(socket)
      }
    }
    throw new IllegalStateException("Could not find a free TCP/IP port");
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

  def blockUntilSuccess(predicate: () => Try[?], timeout: Duration = 30.seconds): Unit = {
    val startTime                          = System.currentTimeMillis()
    val resultStack: mutable.Stack[Try[?]] = mutable.Stack.empty
    val maxWaitTime                        = 5000

    while (
      resultStack.isEmpty || (resultStack.top.isFailure && (System.currentTimeMillis() - startTime) < timeout.toMillis)
    ) {
      if (resultStack.nonEmpty) {
        val waitTime = Math.min(200L * resultStack.length, maxWaitTime)
        Thread.sleep(waitTime)
      }
      try {
        val result = predicate()
        resultStack.push(result)
      } catch {
        case e: Throwable => println(("problem while testing predicate", e))
      }
    }

    Try(resultStack.top).flatten match {
      case Success(_)  =>
      case Failure(ex) =>
        val header   = s"waited for ${System.currentTimeMillis() - startTime}ms, retries: ${resultStack.length}"
        val failures = resultStack
          .zipWithIndex
          .map { case (res, retryIdx) =>
            s"  retry #$retryIdx: $res"
          }
          .mkString("\n")

        fail(s"Failed waiting for predicate to return Success:\n\n$header\n$failures", ex)
    }
  }

  // Adds method to `Try`s in tests that will fail the test if a `Try` is `Failure`
  // and return the result if it is a `Success`
  implicit class failableTry[T](result: Try[T]) {
    def failIfFailure: T = result match {
      case Success(r)  => r
      case Failure(ex) => fail(
          """Failure gotten when Success was expected :^)
            |See cause exception at the bottom of the stack trace.
            |""".stripMargin,
          ex,
        )
    }
  }
}
