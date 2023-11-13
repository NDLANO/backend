/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import cats.implicits.toTraverseOps
import com.typesafe.scalalogging.StrictLogging
import enumeratum.Json4s
import no.ndla.network.NdlaClient
import no.ndla.network.TaxonomyData.{TAXONOMY_VERSION_HEADER, defaultVersion}
import no.ndla.search.model.SearchableLanguageFormats
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api.TaxonomyException
import no.ndla.searchapi.model.taxonomy._
import org.json4s.Formats
import org.json4s.native.Serialization
import sttp.client3.quick._

import java.io.{File, FileOutputStream}
import java.util.concurrent.Executors
import scala.concurrent._
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient with Props =>
  val taxonomyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends StrictLogging {
    import props.TaxonomyUrl

    implicit val formats: Formats   = SearchableLanguageFormats.JSonFormatsWithMillis + Json4s.serializer(NodeType)
    private val TaxonomyApiEndpoint = s"$TaxonomyUrl/v1"
    private val timeoutSeconds      = 600.seconds

    private def getResources(shouldUsePublishedTax: Boolean): Try[File] = {
      val tmpDir = java.nio.file.Files.createTempDirectory("taxonomy-bundle").toFile
      getPaginated(
        s"$TaxonomyApiEndpoint/nodes/search",
        headers = getVersionHashHeader(shouldUsePublishedTax),
        Seq(
          "pageSize" -> "500",
          "nodeType" -> List(NodeType.NODE, NodeType.SUBJECT, NodeType.TOPIC, NodeType.RESOURCE.toString).mkString(","),
          "includeContexts"  -> "true",
          "filterProgrammes" -> "true"
        ),
        shouldUsePublishedTax,
        tmpDir
      ).map(_ => tmpDir)
    }

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

    def getTaxonomyBundle(shouldUsePublishedTax: Boolean) = {
      logger.info(s"Fetching ${if (shouldUsePublishedTax) "published" else "draft"} taxonomy in bulk...")
      val startFetch = System.currentTimeMillis()

      getResources(shouldUsePublishedTax) match {
        case Success(tmpDir) =>
          logger.info(s"Fetched taxonomy in ${System.currentTimeMillis() - startFetch}ms...")
          val bundle = TaxonomyBundle(tmpDir.getAbsolutePath, shouldUsePublishedTax)
          Success(bundle)
        case Failure(ex) =>
          logger.error(s"Could not fetch taxonomy bundle (${ex.getMessage})", ex)
          Failure(TaxonomyException("Could not fetch taxonomy bundle..."))
      }
    }

    private def get[A](url: String, headers: Map[String, String], params: Seq[(String, String)])(implicit
        mf: Manifest[A],
        formats: Formats
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest.get(uri"$url?$params").headers(headers).readTimeout(timeoutSeconds),
        None
      )
    }

    private def getPaginated(
        url: String,
        headers: Map[String, String],
        params: Seq[(String, String)],
        published: Boolean,
        tmpDir: File
    ): Try[Unit] = {
      def fetchPage(p: Seq[(String, String)]): Try[PaginationPage] =
        get[PaginationPage](url, headers, p)

      val pageSize   = params.toMap.get("pageSize").get.toInt
      val pageParams = params :+ ("page" -> "1")
      fetchPage(pageParams).flatMap(firstPage => {
        val numPages  = Math.ceil(firstPage.totalCount.toDouble / pageSize.toDouble).toInt
        val pageRange = 1 to numPages

        val numThreads = Math.max(20, numPages)
        implicit val executionContext: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numThreads))

        val pages = pageRange.map(pageNum =>
          Future {
            fetchPage(params :+ ("page" -> s"$pageNum")).map(page =>
              page.results.foreach(n =>
                n.contentUri.foreach(uri => {
                  val pub         = if (published) "published" else "draft"
                  val path        = s"$tmpDir/${uri}_$pub.json"
                  val f           = new java.io.File(path)
                  lazy val stream = new FileOutputStream(f)

                  if (f.exists()) stream.write('\n')
                  val json = Serialization.write(n)
                  stream.write(json.getBytes)
                })
              )
            )
          }
        )
        val mergedFuture = Future.sequence(pages)
        val awaited      = Await.result(mergedFuture, timeoutSeconds)
        awaited.toList.sequence.map(_ => ())
      })
    }
  }
}
case class PaginationPage(totalCount: Long, results: List[Node])
