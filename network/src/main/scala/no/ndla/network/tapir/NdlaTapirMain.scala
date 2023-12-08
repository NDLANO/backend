/*
 * Part of NDLA network.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import no.ndla.common.Environment.setPropsFromEnv
import no.ndla.common.configuration.BaseProps
import org.log4s.{Logger, getLogger}
import scala.concurrent.Future
import scala.io.Source

trait NdlaTapirMain[F[_]] {
  val logger: Logger = getLogger

  val props: BaseProps
  def startServer(name: String, port: Int)(warmupFunc: => Unit): Unit
  def warmup(): Unit
  def beforeStart(): Unit

  private def logCopyrightHeader(): Unit = {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)
  }

  private def performWarmup(): Unit = if (!props.disableWarmup) {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      // Since we don't really have a good way to check whether server is ready lets just wait a bit
      Thread.sleep(500)
      val warmupStart = System.currentTimeMillis()
      logger.info("Starting warmup procedure...")

      warmup()

      val warmupTime = System.currentTimeMillis() - warmupStart
      logger.info(s"Warmup procedure finished in ${warmupTime}ms.")
    }: Unit
  }

  def run(): Unit = {
    setPropsFromEnv()

    logCopyrightHeader()
    beforeStart()
    startServer(props.ApplicationName, props.ApplicationPort) { performWarmup() }
  }
}
