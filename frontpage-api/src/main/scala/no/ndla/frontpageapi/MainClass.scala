/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.{ExitCode, IO, IOApp}
import no.ndla.common.Environment.setPropsFromEnv
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.log4s.{Logger, getLogger}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.io.Source

class MainClass(props: FrontpageApiProperties) extends IOApp {
  val componentRegistry = new ComponentRegistry(props)
  val logger: Logger    = getLogger

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

    BlazeServerBuilder[IO](executionContext)
      .withHttpApp(app)
      .bindHttp(props.ApplicationPort, "0.0.0.0")
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

  }
}
