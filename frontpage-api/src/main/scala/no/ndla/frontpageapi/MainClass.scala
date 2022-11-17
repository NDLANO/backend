/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.{ExitCode, IO, IOApp}
import no.ndla.common.Environment.setPropsFromEnv
import no.ndla.common.Warmup
import no.ndla.frontpageapi.controller.HealthController
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.log4s.{Logger, getLogger}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class MainClass(props: FrontpageApiProperties) extends IOApp {
  val componentRegistry = new ComponentRegistry(props)
  val logger: Logger    = getLogger

  def warmup(): Unit = {
    def warmupRequest = (
        path: String,
        params: Map[String, String]
    ) => Warmup.warmupRequest(props.ApplicationPort, path, params)
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      // Since we don't really have a good way to check whether blaze is ready lets just wait a bit
      Thread.sleep(500)
      val warmupStart = System.currentTimeMillis()
      logger.info("Starting warmup procedure...")

      warmupRequest("/frontpage-api/v1/frontpage", Map.empty)
      warmupRequest("/frontpage-api/v1/subjectpage/1", Map.empty)
      warmupRequest("/health", Map.empty)

      HealthController.setWarmedUp()

      val warmupTime = System.currentTimeMillis() - warmupStart
      logger.info(s"Warmup procedure finished in ${warmupTime}ms.")
    }
  }

  override def run(args: List[String]): IO[ExitCode] = {
    setPropsFromEnv()

    logger.info(
      Source
        .fromInputStream(getClass.getResourceAsStream("/log-license.txt"))
        .mkString
    )

    logger.info("Starting database migration")
    componentRegistry.migrator.migrate()

    logger.info("Building swagger service")
    val routes = Routes.buildRoutes(componentRegistry, props)

    logger.info(s"Starting on port ${props.ApplicationPort}")
    val app = Router[IO](
      routes.map(r => r.mountPoint -> r.toRoutes): _*
    ).orNotFound

    val executorService  = Executors.newWorkStealingPool(props.NumThreads)
    val executionContext = ExecutionContext.fromExecutor(executorService)

    warmup()

    BlazeServerBuilder[IO](executionContext)
      .withHttpApp(app)
      .bindHttp(props.ApplicationPort, "0.0.0.0")
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

  }
}
