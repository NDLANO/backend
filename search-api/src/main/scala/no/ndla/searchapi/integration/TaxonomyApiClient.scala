/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import com.typesafe.scalalogging.StrictLogging
import enumeratum.Json4s
import no.ndla.network.NdlaClient
import no.ndla.network.TaxonomyData.{TAXONOMY_VERSION_HEADER, defaultVersion}
import no.ndla.network.model.RequestInfo
import no.ndla.search.model.SearchableLanguageFormats
import no.ndla.searchapi.Props
import no.ndla.searchapi.caching.Memoize
import no.ndla.searchapi.model.api.TaxonomyException
import no.ndla.searchapi.model.taxonomy._
import org.json4s.Formats
import sttp.client3.quick._

import java.util.concurrent.Executors
import scala.concurrent._
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient with Props =>
  val taxonomyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends StrictLogging {
    import props.TaxonomyUrl

    implicit val formats: Formats   = SearchableLanguageFormats.JSonFormatsWithMillis + Json4s.serializer(NodeType)
    private val TaxonomyApiEndpoint = s"$TaxonomyUrl/v1"
    private val timeoutSeconds      = 600.seconds
    private def getAllNodes(shouldUsePublishedTax: Boolean): Try[List[Node]] =
      get[List[Node]](
        s"$TaxonomyApiEndpoint/nodes/",
        headers = getVersionHashHeader(shouldUsePublishedTax),
        "nodeType"        -> NodeType.values.mkString(","),
        "includeContexts" -> "true"
      )

    def getTaxonomyContext(
        contentUri: String,
        filterVisibles: Boolean,
        filterContexts: Boolean,
        shouldUsePublishedTax: Boolean
    ): Try[List[TaxonomyContext]] = {
      val contexts = get[List[TaxonomyContext]](
        s"$TaxonomyApiEndpoint/queries/$contentUri",
        headers = getVersionHashHeader(shouldUsePublishedTax),
        params = "filterVisibles" -> filterVisibles.toString
      )
      if (filterContexts) contexts.map(list => list.filter(c => c.rootId.contains("subject"))) else contexts
    }

    private def getVersionHashHeader(shouldUsePublishedTax: Boolean): Map[String, String] = {
      if (shouldUsePublishedTax) Map.empty else Map(TAXONOMY_VERSION_HEADER -> defaultVersion)
    }

    val getTaxonomyBundle: Memoize[Boolean, Try[TaxonomyBundle]] =
      new Memoize(1000 * 60, shouldUsePublishedTax => getTaxonomyBundleUncached(shouldUsePublishedTax))

    /** The memoized function of this [[getTaxonomyBundle]] should probably be used in most cases */
    private def getTaxonomyBundleUncached(shouldUsePublishedTax: Boolean): Try[TaxonomyBundle] = {
      logger.info(s"Fetching ${if (shouldUsePublishedTax) "published" else "draft"} taxonomy in bulk...")
      val startFetch                            = System.currentTimeMillis()
      implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(12))

      val requestInfo = RequestInfo.fromThreadContext()

      /** Calls function in separate thread and converts Try to Future */
      def tryToFuture[T](x: Boolean => Try[T]) = Future {
        requestInfo.setRequestInfo(): Unit
        x(shouldUsePublishedTax)
      }.flatMap(Future.fromTry)

      val nodes = tryToFuture(shouldUsePublishedTax => getAllNodes(shouldUsePublishedTax))

      val x = for {
        n <- nodes
      } yield TaxonomyBundle(n)

      Try(Await.result(x, Duration(300, "seconds"))) match {
        case Success(bundle) =>
          logger.info(s"Fetched taxonomy in ${System.currentTimeMillis() - startFetch}ms...")
          Success(bundle)
        case Failure(ex) =>
          logger.error(s"Could not fetch taxonomy bundle (${ex.getMessage})", ex)
          Failure(TaxonomyException("Could not fetch taxonomy bundle..."))
      }
    }

    private def get[A](url: String, headers: Map[String, String], params: (String, String)*)(implicit
        mf: Manifest[A],
        formats: Formats
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest.get(uri"$url?$params").headers(headers).readTimeout(timeoutSeconds)
      )
    }
  }
}
