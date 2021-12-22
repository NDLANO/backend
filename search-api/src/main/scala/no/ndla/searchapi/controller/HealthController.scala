/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.RequestSuccess
import com.typesafe.scalalogging.LazyLogging
import no.ndla.searchapi.integration.Elastic4sClient
import org.scalatra.{InternalServerError, Ok, ScalatraServlet}

import scala.util.{Failure, Success, Try}

trait HealthController {
  this: Elastic4sClient =>
  val healthController: HealthController

  class HealthController extends ScalatraServlet with LazyLogging {
    get("/") {
      e4sClient.execute(clusterHealth()) match {
        case Failure(exception) =>
          logger.error("Something went wrong when contacting elasticsearch instance when performing health check",
                       exception)
          InternalServerError()
        case Success(successfulResponse) if successfulResponse.status == 200 && successfulResponse.isSuccess =>
          Ok()
        case Success(notReallySuccessfulResponse) =>
          logger.error(
            s"""Health check against elasticsearch failed --->
                 |Status: $status
                 |Body: ${notReallySuccessfulResponse.body.getOrElse("<missing>")}
                 |headers:   ${notReallySuccessfulResponse.headers.mkString("\n\t")}
                 |Result: ${notReallySuccessfulResponse.result.toString}
                 |""".stripMargin
          )
          InternalServerError()
      }
    }

  }
}
