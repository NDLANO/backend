package no.ndla.network.clients

import io.circe.Decoder
import no.ndla.common.configuration.HasBaseProps
import no.ndla.common.model.domain.frontpage.SubjectPage
import no.ndla.network.NdlaClient
import sttp.client3.quick.*
import scala.concurrent.duration.*
import scala.util.Try

trait FrontpageApiClient {
  this: HasBaseProps & NdlaClient =>
  val frontpageApiClient: FrontpageApiClient

  class FrontpageApiClient {
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
}
