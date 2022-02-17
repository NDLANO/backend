/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.{ExitCode, IO, IOApp}
import no.ndla.common.Environment.setPropsFromEnv
import no.ndla.frontpageapi.FrontpageApiProperties.{ApplicationPort, NumThreads}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.log4s.getLogger

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.io.Source

object Main extends IOApp {
  val logger = getLogger

  override def run(args: List[String]): IO[ExitCode] = {
    setPropsFromEnv()

    logger.info(
      Source
        .fromInputStream(getClass.getResourceAsStream("/log-license.txt"))
        .mkString
    )

    logger.info("Starting database migration")
    DBMigrator.migrate(ComponentRegistry.dataSource)

    logger.info("Building swagger service")
    val routes = Routes.buildRoutes()

    logger.info(s"Starting on port $ApplicationPort")
    val app = Router[IO](
      routes.map(r => r.mountPoint -> r.toRoutes): _*
    ).orNotFound

    val executorService  = Executors.newWorkStealingPool(NumThreads)
    val executionContext = ExecutionContext.fromExecutor(executorService)

    BlazeServerBuilder[IO](executionContext)
      .withHttpApp(app)
      .bindHttp(ApplicationPort, "0.0.0.0")
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

  }
}
