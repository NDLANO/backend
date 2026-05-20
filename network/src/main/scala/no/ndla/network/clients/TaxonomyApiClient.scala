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
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.taxonomy.*
import no.ndla.network.NdlaClient
import no.ndla.network.TaxonomyData.{TAXONOMY_VERSION_HEADER, defaultVersion}
import sttp.client4.quick.*

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
      val body     = NodeSearchBody(
        pageSize = pageSize,
        page = 1,
        contentUris = contentUris.toList,
        nodeType = NodeType.values.toList,
        includeContexts = true,
      )
      postPaginated[Node](
        s"$TaxonomyApiEndpoint/nodes/search",
        headers = getVersionHashHeader(shouldUsePublishedTax),
        body = body,
      ).map(TaxonomyBundle.fromNodeList)
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

  private def postPaginated[T: Decoder](
      url: String,
      headers: Map[String, String],
      body: NodeSearchBody,
  ): Try[List[T]] = {
    def fetchPage(page: Int): Try[PaginationPage[T]] = {
      val pageBody = body.copy(page = page).asJson.noSpaces
      ndlaClient.fetchWithForwardedAuth[PaginationPage[T]](
        quickRequest
          .post(uri"$url")
          .body(pageBody)
          .header("Content-Type", "application/json")
          .headers(headers)
          .readTimeout(timeoutSeconds),
        None,
      )
    }

    fetchPage(1).flatMap { firstPage =>
      val numPages = Math.max(1, Math.ceil(firstPage.totalCount.toDouble / body.pageSize.toDouble).toInt)
      if (numPages == 1) Success(firstPage.results)
      else {
        val numThreads                                                 = Math.min(8, numPages - 1)
        implicit val executionContext: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numThreads))
        try {
          val tailPages = (
            2 to numPages
          ).map(p => Future(fetchPage(p)))
          val mergedFuture = Future.sequence(tailPages)
          val awaited      = Await.result(mergedFuture, timeoutSeconds)
          awaited.toList.sequence.map(rest => firstPage.results ++ rest.flatMap(_.results))
        } finally {
          executionContext.shutdown()
        }
      }
    }
  }
}

case class NodeSearchBody(
    pageSize: Int,
    page: Int,
    contentUris: List[String],
    nodeType: List[NodeType],
    includeContexts: Boolean,
)
object NodeSearchBody {
  implicit val encoder: Encoder[NodeSearchBody] = deriveEncoder
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
