/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import java.util.concurrent.Executors

import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.dsl._
import no.ndla.draftapi.DraftApiProperties.ConceptApiHost
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import org.json4s.Formats
import scalaj.http.Http

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

case class ConceptStatus(current: String)
case class DraftConcept(
    id: Long,
    status: ConceptStatus
)

trait ConceptApiClient {
  this: NdlaClient with ConverterService =>
  val conceptApiClient: ConceptApiClient

  class ConceptApiClient(conceptBaseUrl: String = s"http://$ConceptApiHost") extends LazyLogging {
    private val draftEndpoint = s"concept-api/v1/drafts"
    private val conceptTimeout = 1000 * 10 // 10 seconds

    def publishConceptsIfToPublishing(ids: Seq[Long]): Seq[Try[DraftConcept]] = {
      implicit val executionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(ids.size))
      val statusToPublish = "QUEUED_FOR_PUBLISHING"
      val shouldPublish = (c: DraftConcept) => c.status.current == statusToPublish

      ids.map(id => {
        getDraftConcept(id) match {
          case Success(concept) if shouldPublish(concept) => publishConcept(concept.id)
          case Success(concept) =>
            logger.info(
              s"Not publishing concept with id '${concept.id}' since status was '${concept.status.current}' and not '$statusToPublish'")
            Success(concept)
          case Failure(ex) =>
            logger.error(s"Something went wrong when fetching concept with id: '$id'", ex)
            Failure(ex)
        }
      })
    }

    private def publishConcept(id: Long)(implicit ec: ExecutionContext): Try[DraftConcept] = {
      put[DraftConcept](s"$draftEndpoint/$id/status/PUBLISHED", conceptTimeout)
    }

    private def getDraftConcept(id: Long)(implicit ec: ExecutionContext): Try[DraftConcept] =
      get[DraftConcept](s"$draftEndpoint/$id", 10 * 1000, "fallback" -> "true")

    private[integration] def get[T](path: String, timeout: Int, params: (String, String)*)(
        implicit mf: Manifest[T]): Try[T] = {
      implicit val formats: Formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
      ndlaClient.fetchWithForwardedAuth[T](
        Http(conceptBaseUrl / path)
          .timeout(timeout, timeout)
          .params(params)
      )
    }

    private[integration] def put[A](path: String, timeout: Int, params: (String, String)*)(
        implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        Http(conceptBaseUrl / path)
          .timeout(timeout, timeout)
          .params(params)
          .method("PUT")
      ) match {
        case Success(res) => Success(res)
        case Failure(ex)  => Failure(ex)
      }
    }
  }
}
