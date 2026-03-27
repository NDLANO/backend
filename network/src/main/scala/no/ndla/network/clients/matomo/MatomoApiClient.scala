/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients.matomo

import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import no.ndla.network.NdlaClient
import sttp.client3.quick.*

import scala.concurrent.duration.*
import scala.util.Try

case class MatomoPageUrlResult(label: String, nb_hits: Long, nb_visits: Long, url: Option[String])

object MatomoPageUrlResult {
  implicit val decoder: Decoder[MatomoPageUrlResult] = deriveDecoder
}

class MatomoApiClient(using props: MatomoProps, client: NdlaClient) extends StrictLogging {
  private val timeout: FiniteDuration = 30.seconds

  def getTopPageUrlsForSubject(
      subjectSlug: String,
      period: String,
      date: String,
      limit: Int,
  ): Try[List[MatomoPageUrlResult]] = {
    val baseUrl = s"${props.MatomoUrl}/index.php"
    val params  = Map(
      "module"             -> "API",
      "method"             -> "Actions.getPageUrls",
      "idSite"             -> props.MatomoSiteId.toString,
      "period"             -> period,
      "date"               -> date,
      "format"             -> "JSON",
      "filter_limit"       -> limit.toString,
      "flat"               -> "1",
      "segment"            -> s"dimension13==$subjectSlug",
      "filter_sort_column" -> "nb_hits",
      "token_auth"         -> props.MatomoTokenAuth.toString,
    )

    val request = quickRequest.get(uri"$baseUrl?$params").readTimeout(timeout)

    logger.info(s"Fetching top page URLs from Matomo for subject '$subjectSlug' (period=$period, date=$date)")
    client.fetch[List[MatomoPageUrlResult]](request)
  }
}
