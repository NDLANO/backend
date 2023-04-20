/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import cats.effect.IO
import net.bull.javamelody.{MonitoringFilter, ReportServlet}
import org.http4s.HttpApp
import org.http4s.jetty.server.JettyBuilder
import org.log4s.{Logger, getLogger}

import javax.servlet.DispatcherType
import scala.concurrent.Future

case class TapirServer(name: String, serverPort: Int, app: HttpApp[IO], enableMelody: Boolean)(onReady: => Unit = {}) {
  val logger: Logger      = getLogger
  private var serverReady = false

  private def setupMelody(b: JettyBuilder[IO]): JettyBuilder[IO] = {
    val filt          = new MonitoringFilter()
    val dispatches    = java.util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)
    val reportServlet = new ReportServlet

    b
      .mountFilter(filt, "/*", Some(name), dispatches)
      .mountServlet(reportServlet, "/monitoring")
  }

  private val builder = JettyBuilder[IO]
    .mountHttpApp(app, "/")
    .bindHttp(serverPort, "0.0.0.0")

  private val withMelody = if (enableMelody) setupMelody(builder) else builder

  val server: IO[Nothing] = withMelody.resource
    .use(server => {
      IO {
        logger.info(s"$name server has started at ${server.address}")
        onReady
        serverReady = true
      }.flatMap(_ => IO.never)
    })

  def as[B](b: B): IO[B] = server.as[B](b)

  def toFuture: Future[Nothing] = {
    import cats.effect.unsafe.implicits.global
    server.unsafeToFuture()
  }

  def isReady: Boolean = this.serverReady
}
