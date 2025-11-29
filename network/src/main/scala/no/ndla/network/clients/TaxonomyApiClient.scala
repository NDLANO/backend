/*
 * Part of NDLA network
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import cats.implicits.toTraverseOps
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.taxonomy.*
import no.ndla.network.NdlaClient
import no.ndla.network.TaxonomyData.{TAXONOMY_VERSION_HEADER, defaultVersion}
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.annotation.unused
import scala.concurrent.*
import scala.concurrent.duration.DurationInt
import scala.util.{Success, Try}

class TaxonomyApiClient(taxonomyBaseUrl: String)(using ndlaClient: NdlaClient) extends StrictLogging {
  private val TaxonomyApiEndpoint = s"$taxonomyBaseUrl/v1"
  private val timeoutSeconds      = 600.seconds

  def getNodesPage(page: Int, pageSize: Int, shouldUsePublishedTax: Boolean): Try[PaginationPage[Node]] =
    get[PaginationPage[Node]](
      s"$TaxonomyApiEndpoint/nodes/page",
      headers = getVersionHashHeader(shouldUsePublishedTax),
      Seq(
        "page"            -> page.toString,
        "pageSize"        -> pageSize.toString,
        "includeContexts" -> "true",
        "isVisible"       -> getIsVisibleParam(shouldUsePublishedTax),
      ),
    )

  def getTaxonomyBundleForContentUris(contentUris: Seq[String], shouldUsePublishedTax: Boolean): Try[TaxonomyBundle] = {
    if (contentUris.isEmpty) Success(TaxonomyBundle.empty)
    else {
      val pageSize = Math.max(500, contentUris.size)
      val nodes    = getPaginated[Node](
        s"$TaxonomyApiEndpoint/nodes/search",
        headers = getVersionHashHeader(shouldUsePublishedTax),
        Seq(
          "pageSize"         -> pageSize.toString,
          "nodeType"         -> NodeType.values.mkString(","),
          "filterProgrammes" -> "true",
          "includeContexts"  -> "true",
          "isVisible"        -> getIsVisibleParam(shouldUsePublishedTax),
          "contentUris"      -> contentUris.mkString(","),
        ),
      )
      nodes.map(TaxonomyBundle.fromNodeList)
    }
  }

  def getTaxonomyContext(
      contentUri: String,
      filterVisibles: Boolean,
      filterContexts: Boolean,
      shouldUsePublishedTax: Boolean,
  ): Try[List[TaxonomyContext]] = {
    val contexts = get[List[TaxonomyContext]](
      s"$TaxonomyApiEndpoint/queries/$contentUri",
      headers = getVersionHashHeader(shouldUsePublishedTax),
      params = Seq("filterVisibles" -> filterVisibles.toString),
    )
    if (filterContexts) contexts.map(list => list.filter(c => c.rootId.contains("subject")))
    else contexts
  }

  private def getIsVisibleParam(shouldUsePublishedTax: Boolean) = {
    if (shouldUsePublishedTax) ""
    else "false"
  }

  private def getVersionHashHeader(shouldUsePublishedTax: Boolean): Map[String, String] = {
    if (shouldUsePublishedTax) Map.empty
    else Map(TAXONOMY_VERSION_HEADER -> defaultVersion)
  }

  private def get[A: Decoder](url: String, headers: Map[String, String], params: Seq[(String, String)]): Try[A] = {
    ndlaClient.fetchWithForwardedAuth[A](
      quickRequest.get(uri"$url?$params").headers(headers).readTimeout(timeoutSeconds),
      None,
    )
  }

  private def getPaginated[T: Decoder](
      url: String,
      headers: Map[String, String],
      params: Seq[(String, String)],
  ): Try[List[T]] = {
    def fetchPage(p: Seq[(String, String)]): Try[PaginationPage[T]] = get[PaginationPage[T]](url, headers, p)

    val pageSize   = params.toMap.getOrElse("pageSize", "100").toInt
    val pageParams = params :+ ("page" -> "1")

    fetchPage(pageParams).flatMap(firstPage => {
      val numPages  = Math.ceil(firstPage.totalCount.toDouble / pageSize.toDouble).toInt
      val pageRange = 1 to numPages

      val numThreads                                                 = Math.min(8, Math.max(1, numPages))
      implicit val executionContext: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numThreads))

      try {
        val pages        = pageRange.map(pageNum => Future(fetchPage(params :+ ("page" -> s"$pageNum"))))
        val mergedFuture = Future.sequence(pages)
        val awaited      = Await.result(mergedFuture, timeoutSeconds)
        awaited.toList.sequence.map(_.flatMap(_.results))
      } finally {
        executionContext.shutdown()
      }
    })
  }
}

case class PaginationPage[T](totalCount: Long, results: List[T])
object PaginationPage {
  implicit def encoder[T](implicit
      @unused
      e: Encoder[T]
  ): Encoder[PaginationPage[T]] = deriveEncoder
  implicit def decoder[T](implicit
      @unused
      d: Decoder[T]
  ): Decoder[PaginationPage[T]] = deriveDecoder
}
