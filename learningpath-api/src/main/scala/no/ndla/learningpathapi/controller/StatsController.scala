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
import no.ndla.myndla.service.FolderReadService
import no.ndla.network.scalatra.NdlaSwaggerSupport
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{ResponseMessage, Swagger}
import org.scalatra.{NotFound, Ok}

trait StatsController {
  this: ReadService with NdlaController with Props with NdlaSwaggerSupport with FolderReadService =>
  val statsController: StatsController

  class StatsController(implicit val swagger: Swagger) extends NdlaController with NdlaSwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    protected val applicationDescription = "API for getting stats for my-ndla usage."

    // Additional models used in error responses
    registerModel[Error]()

    val response404: ResponseMessage = ResponseMessage(404, "Not found", Some("Error"))
    val response500: ResponseMessage = ResponseMessage(500, "Unknown error", Some("Error"))
    val response502: ResponseMessage = ResponseMessage(502, "Remote error", Some("Error"))

    get(
      "/",
      operation(
        apiOperation[Stats]("getStats")
          .summary("Get stats for my-ndla usage.")
          .description("Get stats for my-ndla usage.")
          .responseMessages(response404, response500, response502)
      )
    ) {
      folderReadService.getStats match {
        case Some(c) => Ok(c)
        case None    => NotFound("No stats found")
      }
    }: Unit
  }

}
