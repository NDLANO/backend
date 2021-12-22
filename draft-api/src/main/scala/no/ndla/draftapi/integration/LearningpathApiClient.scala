/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import no.ndla.draftapi.DraftApiProperties.LearningpathApiHost
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import scalaj.http.Http

import scala.util.Try

trait LearningpathApiClient {
  this: NdlaClient with ConverterService =>
  val learningpathApiClient: LearningpathApiClient

  class LearningpathApiClient {
    implicit val format = org.json4s.DefaultFormats
    private val InternalEndpoint = s"http://$LearningpathApiHost/intern"

    def getLearningpathsWithPaths(paths: Seq[String]): Try[Seq[LearningPath]] = {
      get[Seq[LearningPath]](s"$InternalEndpoint/containsArticle?paths=${paths.mkString(",")}")
    }

    private def get[A](endpointUrl: String, params: (String, String)*)(implicit mf: Manifest[A],
                                                                       format: org.json4s.Formats): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(endpointUrl).method("GET").params(params.toMap))
    }

  }
}
case class LearningPath(id: Option[Long])
