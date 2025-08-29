/*
 * Part of NDLA network
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import io.circe.Decoder
import no.ndla.common.configuration.BaseProps
import no.ndla.common.model.domain.frontpage.SubjectPage
import no.ndla.network.NdlaClient
import sttp.client3.quick.*
import scala.concurrent.duration.*
import scala.util.Try

class FrontpageApiClient(using props: BaseProps, ndlaClient: NdlaClient) {
  val timeout: FiniteDuration = 15.seconds

  def getSubjectPage(id: Long): Try[SubjectPage] = {
    get[SubjectPage](s"${props.FrontpageApiUrl}/intern/dump/subjectpage/$id", Map.empty, Seq.empty)
  }

  private def get[A: Decoder](url: String, headers: Map[String, String], params: Seq[(String, String)]): Try[A] = {
    ndlaClient.fetchWithForwardedAuth[A](
      quickRequest.get(uri"$url?$params").headers(headers).readTimeout(timeout),
      None
    )
  }

}
