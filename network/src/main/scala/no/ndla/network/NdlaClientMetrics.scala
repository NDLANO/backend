/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import io.prometheus.metrics.core.metrics.{Counter, Histogram}
import no.ndla.network.tapir.NdlaPrometheusRegistry

object NdlaClientMetrics {
  val SlowRequestThresholdMs: Double = 3000.0

  private val requestsTotal: Counter = Counter
    .builder()
    .name("ndla_http_client_requests_total")
    .help("Outgoing HTTP client requests that got a response, by target host, method and response status.")
    .labelNames("host", "method", "status")
    .register(NdlaPrometheusRegistry.registry)

  private val requestDurationSeconds: Histogram = Histogram
    .builder()
    .name("ndla_http_client_request_duration_seconds")
    .help("Outgoing HTTP client request duration in seconds, by target host and method.")
    .labelNames("host", "method")
    .classicUpperBounds(0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0)
    .register(NdlaPrometheusRegistry.registry)

  private val failuresTotal: Counter = Counter
    .builder()
    .name("ndla_http_client_failures_total")
    .help("Outgoing HTTP client requests that failed before a response, by host, method and exception class.")
    .labelNames("host", "method", "exception")
    .register(NdlaPrometheusRegistry.registry)

  def observe(host: String, method: String, status: Int, millis: Double): Unit = {
    requestsTotal.labelValues(host, method, status.toString).inc()
    requestDurationSeconds.labelValues(host, method).observe(millis / 1000.0)
  }

  def observeFailure(host: String, method: String, t: Throwable): Unit = {
    failuresTotal.labelValues(host, method, t.getClass.getSimpleName).inc()
  }
}
