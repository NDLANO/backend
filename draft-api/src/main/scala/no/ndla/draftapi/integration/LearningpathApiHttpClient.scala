/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.integration

import io.circe.Decoder
import no.ndla.draftapi.Props
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.auth.TokenUser
import sttp.client4.quick.*

import scala.util.Try

class LearningpathApiHttpClient(using ndlaClient: NdlaClient, props: Props) extends LearningpathApiClient {
  private val Endpoint = s"http://${props.LearningpathApiHost}/learningpath-api/v2/learningpaths"

  def getLearningpathsWithId(articleId: Long, user: TokenUser): Try[Seq[LearningPath]] = {
    get[Seq[LearningPath]](s"$Endpoint/contains-article/$articleId", user)
  }

  private def get[A: Decoder](endpointUrl: String, user: TokenUser, params: (String, String)*): Try[A] = {
    val request = quickRequest.get(uri"$endpointUrl".withParams(params*))
    ndlaClient.fetchWithForwardedAuth[A](request, Some(user))
  }

}
