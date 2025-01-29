/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.{Level, LogManager}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Outcome}

import scala.collection.mutable.ListBuffer

/** Sets up test environment to keep logs and print them if the test fails */
trait TestSuiteLoggingSetup extends AnyFunSuite with BeforeAndAfterEach with BeforeAndAfterAll {
  private val appender      = new BufferedLogAppender()
  private def getNDLALogger = LogManager.getContext(false).asInstanceOf[LoggerContext].getLogger("no.ndla")
  private val beforeAllLog  = ListBuffer.empty[String]

  private def setupLogger(): Unit = {
    if (!appender.isStarted) appender.start()
    val ndlaLogger = getNDLALogger
    ndlaLogger.setLevel(Level.DEBUG)
    ndlaLogger.addAppender(appender)
  }

  override def beforeEach(): Unit = {
    val logs = appender.getAndClearLogs
    beforeAllLog.addAll(logs)
    setupLogger()
    super.beforeEach()
  }

  override def beforeAll(): Unit = {
    setupLogger()
    super.beforeAll()
  }

  override def afterEach(): Unit = {
    val ndlaLogger = getNDLALogger
    ndlaLogger.removeAppender(appender)
    appender.clear()
    super.afterEach()
  }

  override def withFixture(test: NoArgTest): Outcome = {
    val result = super.withFixture(test)
    if (!result.isSucceeded) {
      // If test fails, print the buffered logs
      val testName = s"${this.suiteName}$$'${test.name}'"
      ColoredText.print(Red, s"\n---- Captured Logs for test: $testName ----")
      if (beforeAllLog.nonEmpty) {
        ColoredText.print(Red, s">>> Captured Logs from $suiteName$$beforeAll: >>>")
        beforeAllLog.foreach(print)
        ColoredText.print(Red, s"<<< End of Captured Logs from $suiteName$$beforeAll <<<")
      }
      appender.printLogs()
      ColoredText.print(Red, s"---- End of Captured Logs for test: $testName ----\n")
    }
    result
  }
}
