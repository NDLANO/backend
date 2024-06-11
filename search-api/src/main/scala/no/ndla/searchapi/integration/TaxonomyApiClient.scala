/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import cats.implicits.toTraverseOps
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.network.NdlaClient
import no.ndla.network.TaxonomyData.{TAXONOMY_VERSION_HEADER, defaultVersion}
import no.ndla.network.model.RequestInfo
import no.ndla.searchapi.Props
import no.ndla.searchapi.caching.Memoize
import no.ndla.searchapi.model.api.TaxonomyException
import no.ndla.searchapi.model.taxonomy.*
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.annotation.unused
import scala.collection.mutable.ListBuffer
import scala.concurrent.*
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient & Props =>
  val taxonomyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends StrictLogging {
    import props.TaxonomyUrl

    private val TaxonomyApiEndpoint = s"$TaxonomyUrl/v1"
    private val timeoutSeconds      = 600.seconds
    private def getNodes(shouldUsePublishedTax: Boolean): Try[ListBuffer[Node]] =
      get[ListBuffer[Node]](
        s"$TaxonomyApiEndpoint/nodes/",
        headers = getVersionHashHeader(shouldUsePublishedTax),
        Seq(
          "nodeType"        -> List(NodeType.NODE, NodeType.SUBJECT, NodeType.TOPIC).mkString(","),
          "includeContexts" -> "true"
        )
      )

    private def getResources(shouldUsePublishedTax: Boolean): Try[List[Node]] =
      getPaginated[Node](
        s"$TaxonomyApiEndpoint/nodes/search",
        headers = getVersionHashHeader(shouldUsePublishedTax),
        Seq(
          "pageSize"         -> "500",
          "nodeType"         -> NodeType.RESOURCE.toString,
          "includeContexts"  -> "true",
          "filterProgrammes" -> "true"
        )
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
        params = Seq("filterVisibles" -> filterVisibles.toString)
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
      implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

      val requestInfo = RequestInfo.fromThreadContext()

      /** Calls function in separate thread and converts Try to Future */
      def tryToFuture[T](x: Boolean => Try[T]) = Future {
        requestInfo.setRequestInfo(): Unit
        x(shouldUsePublishedTax)
      }.flatMap(Future.fromTry)

      val nodes     = tryToFuture(shouldUsePublishedTax => getNodes(shouldUsePublishedTax))
      val resources = tryToFuture(shouldUsePublishedTax => getResources(shouldUsePublishedTax))

      val x = for {
        n <- nodes
        r <- resources
      } yield TaxonomyBundle(n.addAll(r).result())

      Try(Await.result(x, Duration(300, "seconds"))) match {
        case Success(bundle) =>
          logger.info(s"Fetched taxonomy in ${System.currentTimeMillis() - startFetch}ms...")
          Success(bundle)
        case Failure(ex) =>
          logger.error(s"Could not fetch taxonomy bundle (${ex.getMessage})", ex)
          Failure(TaxonomyException("Could not fetch taxonomy bundle..."))
      }
    }

    private def get[A: Decoder](url: String, headers: Map[String, String], params: Seq[(String, String)]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest.get(uri"$url?$params").headers(headers).readTimeout(timeoutSeconds),
        None
      )
    }

    private def getPaginated[T: Decoder](
        url: String,
        headers: Map[String, String],
        params: Seq[(String, String)]
    ): Try[List[T]] = {
      def fetchPage(p: Seq[(String, String)]): Try[PaginationPage[T]] =
        get[PaginationPage[T]](url, headers, p)

      val pageSize   = params.toMap.get("pageSize").get.toInt
      val pageParams = params :+ ("page" -> "1")
      fetchPage(pageParams).flatMap(firstPage => {
        val numPages  = Math.ceil(firstPage.totalCount.toDouble / pageSize.toDouble).toInt
        val pageRange = 1 to numPages

        val numThreads = Math.max(20, numPages)
        implicit val executionContext: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numThreads))

        val pages        = pageRange.map(pageNum => Future(fetchPage(params :+ ("page" -> s"$pageNum"))))
        val mergedFuture = Future.sequence(pages)
        val awaited      = Await.result(mergedFuture, timeoutSeconds)

        awaited.toList.sequence.map(_.flatMap(_.results))
      })
    }
  }
}
case class PaginationPage[T](totalCount: Long, results: List[T])
object PaginationPage {
  implicit def encoder[T](implicit @unused e: Encoder[T]): Encoder[PaginationPage[T]] = deriveEncoder
  implicit def decoder[T](implicit @unused d: Decoder[T]): Decoder[PaginationPage[T]] = deriveDecoder
}
