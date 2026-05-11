/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import com.typesafe.scalalogging.StrictLogging
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.ndla.common.TryUtil
import sttp.shared.Identity
import sttp.tapir.server.metrics.MetricLabels
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics

object NdlaPrometheusRegistry extends StrictLogging {
  val registry: PrometheusRegistry = new PrometheusRegistry()

  private val tapirClosedErrorMessage    = "Client disconnected, request timed out, or request cancelled"
  private val metricLabels: MetricLabels = MetricLabels(
    forRequest = List("path" -> (req => req.pathSegments.mkString("/")), "method" -> (req => req.method.method)),
    forEndpoint = List.empty,
    forResponse = List(
      "status" -> {
        case Right(r)                                                               => Some(r.code.code.toString)
        case Left(ex: RuntimeException) if ex.getMessage == tapirClosedErrorMessage =>
          logger.info("Mapping closed request exception to status code 499 for metrics")
          Some("499")
        case Left(ex) if TryUtil.containsInterruptedException(ex) => Some("499")
        case Left(_)                                              => Some("5xx")
      }
    ),
  )

  lazy val tapirPrometheusMetrics: PrometheusMetrics[Identity] =
    PrometheusMetrics.default[Identity](namespace = "tapir", registry = registry, labels = metricLabels)
}
