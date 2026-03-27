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
import scala.util.{Failure, Try}

case class MatomoDimensionResult(label: String, nb_hits: Long, nb_visits: Long, idsubdatatable: Option[Long])

object MatomoDimensionResult {
  implicit val decoder: Decoder[MatomoDimensionResult] = deriveDecoder
}

case class MatomoPageUrlResult(label: String, nb_hits: Long, nb_visits: Long)

object MatomoPageUrlResult {
  implicit val decoder: Decoder[MatomoPageUrlResult] = deriveDecoder
}

class MatomoApiClient(using props: MatomoProps, client: NdlaClient) extends StrictLogging {
  private val timeout: FiniteDuration = 30.seconds
  private val baseUrl                 = uri"${props.MatomoUrl}/index.php"

  private val dimensionId = "13"

  private def getSubtableIdForSubject(
      subjectSlug: String,
      period: String,
      date: String
  ): Try[Long] = {
    val params = Map[String, String](
      "module"         -> "API",
      "method"         -> "CustomDimensions.getCustomDimension",
      "idSite"         -> props.MatomoSiteId,
      "idDimension"    -> dimensionId,
      "period"         -> period,
      "date"           -> date,
      "format"         -> "JSON",
      "filter_limit"   -> "1",
      "filter_pattern" -> subjectSlug,
      "token_auth"     -> props.MatomoTokenAuth,
    )

    val request = quickRequest.post(baseUrl).body(params).readTimeout(timeout)
    logger.info(s"Looking up Matomo subtable ID for subject '$subjectSlug' (period=$period, date=$date)")
    client.fetch[List[MatomoDimensionResult]](request).flatMap { results =>
      results
        .find(_.label == subjectSlug)
        .flatMap(_.idsubdatatable)
        .map(scala.util.Success(_))
        .getOrElse(Failure(new RuntimeException(s"No Matomo data found for subject '$subjectSlug'")))
    }
  }

  def getTopPageUrlsForSubject(
      subjectSlug: String,
      period: String,
      date: String,
      limit: Int,
  ): Try[List[MatomoPageUrlResult]] = {
    for {
      subtableId <- getSubtableIdForSubject(subjectSlug, period, date)
      results    <- {
        val params = Map[String, String](
          "module"             -> "API",
          "method"             -> "CustomDimensions.getCustomDimension",
          "idSite"             -> props.MatomoSiteId,
          "idDimension"        -> dimensionId,
          "period"             -> period,
          "date"               -> date,
          "format"             -> "JSON",
          "idSubtable"         -> subtableId.toString,
          "filter_limit"       -> limit.toString,
          "filter_sort_column" -> "nb_hits",
          "token_auth"         -> props.MatomoTokenAuth,
        )

        val request = quickRequest.post(baseUrl).body(params).readTimeout(timeout)
        logger.info(s"Fetching top pages from Matomo subtable $subtableId for subject '$subjectSlug'")
        client.fetch[List[MatomoPageUrlResult]](request)
      }
    } yield results
  }
}
