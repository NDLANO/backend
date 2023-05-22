/*
 * Part of NDLA search.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.search

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.JavaClient
import io.lemonlabs.uri.typesafe.dsl._
import no.ndla.common.configuration.HasBaseProps
import org.apache.http.client.config.RequestConfig
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback

import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

trait Elastic4sClient {
  this: HasBaseProps =>
  var e4sClient: NdlaE4sClient

  case class NdlaE4sClient(searchServer: String) {
    private var client: ElasticClient = Elastic4sClientFactory.getNonSigningClient(searchServer)
    def recreateClient(): Unit        = client = Elastic4sClientFactory.getNonSigningClient(searchServer)

    implicit val ec: ExecutionContextExecutor =
      ExecutionContext.fromExecutor(Executors.newWorkStealingPool(props.MAX_SEARCH_THREADS))

    def executeAsync[T, U](
        request: T
    )(implicit handler: Handler[T, U], mf: Manifest[U], ec: ExecutionContext): Future[Try[RequestSuccess[U]]] = {

      val result = client.execute(request).map {
        case failure: RequestFailure   => Failure(NdlaSearchException(failure))
        case result: RequestSuccess[U] => Success(result)
      }

      result.onComplete {
        case Success(Success(_: RequestSuccess[U])) =>
        case _                                      => recreateClient()
      }

      result
    }

    def executeBlocking[T, U](
        request: T
    )(implicit handler: Handler[T, U], mf: Manifest[U], ec: ExecutionContext): Try[RequestSuccess[U]] = {
      Try(Await.result(this.executeAsync(request), Duration.Inf)).flatten
    }

    def execute[T, U](request: T)(implicit handler: Handler[T, U], mf: Manifest[U]): Try[RequestSuccess[U]] = {
      Try(Await.result(this.executeAsync(request), Duration.Inf)).flatten
    }
  }

  object Elastic4sClientFactory {

    def getClient(searchServer: String): NdlaE4sClient = NdlaE4sClient(searchServer)

    private def getProperties(searchServer: String, defaultPort: Int): ElasticProperties = {
      val scheme = searchServer.schemeOption.getOrElse("http")
      val host   = searchServer.hostOption.map(_.toString()).getOrElse("localhost")
      val port   = searchServer.port.getOrElse(defaultPort)

      ElasticProperties(s"$scheme://$host:$port?ssl=false")
    }

    private class RequestConfigCallbackWithTimeout extends RequestConfigCallback {

      override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder = {
        val elasticSearchRequestTimeoutMs = 10000
        requestConfigBuilder.setConnectionRequestTimeout(elasticSearchRequestTimeoutMs)
      }
    }

    def getNonSigningClient(searchServer: String): ElasticClient = {
      val props                 = getProperties(searchServer, 9200)
      val requestConfigCallback = new RequestConfigCallbackWithTimeout
      ElasticClient(
        JavaClient(
          props,
          requestConfigCallback
        )
      )
    }
  }
}
