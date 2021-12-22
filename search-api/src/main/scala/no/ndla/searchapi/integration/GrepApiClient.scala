/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import java.util.concurrent.Executors

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.NdlaClient
import no.ndla.searchapi.SearchApiProperties.GrepApiUrl
import no.ndla.searchapi.caching.Memoize
import no.ndla.searchapi.model.api.GrepException
import no.ndla.searchapi.model.domain.RequestInfo
import no.ndla.searchapi.model.grep._
import org.json4s.DefaultFormats
import scalaj.http.Http

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

trait GrepApiClient {
  this: NdlaClient =>
  val grepApiClient: GrepApiClient

  class GrepApiClient extends LazyLogging {
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
    private val GrepApiEndpoint = s"$GrepApiUrl/kl06/v201906"

    def getAllKjerneelementer: Try[List[GrepElement]] =
      get[List[GrepElement]](s"$GrepApiEndpoint/kjerneelementer-lk20/").map(_.distinct)

    def getAllKompetansemaal: Try[List[GrepElement]] =
      get[List[GrepElement]](s"$GrepApiEndpoint/kompetansemaal-lk20/").map(_.distinct)

    def getAllTverrfagligeTemaer: Try[List[GrepElement]] =
      get[List[GrepElement]](s"$GrepApiEndpoint/tverrfaglige-temaer-lk20/").map(_.distinct)

    val getGrepBundle: Memoize[Try[GrepBundle]] = Memoize(() => getGrepBundleUncached)

    /** The memoized function of this [[getGrepBundle]] should probably be used in most cases */
    private def getGrepBundleUncached: Try[GrepBundle] = {
      logger.info("Fetching grep in bulk...")
      val startFetch = System.currentTimeMillis()
      implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(3))

      val requestInfo = RequestInfo()

      /** Calls function in separate thread and converts Try to Future */
      def tryToFuture[T](x: () => Try[T]) = Future { requestInfo.setRequestInfo(); x() }.flatMap(Future.fromTry)

      val kjerneelementer = tryToFuture(() => getAllKjerneelementer)
      val kompetansemaal = tryToFuture(() => getAllKompetansemaal)
      val tverrfagligeTemaer = tryToFuture(() => getAllTverrfagligeTemaer)

      val x = for {
        f1 <- kjerneelementer
        f2 <- kompetansemaal
        f3 <- tverrfagligeTemaer
      } yield GrepBundle(f1, f2, f3)

      Try(Await.result(x, Duration(300, "seconds"))) match {
        case Success(bundle) =>
          logger.info(s"Fetched grep in ${System.currentTimeMillis() - startFetch}ms...")
          Success(bundle)
        case Failure(ex) =>
          logger.error(s"Could not fetch grep bundle (${ex.getMessage})", ex)
          Failure(GrepException("Could not fetch grep bundle..."))
      }
    }

    private def get[A](url: String, params: (String, String)*)(implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetch[A](Http(url).timeout(60000, 60000).params(params))
    }
  }
}
