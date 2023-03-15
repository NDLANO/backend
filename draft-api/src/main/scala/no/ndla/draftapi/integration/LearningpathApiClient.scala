/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import no.ndla.draftapi.Props
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import sttp.client3.quick._

import scala.util.Try

trait LearningpathApiClient {
  this: NdlaClient with ConverterService with Props =>
  val learningpathApiClient: LearningpathApiClient

  class LearningpathApiClient {
    implicit val format  = org.json4s.DefaultFormats
    private val Endpoint = s"http://${props.LearningpathApiHost}/learningpath-api/v2/learningpaths"

    def getLearningpathsWithId(articleId: Long): Try[Seq[LearningPath]] = {
      get[Seq[LearningPath]](s"$Endpoint/contains-article/$articleId")
    }

    private def get[A](endpointUrl: String, params: (String, String)*)(implicit
        mf: Manifest[A],
        format: org.json4s.Formats
    ): Try[A] = {
      val request = quickRequest.get(uri"$endpointUrl".withParams(params: _*))
      ndlaClient.fetchWithForwardedAuth[A](request)
    }

  }
}
case class LearningPath(id: Long, title: Title)
