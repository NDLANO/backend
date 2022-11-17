package no.ndla.common

import com.typesafe.scalalogging.StrictLogging
import scalaj.http.{Http, HttpResponse}

trait Warmup {
  var isWarmedUp: Boolean = false
  def setWarmedUp(): Unit = this.isWarmedUp = true
}

object Warmup extends StrictLogging {
  def warmupRequest(port: Int, path: String, params: Map[String, String]): Unit = {
    val startTime = System.currentTimeMillis()
    val url       = s"http://localhost:$port$path"
    val response = Http(url)
      .params(params)
      .header("X-Correlation-ID", "WARMUP")
      .timeout(15000, 15000)
      .asString

    response match {
      case HttpResponse(_, code, _) =>
        val time = System.currentTimeMillis() - startTime
        logger.info(s"Warming up with $url -> ($code) and it took ${time}ms")
    }
  }
}
