/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, NativeSwaggerBase, Swagger}

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

object AudioApiInfo {
  val apiInfo = ApiInfo(
    "Audio Api",
    "Documentation for the Audio API of NDLA.no",
    "http://ndla.no",
    AudioApiProperties.ContactEmail,
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class AudioSwagger extends Swagger(Swagger.SpecVersion, "0.8", AudioApiInfo.apiInfo)
