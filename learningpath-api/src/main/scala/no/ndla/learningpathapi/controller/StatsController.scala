/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.api.Error
import no.ndla.learningpathapi.service.ReadService
import no.ndla.myndla.model.api.Stats
import no.ndla.network.scalatra.NdlaSwaggerSupport
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{ResponseMessage, Swagger}
import org.scalatra.MovedPermanently

trait StatsController {
  this: ReadService with NdlaController with Props with NdlaSwaggerSupport =>
  val statsController: StatsController

  class StatsController(implicit val swagger: Swagger) extends NdlaController with NdlaSwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    protected val applicationDescription = "API for getting stats for my-ndla usage."

    // Additional models used in error responses
    registerModel[Error]()

    private val response301: ResponseMessage = ResponseMessage(301, "Moved permanently", Some("Error"))

    get(
      "/",
      operation(
        apiOperation[Stats]("getStats")
          .summary("Get stats for my-ndla usage.")
          .description("Get stats for my-ndla usage.")
          .responseMessages(response301)
          .deprecated(true)
      )
    ) {
      MovedPermanently("/myndla-api/v1/stats")
    }: Unit
  }

}
