/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout

import scala.collection.mutable.ListBuffer

object Layout {
  lazy val prebuiltLayout: PatternLayout = PatternLayout
    .newBuilder()
    .withPattern("[%level] %C.%M#%L: %msg%n")
    .build()
}

class BufferedLogAppender
    extends AbstractAppender("BufferedAppender", null, Layout.prebuiltLayout, false, Array.empty) {
  private val logQueue  = ListBuffer.empty[String]
  def clear(): Unit     = logQueue.clear()
  def printLogs(): Unit = logQueue.foreach(print)
  def getAndClearLogs: List[String] = {
    val toReturn = logQueue.toList
    logQueue.clear()
    toReturn
  }

  override def append(event: LogEvent): Unit = {
    val logString = getLayout.toSerializable(event).toString
    logQueue.append(logString)
  }
}
