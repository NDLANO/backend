/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import java.util.concurrent.Executors

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.NdlaClient
import no.ndla.searchapi.SearchApiProperties.ApiGatewayUrl
import no.ndla.searchapi.caching.Memoize
import no.ndla.searchapi.model.api.TaxonomyException
import no.ndla.searchapi.model.domain.RequestInfo
import no.ndla.searchapi.model.taxonomy._
import org.json4s.DefaultFormats
import scalaj.http.Http

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient =>
  val taxonomyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends LazyLogging {
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
    private val TaxonomyApiEndpoint = s"$ApiGatewayUrl/taxonomy/v1"

    def getAllResources: Try[List[Resource]] =
      get[List[Resource]](s"$TaxonomyApiEndpoint/resources/").map(_.distinct)

    def getAllSubjects: Try[List[TaxSubject]] =
      get[List[TaxSubject]](s"$TaxonomyApiEndpoint/subjects/").map(_.distinct)

    def getAllTopics: Try[List[Topic]] =
      get[List[Topic]](s"$TaxonomyApiEndpoint/topics/").map(_.distinct)

    def getAllResourceTypes: Try[List[ResourceType]] =
      get[List[ResourceType]](s"$TaxonomyApiEndpoint/resource-types/").map(_.distinct)

    def getAllTopicResourceConnections: Try[List[TopicResourceConnection]] =
      get[List[TopicResourceConnection]](s"$TaxonomyApiEndpoint/topic-resources/").map(_.distinct)

    def getAllTopicSubtopicConnections: Try[List[TopicSubtopicConnection]] =
      get[List[TopicSubtopicConnection]](s"$TaxonomyApiEndpoint/topic-subtopics/").map(_.distinct)

    def getAllResourceResourceTypeConnections: Try[List[ResourceResourceTypeConnection]] =
      get[List[ResourceResourceTypeConnection]](s"$TaxonomyApiEndpoint/resource-resourcetypes/").map(_.distinct)

    def getAllSubjectTopicConnections: Try[List[SubjectTopicConnection]] =
      get[List[SubjectTopicConnection]](s"$TaxonomyApiEndpoint/subject-topics/").map(_.distinct)

    def getAllRelevances: Try[List[Relevance]] =
      get[List[Relevance]](s"$TaxonomyApiEndpoint/relevances/").map(_.distinct)

    val getTaxonomyBundle: Memoize[Try[TaxonomyBundle]] = Memoize(() => getTaxonomyBundleUncached)

    /** The memoized function of this [[getTaxonomyBundle]] should probably be used in most cases */
    private def getTaxonomyBundleUncached: Try[TaxonomyBundle] = {
      logger.info("Fetching taxonomy in bulk...")
      val startFetch = System.currentTimeMillis()
      implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(12))

      val requestInfo = RequestInfo()

      /** Calls function in separate thread and converts Try to Future */
      def tryToFuture[T](x: () => Try[T]) = Future { requestInfo.setRequestInfo(); x() }.flatMap(Future.fromTry)

      val relevances = tryToFuture(() => getAllRelevances)
      val resourceResourceTypeConnections = tryToFuture(() => getAllResourceResourceTypeConnections)
      val resourceTypes = tryToFuture(() => getAllResourceTypes)
      val resources = tryToFuture(() => getAllResources)
      val subjectTopicConnections = tryToFuture(() => getAllSubjectTopicConnections)
      val subjects = tryToFuture(() => getAllSubjects)
      val topicResourceConnections = tryToFuture(() => getAllTopicResourceConnections)
      val topicSubtopicConnections = tryToFuture(() => getAllTopicSubtopicConnections)
      val topics = tryToFuture(() => getAllTopics)

      val x = for {
        f2 <- relevances
        f4 <- resourceResourceTypeConnections
        f5 <- resourceTypes
        f6 <- resources
        f7 <- subjectTopicConnections
        f8 <- subjects
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

    private def get[A](url: String, params: (String, String)*)(implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(url).timeout(60000, 60000).params(params))
    }
  }
}
