/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.scalatra

import com.typesafe.scalalogging.StrictLogging
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import no.ndla.common.Warmup
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
    afterHeaderBeforeStart: => Unit,
    warmupFunction: ((String, Map[String, String]) => Unit) => Unit
) extends Server(componentRegistry.props.ApplicationPort)
    with StrictLogging {

  val props: PropType           = componentRegistry.props
  private val startMillis: Long = System.currentTimeMillis()

  private def setupJavaMelody(context: ServletContextHandler): Unit = {
    context.addServlet(classOf[ReportServlet], "/monitoring")
    context.addEventListener(new SessionListener)
    val monitoringFilter = new FilterHolder(new MonitoringFilter())
    monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, props.ApplicationName)
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

  private def warmup(): Unit = {
    if (!props.disableWarmup) {
      val warmupStart = System.currentTimeMillis()
      logger.info("Starting warmup procedure...")
      warmupFunction((path, params) => Warmup.warmupRequest(props.ApplicationPort, path, params))
      val warmupTime = System.currentTimeMillis() - warmupStart
      logger.info(s"Warmup procedure finished in ${warmupTime}ms.")
      componentRegistry.healthController.setWarmedUp()
    }
  }

  logCopyrightHeader()

  afterHeaderBeforeStart

  this.setRequestLog(new NdlaRequestLogger())
  setupServletContext()
  this.start()

  this.warmup()

  private val startedTime: Long = System.currentTimeMillis() - startMillis
  logger.info(s"Started ${props.ApplicationName} server at port ${props.ApplicationPort} in ${startedTime}ms.")
}
