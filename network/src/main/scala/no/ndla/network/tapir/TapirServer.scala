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

case class TapirServer(name: String, serverPort: Int, app: HttpApp[IO], enableMelody: Boolean)(onReady: => Unit = {}) {
  val logger: Logger      = getLogger
  private var serverReady = false

  implicit class BuilderExtensionMethod(jettyBuilder: JettyBuilder[IO]) {
    def setupMelody(enable: Boolean): JettyBuilder[IO] = {
      if (enable) {
        val monitoringFilter = new MonitoringFilter()
        val dispatches       = java.util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)
        val reportServlet    = new ReportServlet

        jettyBuilder
          .mountFilter(monitoringFilter, "/*", Some(name), dispatches)
          .mountServlet(reportServlet, "/monitoring")
      } else {
        jettyBuilder
      }
    }
  }

  private val builder = JettyBuilder[IO]
    .mountHttpApp(app, "/")
    .bindHttp(serverPort, "0.0.0.0")
    .setupMelody(enableMelody)

  val server: IO[Nothing] = builder.resource
    .use(server => {
      IO {
        logger.info(s"$name server has started at ${server.address}")
        onReady
        serverReady = true
      }.flatMap(_ => IO.never)
    })

  def as[B](b: B): IO[B] = server.as[B](b)

  def runInBackground(): Unit = {
    import cats.effect.unsafe.implicits.global
    server.unsafeRunAndForget()
  }

  def isReady: Boolean = this.serverReady
}
