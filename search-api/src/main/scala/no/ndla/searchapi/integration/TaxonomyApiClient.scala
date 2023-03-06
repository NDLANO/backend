/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

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

    def getAllResources(shouldUsePublishedTax: Boolean): Try[List[Resource]] =
      get[List[Resource]](s"$TaxonomyApiEndpoint/resources/", headers = getVersionHashHeader(shouldUsePublishedTax))

    def getAllSubjects(shouldUsePublishedTax: Boolean): Try[List[TaxSubject]] =
      get[List[TaxSubject]](s"$TaxonomyApiEndpoint/subjects/", headers = getVersionHashHeader(shouldUsePublishedTax))

    def getAllTopics(shouldUsePublishedTax: Boolean): Try[List[Topic]] =
      get[List[Topic]](s"$TaxonomyApiEndpoint/topics/", headers = getVersionHashHeader(shouldUsePublishedTax))

    def getAllResourceTypes(shouldUsePublishedTax: Boolean): Try[List[ResourceType]] =
      get[List[ResourceType]](
        s"$TaxonomyApiEndpoint/resource-types/",
        headers = getVersionHashHeader(shouldUsePublishedTax)
      ).map(_.distinct)

    def getAllTopicResourceConnections(shouldUsePublishedTax: Boolean): Try[List[TopicResourceConnection]] =
      get[List[TopicResourceConnection]](
        s"$TaxonomyApiEndpoint/topic-resources/",
        headers = getVersionHashHeader(shouldUsePublishedTax)
      )

    def getAllTopicSubtopicConnections(shouldUsePublishedTax: Boolean): Try[List[TopicSubtopicConnection]] =
      get[List[TopicSubtopicConnection]](
        s"$TaxonomyApiEndpoint/topic-subtopics/",
        headers = getVersionHashHeader(shouldUsePublishedTax)
      )

    def getAllResourceResourceTypeConnections(
        shouldUsePublishedTax: Boolean
    ): Try[List[ResourceResourceTypeConnection]] =
      get[List[ResourceResourceTypeConnection]](
        s"$TaxonomyApiEndpoint/resource-resourcetypes/",
        headers = getVersionHashHeader(shouldUsePublishedTax)
      ).map(_.distinct)

    def getAllSubjectTopicConnections(shouldUsePublishedTax: Boolean): Try[List[SubjectTopicConnection]] =
      get[List[SubjectTopicConnection]](
        s"$TaxonomyApiEndpoint/subject-topics/",
        headers = getVersionHashHeader(shouldUsePublishedTax)
      )

    def getAllRelevances(shouldUsePublishedTax: Boolean): Try[List[Relevance]] =
      get[List[Relevance]](s"$TaxonomyApiEndpoint/relevances/", headers = getVersionHashHeader(shouldUsePublishedTax))
        .map(_.distinct)

    def getSearchableTaxonomy(
        contentUri: String,
        filterVisibles: Boolean,
        shouldUsePublishedTax: Boolean
    ): Try[List[SearchableTaxonomyContext]] = {
      implicit val formats = SearchableLanguageFormats.JSonFormatsWithMillis
      get[List[SearchableTaxonomyContext]](
        s"$TaxonomyApiEndpoint/queries/$contentUri",
        headers = getVersionHashHeader(shouldUsePublishedTax) + Map("filterVisibles" -> filterVisibles.toString)
      )
    }

    private def getVersionHashHeader(shouldUsePublishedTax: Boolean): Map[String, String] = {
      if (shouldUsePublishedTax) Map.empty else Map("versionHash" -> "default")
    }

    val getTaxonomyBundle: Memoize[Boolean, Try[TaxonomyBundle]] =
      new Memoize(1000 * 60, shouldUsePublishedTax => getTaxonomyBundleUncached(shouldUsePublishedTax))

    /** The memoized function of this [[getTaxonomyBundle]] should probably be used in most cases */
    private def getTaxonomyBundleUncached(shouldUsePublishedTax: Boolean): Try[TaxonomyBundle] = {
      logger.info("Fetching taxonomy in bulk...")
      val startFetch                            = System.currentTimeMillis()
      implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(12))

      val requestInfo = RequestInfo.fromThreadContext()

      /** Calls function in separate thread and converts Try to Future */
      def tryToFuture[T](x: Boolean => Try[T]) =
        Future { requestInfo.setRequestInfo(); x(shouldUsePublishedTax) }.flatMap(Future.fromTry)

      // format: off
      val relevances                      = tryToFuture(shouldUsePublishedTax => getAllRelevances(shouldUsePublishedTax))
      val resourceResourceTypeConnections = tryToFuture(shouldUsePublishedTax => getAllResourceResourceTypeConnections(shouldUsePublishedTax))
      val resourceTypes                   = tryToFuture(shouldUsePublishedTax => getAllResourceTypes(shouldUsePublishedTax))
      val resources                       = tryToFuture(shouldUsePublishedTax => getAllResources(shouldUsePublishedTax))
      val subjectTopicConnections         = tryToFuture(shouldUsePublishedTax => getAllSubjectTopicConnections(shouldUsePublishedTax))
      val subjects                        = tryToFuture(shouldUsePublishedTax => getAllSubjects(shouldUsePublishedTax))
      val topicResourceConnections        = tryToFuture(shouldUsePublishedTax => getAllTopicResourceConnections(shouldUsePublishedTax))
      val topicSubtopicConnections        = tryToFuture(shouldUsePublishedTax => getAllTopicSubtopicConnections(shouldUsePublishedTax))
      val topics                          = tryToFuture(shouldUsePublishedTax => getAllTopics(shouldUsePublishedTax))
      // format: on

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
