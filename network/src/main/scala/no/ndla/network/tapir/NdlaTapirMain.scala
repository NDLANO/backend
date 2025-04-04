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
import org.log4s.{Logger, getLogger}

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

trait NdlaTapirMain[T <: TapirApplication] {
  val logger: Logger = getLogger

  val props: BaseProps
  val componentRegistry: T
  def warmup(): Unit
  def beforeStart(): Unit

  private def logCopyrightHeader(): Unit = {
    if (!props.DisableLicense) {
      logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)
    }
  }

  def startServer(name: String, port: Int)(warmupFunc: => Unit): Unit = {
    componentRegistry.Routes.startJdkServer(name, port)(warmupFunc)
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

  def runServer(): Try[Unit] = {
    logCopyrightHeader()
    Try(startServer(props.ApplicationName, props.ApplicationPort) {
      beforeStart()
      performWarmup()
    }).recover { ex =>
      logger.error(ex)("Failed to start server, exiting...")
    }

  }

  def run(args: Array[String]): Try[Unit] = {
    setPropsFromEnv()
    if (args.contains("--save-swagger")) {
      Try(componentRegistry.swagger.saveSwagger())
    } else {
      props.throwIfFailedProps()
      runServer()
    }
  }
}
