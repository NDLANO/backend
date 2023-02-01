/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import cats.implicits.toTraverseOps
import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.NdlaClient
import no.ndla.network.model.RequestInfo
import no.ndla.search.model.SearchableLanguageFormats
import no.ndla.searchapi.Props
import no.ndla.searchapi.caching.Memoize
import no.ndla.searchapi.model.api.TaxonomyException
import no.ndla.searchapi.model.search.SearchableTaxonomyContext
import no.ndla.searchapi.model.taxonomy._
import org.json4s.{DefaultFormats, Formats}
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
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
    private val TaxonomyApiEndpoint           = s"$TaxonomyUrl/v1"
    private val timeoutSeconds                = 600.seconds

    def getAllResources: Try[List[Resource]] =
      getPaginated[Resource](s"$TaxonomyApiEndpoint/resources/page", 500).map(_.distinct)

    def getAllSubjects: Try[List[TaxSubject]] =
      getPaginated[TaxSubject](s"$TaxonomyApiEndpoint/subjects/page", 500).map(_.distinct)

    def getAllTopics: Try[List[Topic]] =
      getPaginated[Topic](s"$TaxonomyApiEndpoint/topics/page", 500).map(_.distinct)

    def getAllResourceTypes: Try[List[ResourceType]] =
      get[List[ResourceType]](s"$TaxonomyApiEndpoint/resource-types/").map(_.distinct)

    def getAllTopicResourceConnections: Try[List[TopicResourceConnection]] =
      getPaginated[TopicResourceConnection](s"$TaxonomyApiEndpoint/topic-resources/page", 5000).map(_.distinct)

    def getAllTopicSubtopicConnections: Try[List[TopicSubtopicConnection]] =
      getPaginated[TopicSubtopicConnection](s"$TaxonomyApiEndpoint/topic-subtopics/page", 1000).map(_.distinct)

    def getAllResourceResourceTypeConnections: Try[List[ResourceResourceTypeConnection]] =
      get[List[ResourceResourceTypeConnection]](s"$TaxonomyApiEndpoint/resource-resourcetypes/").map(_.distinct)

    def getAllSubjectTopicConnections: Try[List[SubjectTopicConnection]] =
      getPaginated[SubjectTopicConnection](s"$TaxonomyApiEndpoint/subject-topics/page", 1000).map(_.distinct)

    def getAllRelevances: Try[List[Relevance]] =
      get[List[Relevance]](s"$TaxonomyApiEndpoint/relevances/").map(_.distinct)

    def getSearchableTaxonomy(contentUri: String, filterVisibles: Boolean): Try[List[SearchableTaxonomyContext]] = {
      implicit val formats = SearchableLanguageFormats.JSonFormatsWithMillis
      get[List[SearchableTaxonomyContext]](
        s"$TaxonomyApiEndpoint/queries/$contentUri",
        "filterVisibles" -> filterVisibles.toString
      )
    }

    val getTaxonomyBundle: Memoize[Try[TaxonomyBundle]] = Memoize(() => getTaxonomyBundleUncached)

    /** The memoized function of this [[getTaxonomyBundle]] should probably be used in most cases */
    private def getTaxonomyBundleUncached: Try[TaxonomyBundle] = {
      logger.info("Fetching taxonomy in bulk...")
      val startFetch                            = System.currentTimeMillis()
      implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(12))

      val requestInfo = RequestInfo()

      /** Calls function in separate thread and converts Try to Future */
      def tryToFuture[T](x: () => Try[T]) = Future { requestInfo.setRequestInfo(); x() }.flatMap(Future.fromTry)

      val relevances                      = tryToFuture(() => getAllRelevances)
      val resourceResourceTypeConnections = tryToFuture(() => getAllResourceResourceTypeConnections)
      val resourceTypes                   = tryToFuture(() => getAllResourceTypes)
      val resources                       = tryToFuture(() => getAllResources)
      val subjectTopicConnections         = tryToFuture(() => getAllSubjectTopicConnections)
      val subjects                        = tryToFuture(() => getAllSubjects)
      val topicResourceConnections        = tryToFuture(() => getAllTopicResourceConnections)
      val topicSubtopicConnections        = tryToFuture(() => getAllTopicSubtopicConnections)
      val topics                          = tryToFuture(() => getAllTopics)

      val x = for {
        f2  <- relevances
        f4  <- resourceResourceTypeConnections
        f5  <- resourceTypes
        f6  <- resources
        f7  <- subjectTopicConnections
        f8  <- subjects
        f10 <- topicResourceConnections
        f11 <- topicSubtopicConnections
        f13 <- topics
      } yield TaxonomyBundle(f2, f4, f5, f6, f7, f8, f10, f11, f13)

      Try(Await.result(x, Duration(300, "seconds"))) match {
        case Success(bundle) =>
          logger.info(s"Fetched taxonomy in ${System.currentTimeMillis() - startFetch}ms...")
          Success(bundle)
        case Failure(ex) =>
          logger.error(s"Could not fetch taxonomy bundle (${ex.getMessage})", ex)
          Failure(TaxonomyException("Could not fetch taxonomy bundle..."))
      }
    }

    private def get[A](url: String, params: (String, String)*)(implicit mf: Manifest[A], formats: Formats): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest.get(uri"$url?$params").readTimeout(timeoutSeconds)
      )
    }

    private def getPaginated[T](url: String, pageSize: Int)(implicit
        mf: Manifest[T]
    ): Try[List[T]] = {
      def fetchPage(page: Int, pageSize: Int = pageSize): Try[PaginationPage[T]] =
        get[PaginationPage[T]](s"$url", "page" -> page.toString, "pageSize" -> pageSize.toString)

      fetchPage(1, 1).flatMap(firstPage => {
        val numPages  = Math.ceil(firstPage.totalCount.toDouble / pageSize.toDouble).toInt
        val pageRange = 1 to numPages

        val numThreads = Math.max(20, numPages)
        implicit val executionContext: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numThreads))

        val pages        = pageRange.map(pageNum => Future(fetchPage(pageNum)))
        val mergedFuture = Future.sequence(pages)
        val awaited      = Await.result(mergedFuture, timeoutSeconds)

        awaited.toList.sequence.map(_.flatMap(_.results))
      })
    }
  }
}
case class PaginationPage[T](totalCount: Long, results: List[T])
