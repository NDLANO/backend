/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Environment.setPropsFromEnv
import no.ndla.common.configuration.BaseProps
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.jul.Log4jBridgeHandler
import sttp.tapir.server.netty.sync.NettySyncServerBinding

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

trait NdlaTapirMain[T <: TapirApplication[?]] extends StrictLogging {
  val props: BaseProps
  val componentRegistry: T
  def warmup(): Unit
  def beforeStart(): Unit
  var serverBinding: Option[NettySyncServerBinding] = None

  private def logCopyrightHeader(): Unit = {
    if (!props.DisableLicense) {
      logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)
    }
  }

  def startServerAndWait(name: String, port: Int)(onStartup: NettySyncServerBinding => Unit): Unit = componentRegistry
    .routes
    .startServerAndWait(name, port)(onStartup)

  private def performWarmup(): Unit =
    if (!props.disableWarmup) {
      import scala.concurrent.ExecutionContext.Implicits.global
      val _ = Future {
        // Since we don't really have a good way to check whether server is ready lets just wait a bit
        Thread.sleep(500)
        val warmupStart = System.currentTimeMillis()
        logger.info("Starting warmup procedure...")

        warmup()

        val warmupTime = System.currentTimeMillis() - warmupStart
        logger.info(s"Warmup procedure finished in ${warmupTime}ms.")
      }
    } else {
      componentRegistry.healthController.setWarmedUp()
    }

  private def setupShutdownHook(): Unit = sys.addShutdownHook {
    componentRegistry.healthController.setShuttingDown()
    this.serverBinding match {
      case Some(server) =>
        // Make sure to wait for readiness probe to fail before we stop the server
        val readinessProbeDelay = props.ReadinessProbeDetectionTimeoutSeconds
        logger.info(s"Waiting $readinessProbeDelay for shutdown to be detected before stopping...")
        Thread.sleep(readinessProbeDelay.toMillis)
        logger.info("Stopping server gracefully...")
        server.stop()
      case None => logger.error("Got shutdown signal, but no server is running, this seems weird.")
    }
    shutdownLogger()
  }: Unit

  /** We require shutting down logger context manually since we implement our own shutdown hook that logs for longer
    * than the default logger shutdown handler will allow
    */
  private def shutdownLogger(): Unit = {
    LoggerContext.getContext(false).stop()
  }

  private def runServer(): Try[Unit] = {
    logCopyrightHeader()
    setupShutdownHook()
    Try(
      startServerAndWait(props.ApplicationName, props.ApplicationPort) { binding =>
        this.serverBinding = Some(binding)
        beforeStart()
        performWarmup()
      }
    ).recover { ex =>
      logger.error("Failed to start server, exiting...", ex)
    }
  }

  def run(args: Array[String]): Try[Unit] = {
    setPropsFromEnv()
    Log4jBridgeHandler.install(true, null, true)
    if (args.contains("--generate-openapi")) {
      Try(componentRegistry.swagger.saveSwagger())
    } else {
      props.throwIfFailedProps()
      runServer()
    }
  }
}
