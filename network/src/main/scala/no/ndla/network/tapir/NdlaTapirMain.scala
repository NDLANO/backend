/*
 * Part of NDLA network.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import no.ndla.common.Environment.setPropsFromEnv
import no.ndla.common.configuration.BaseProps
import org.http4s.{Request, Response}
import org.log4s.{Logger, getLogger}

import scala.concurrent.Future
import scala.io.Source

trait NdlaTapirMain extends IOApp {
  val logger: Logger = getLogger

  val props: BaseProps
  val app: Kleisli[IO, Request[IO], Response[IO]]
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
    }
  }

  override def run(args: List[String]): IO[ExitCode] = {
    setPropsFromEnv()
    logCopyrightHeader()
    beforeStart()

    logger.info(s"Starting ${props.ApplicationName} on port ${props.ApplicationPort}")
    val server = TapirServer(props.ApplicationName, props.ApplicationPort, app, enableMelody = true)({
      performWarmup()
    })

    server.as(ExitCode.Success)
  }
}
