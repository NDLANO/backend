/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.integration

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.Title
import no.ndla.draftapi.Props
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.quick.*

import scala.util.Try

trait LearningpathApiClient {
  this: NdlaClient with ConverterService with Props =>
  lazy val learningpathApiClient: LearningpathApiClient

  class LearningpathApiClient {
    private val Endpoint = s"http://${props.LearningpathApiHost}/learningpath-api/v2/learningpaths"

    def getLearningpathsWithId(articleId: Long, user: TokenUser): Try[Seq[LearningPath]] = {
      get[Seq[LearningPath]](s"$Endpoint/contains-article/$articleId", user)
    }

    private def get[A: Decoder](endpointUrl: String, user: TokenUser, params: (String, String)*): Try[A] = {
      val request = quickRequest.get(uri"$endpointUrl".withParams(params: _*))
      ndlaClient.fetchWithForwardedAuth[A](request, Some(user))
    }

  }
}
case class LearningPath(id: Long, title: Title)

object LearningPath {
  implicit val encoder: Encoder[LearningPath] = deriveEncoder
  implicit val decoder: Decoder[LearningPath] = deriveDecoder
}
