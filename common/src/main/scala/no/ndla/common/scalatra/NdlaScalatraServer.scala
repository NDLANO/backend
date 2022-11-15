/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.scalatra

import com.typesafe.scalalogging.LazyLogging
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import no.ndla.common.configuration.{BaseComponentRegistry, BaseProps}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import java.util
import javax.servlet.DispatcherType
import scala.io.Source

class NdlaScalatraServer[PropType <: BaseProps, CR <: BaseComponentRegistry[PropType]](
    bootstrapPackage: String,
    componentRegistry: CR,
    afterHeaderBeforeStart: => Unit = {}
) extends Server(componentRegistry.props.ApplicationPort)
    with LazyLogging {
  val props: PropType           = componentRegistry.props
  private val startMillis: Long = System.currentTimeMillis()
  private def setupJavaMelody(context: ServletContextHandler): Unit = {
    context.addServlet(classOf[ReportServlet], "/monitoring")
    context.addEventListener(new SessionListener)
    val monitoringFilter = new FilterHolder(new MonitoringFilter())
    monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, props.ApplicationName)
    props.Environment match {
      case "local" =>
      case _ =>
        monitoringFilter.setInitParameter(
          Parameter.CLOUDWATCH_NAMESPACE.getCode,
          "NDLA/APP".replace("APP", props.ApplicationName)
        )
    }
    context.addFilter(monitoringFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC))
  }
  private def setupServletContext(): Unit = {
    val context = new ServletContextHandler()
    context setContextPath "/"
    context.setInitParameter("org.scalatra.LifeCycle", bootstrapPackage)
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    setupJavaMelody(context)

    // Necessary to mount ComponentRegistry members in ScalatraBootstrap
    context.setAttribute("ComponentRegistry", componentRegistry)
    this.setHandler(context)
  }
  private def logCopyrightHeader(): Unit = {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)
  }

  logCopyrightHeader()

  afterHeaderBeforeStart

  this.setRequestLog(new NdlaRequestLogger(props))
  setupServletContext()
  this.start()

  private val startedTime: Long = System.currentTimeMillis() - startMillis
  logger.info(s"Started ${props.ApplicationName} server at port ${props.ApplicationPort} in ${startedTime}ms.")
}
