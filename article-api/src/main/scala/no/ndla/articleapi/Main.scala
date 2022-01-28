package no.ndla.articleapi

import no.ndla.articleapi.JettyLauncher.startServer
import no.ndla.common.Environment.setPropsFromEnv

object Main {

  def main(args: Array[String]): Unit = {
    setPropsFromEnv()
    val server = startServer()
    server.join()
  }
}
