/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import com.typesafe.scalalogging.StrictLogging
import sttp.client3.quick._

import scala.concurrent.duration.DurationInt

trait Warmup {
  @volatile var isWarmedUp: Boolean = false
  def setWarmedUp(): Unit           = this.isWarmedUp = true
}

object Warmup extends StrictLogging {
  def warmupRequest(port: Int, path: String, params: Map[String, String]): Unit = {
    val startTime = System.currentTimeMillis()
    val url       = uri"http://localhost:$port?$params".withWholePath(path)

    val request = quickRequest
      .get(url)
      .readTimeout(15.seconds)
      .header("X-Correlation-ID", "WARMUP")

    val response = simpleHttpClient.send(request)
    val time     = System.currentTimeMillis() - startTime
    logger.info(s"Warming up with $url -> (${response.code}) and it took ${time}ms")
  }
}
