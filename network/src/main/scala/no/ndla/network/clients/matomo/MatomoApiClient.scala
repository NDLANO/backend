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
import no.ndla.network.NdlaClient
import no.ndla.network.clients.matomo.model.{MatomoDimensionResult, MatomoPageUrlResult, MatomoReportMetadata}
import sttp.client3.quick.*

import scala.concurrent.duration.*
import scala.util.{Failure, Success, Try}

class MatomoApiClient(using props: MatomoProps, client: NdlaClient) extends StrictLogging {
  private val timeout: FiniteDuration = 30.seconds
  private val baseUrl                 = uri"${props.MatomoUrl}/index.php"
  private val dimensionName           = "subjectId"

  def getDimensionIdForSubjectId: Try[String] = {
    val params = Map[String, String](
      "module"     -> "API",
      "method"     -> "API.getReportMetadata",
      "idSite"     -> props.MatomoSiteId,
      "format"     -> "JSON",
      "token_auth" -> props.MatomoTokenAuth,
    )
    val request = quickRequest.post(baseUrl).body(params).readTimeout(timeout)
    logger.info(s"Looking up Matomo dimension ID for dimension '$dimensionName'")
    client
      .fetch[List[MatomoReportMetadata]](request)
      .map { response =>
        for {
          foundMeta   <- response.find(_.name == dimensionName)
          parameters  <- foundMeta.parameters
          dimensionId <- parameters.idDimension
        } yield dimensionId
      } match {
      case Failure(ex)   => Failure(ex)
      case Success(None) =>
        Failure(new RuntimeException(s"No dimension with name '$dimensionName' found in Matomo report metadata"))
      case Success(Some(dimensionId)) => Success(dimensionId)
    }
  }

  def getSubtableIds(period: String, date: String, dimensionId: String): Try[Map[String, Long]] = {
    val params = Map[String, String](
      "module"       -> "API",
      "method"       -> "CustomDimensions.getCustomDimension",
      "idSite"       -> props.MatomoSiteId,
      "idDimension"  -> dimensionId,
      "period"       -> period,
      "date"         -> date,
      "format"       -> "JSON",
      "token_auth"   -> props.MatomoTokenAuth,
      "filter_limit" -> "-1",
    )

    val request = quickRequest.post(baseUrl).body(params).readTimeout(timeout)
    logger.info(s"Looking up Matomo subtable ID for subjects (period=$period, date=$date)")
    client
      .fetch[List[MatomoDimensionResult]](request)
      .map { results =>
        results.flatMap(dim => dim.idsubdatatable.map(subdataid => dim.label -> subdataid)).toMap
      }
  }

  def getTopPageUrlsForSubject(
      subjectId: String,
      period: String,
      date: String,
      limit: Int,
      subtableId: Long,
      dimensionId: String,
  ): Try[List[MatomoPageUrlResult]] = {
    for {
      results <- {
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
        logger.info(s"Fetching top pages from Matomo subtable $subtableId for subject '$subjectId'")
        client.fetch[List[MatomoPageUrlResult]](request)
      }
    } yield results
  }
}
