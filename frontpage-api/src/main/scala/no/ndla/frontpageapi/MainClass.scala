/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port}
import no.ndla.common.Environment.setPropsFromEnv
import no.ndla.common.Warmup
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.{Request, Response}
import org.log4s.{Logger, getLogger}

import scala.concurrent.Future
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

      componentRegistry.healthController.setWarmedUp()

      val warmupTime = System.currentTimeMillis() - warmupStart
      logger.info(s"Warmup procedure finished in ${warmupTime}ms.")
    }
  }

  private def buildRouter(): Kleisli[IO, Request[IO], Response[IO]] = {
    logger.info("Building swagger service")
    val router = Router[IO](componentRegistry.routes: _*)
    Kleisli[IO, Request[IO], Response[IO]](a => {
      router.run(a).getOrElse(componentRegistry.getFallbackRoute)
    })
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

    val app = buildRouter()

    logger.info(s"Starting on port ${props.ApplicationPort}")
    val server = EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("0.0.0.0").get)
      .withPort(Port.fromInt(props.ApplicationPort).get)
      .withHttpApp(app)
      .build
      .use(server => {
        IO {
          logger.info(s"Server has started at ${server.address}")
          warmup()
        }.flatMap(_ => IO.never)
      })

    server.as(ExitCode.Success)
  }
}
