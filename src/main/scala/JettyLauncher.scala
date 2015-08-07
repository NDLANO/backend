import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.{HandlerList, ResourceHandler}
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object JettyLauncher { // this is my entry object as specified in sbt project definition
  def main(args: Array[String]) {
    val port = if(System.getenv("PORT") != null) System.getenv("PORT").toInt else 80

    val server = new Server(port)
    val context = new WebAppContext()
    context setContextPath "/"
    context.setResourceBase(getClass.getResource("image-api").toExternalForm)
    context.setWelcomeFiles(Array("index.html"))
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    val resource_handler = new ResourceHandler()
    resource_handler.setWelcomeFiles(Array("index.html"))
    resource_handler.setResourceBase(getClass.getResource("META-INF/resources/webjars").toExternalForm)

    val handlers = new HandlerList()
    handlers.setHandlers(Array(resource_handler, context))

    server.setHandler(handlers)

    server.start
    server.join
  }
}
