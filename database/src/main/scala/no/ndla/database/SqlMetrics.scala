/*
 * Part of NDLA database
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import io.prometheus.metrics.core.metrics.{Counter, Histogram}
import no.ndla.network.tapir.NdlaPrometheusRegistry
import scalikejdbc.GlobalSettings

object SqlMetrics {
  private val whitespace = "\\s+".r

  private def normalize(stmt: String): String = whitespace.replaceAllIn(stmt, " ").trim

  private val queriesTotal: Counter = Counter
    .builder()
    .name("ndla_sql_queries_total")
    .help("Successful SQL queries executed, by prepared statement.")
    .labelNames("statement")
    .register(NdlaPrometheusRegistry.registry)

  private val queryFailuresTotal: Counter = Counter
    .builder()
    .name("ndla_sql_query_failures_total")
    .help("Failed SQL queries, by prepared statement and exception class.")
    .labelNames("statement", "exception")
    .register(NdlaPrometheusRegistry.registry)

  private val queryDurationSeconds: Histogram = Histogram
    .builder()
    .name("ndla_sql_query_duration_seconds")
    .help("SQL query duration in seconds, by prepared statement (successful queries only).")
    .labelNames("statement")
    .classicUpperBounds(0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0)
    .register(NdlaPrometheusRegistry.registry)

  @volatile
  private var installed = false

  def install(): Unit = synchronized {
    if (installed) return
    GlobalSettings.taggedQueryCompletionListener = (stmt, _, millis, _) => {
      val s = normalize(stmt)
      queriesTotal.labelValues(s).inc()
      queryDurationSeconds.labelValues(s).observe(millis / 1000.0)
    }
    GlobalSettings.queryFailureListener = (stmt, _, t) => {
      queryFailuresTotal.labelValues(normalize(stmt), t.getClass.getSimpleName).inc()
    }
    installed = true
  }
}
