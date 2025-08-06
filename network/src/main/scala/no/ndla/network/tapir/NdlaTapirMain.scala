/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import no.ndla.common.Environment.setPropsFromEnv
import no.ndla.common.configuration.BaseProps
import no.ndla.common.logging.logTaskTime
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.jul.Log4jBridgeHandler
import org.log4s.{Logger, getLogger}
import sttp.tapir.server.jdkhttp.HttpServer

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, DurationInt}
import scala.io.Source
import scala.util.{Try, boundary}

trait NdlaTapirMain[T <: TapirApplication] {
  val logger: Logger = getLogger

  val props: BaseProps
  val componentRegistry: T
  def warmup(): Unit
  def beforeStart(): Unit
  var server: Option[HttpServer] = None

  private def logCopyrightHeader(): Unit = {
    if (!props.DisableLicense) {
      logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)
    }
  }

  def startServer(name: String, port: Int)(warmupFunc: => Unit): Unit = {
    val server = componentRegistry.Routes.startJdkServerAsync(name, port)(warmupFunc)
    this.server = Some(server)
    // NOTE: Since JdkHttpServer does not block, we need to block the main thread to keep the application alive
    synchronized { wait() }
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
  } else {
    componentRegistry.healthController.setWarmedUp()
  }

  private def waitForActiveRequests(timeout: Duration): Unit = boundary {
    val startTime      = System.currentTimeMillis()
    val activeRequests = componentRegistry.Routes.activeRequests.get()
    logTaskTime(s"Waiting for $activeRequests active requests to finish...", timeout, logTaskStart = true) {
      while (componentRegistry.Routes.activeRequests.get() > 0) {
        if (System.currentTimeMillis() - startTime > timeout.toMillis) {
          logger.warn(s"Timeout of $timeout reached while waiting for active requests to finish.")
          boundary.break()
        }
        Thread.sleep(100)
      }
    }
  }

  private val gracefulShutdownTimeout   = 30.seconds
  private def setupShutdownHook(): Unit = sys.addShutdownHook {
    logger.info("Shutting down gracefully...")
    componentRegistry.healthController.setShuttingDown()
    this.server match {
      case Some(server) =>
        val gracePeriod = props.ReadinessProbeDetectionTimeoutSeconds
        logger.info(s"Waiting $gracePeriod for shutdown to be detected before stopping...")
        Thread.sleep(gracePeriod.toMillis)
        logger.info("Grace period finished, stopping server after requests are processed...")
        waitForActiveRequests(gracefulShutdownTimeout)
        server.stop(0)
      case None =>
        logger.error("Got shutdown signal, but no server is running, this seems weird.")
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
    Try(startServer(props.ApplicationName, props.ApplicationPort) {
      beforeStart()
      performWarmup()
    }).recover { ex =>
      logger.error(ex)("Failed to start server, exiting...")
    }
  }

  def run(args: Array[String]): Try[Unit] = {
    setPropsFromEnv()
    Log4jBridgeHandler.install(true, null, true)
    if (args.contains("--save-swagger")) {
      Try(componentRegistry.swagger.saveSwagger())
    } else {
      props.throwIfFailedProps()
      runServer()
    }
  }
}
