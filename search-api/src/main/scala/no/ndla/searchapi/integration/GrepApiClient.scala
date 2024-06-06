/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import java.util.concurrent.Executors
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import no.ndla.network.NdlaClient
import no.ndla.network.model.RequestInfo
import no.ndla.searchapi.Props
import no.ndla.searchapi.caching.Memoize
import no.ndla.searchapi.model.api.GrepException
import no.ndla.searchapi.model.grep.*
import sttp.client3.quick.*

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

trait GrepApiClient {
  this: NdlaClient & Props =>
  val grepApiClient: GrepApiClient

  class GrepApiClient extends StrictLogging {
    import props.GrepApiUrl
    private val GrepApiEndpoint = s"$GrepApiUrl/kl06/v201906"

    private def getAllKjerneelementer: Try[List[GrepElement]] =
      get[List[GrepElement]](s"$GrepApiEndpoint/kjerneelementer-lk20/").map(_.distinct)

    private def getAllKompetansemaal: Try[List[GrepElement]] =
      get[List[GrepElement]](s"$GrepApiEndpoint/kompetansemaal-lk20/").map(_.distinct)

    private def getAllTverrfagligeTemaer: Try[List[GrepElement]] =
      get[List[GrepElement]](s"$GrepApiEndpoint/tverrfaglige-temaer-lk20/").map(_.distinct)

    // NOTE: We add a helper so we don't have to provide `()` where this is used :^)
    val getGrepBundle: () => Try[GrepBundle] = () => _getGrepBundle(())

    private val _getGrepBundle: Memoize[Unit, Try[GrepBundle]] = new Memoize(1000 * 60, _ => getGrepBundleUncached)

    /** The memoized function of this [[getGrepBundle]] should probably be used in most cases */
    private def getGrepBundleUncached: Try[GrepBundle] = {
      logger.info("Fetching grep in bulk...")
      val startFetch                            = System.currentTimeMillis()
      implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(3))

      val requestInfo = RequestInfo.fromThreadContext()

      /** Calls function in separate thread and converts Try to Future */
      def tryToFuture[T](x: () => Try[T]) = Future {
        requestInfo.setThreadContextRequestInfo()
        x()
      }.flatMap(Future.fromTry)

      val kjerneelementer    = tryToFuture(() => getAllKjerneelementer)
      val kompetansemaal     = tryToFuture(() => getAllKompetansemaal)
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

    private def get[A: Decoder](url: String, params: (String, String)*): Try[A] = {
      val request = quickRequest.get(uri"$url?$params").readTimeout(60.seconds)
      ndlaClient.fetch[A](request)
    }
  }
}
