/*
 * Part of NDLA search
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
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

trait Elastic4sClient {
  this: HasBaseProps =>
  var e4sClient: NdlaE4sClient

  case class NdlaE4sClient(searchServer: String) {
    private var client: ElasticClient  = Elastic4sClientFactory.getNonSigningClient(searchServer)
    private def recreateClient(): Unit = client = Elastic4sClientFactory.getNonSigningClient(searchServer)
    private val elasticTimeout         = 10.minutes

    def showQuery[T](t: T)(implicit handler: Handler[T, _]): String = client.show(t)

    private val clientExecutionContext: ExecutionContextExecutor =
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
      Try(Await.result(this.executeAsync(request), elasticTimeout)).flatten
    }

    def execute[T, U](request: T)(implicit handler: Handler[T, U], mf: Manifest[U]): Try[RequestSuccess[U]] = {
      implicit val ec: ExecutionContextExecutor = clientExecutionContext

      val future = this.executeAsync(request)
      Try(Await.result(future, elasticTimeout)).flatten
    }
  }

  object Elastic4sClientFactory {
    private val requestTimeoutMs = 10.minutes.toMillis.toInt

    def getClient(searchServer: String): NdlaE4sClient = NdlaE4sClient(searchServer)

    private def getProperties(searchServer: String, defaultPort: Int): ElasticProperties = {
      val scheme = searchServer.schemeOption.getOrElse("http")
      val host   = searchServer.hostOption.map(_.toString()).getOrElse("localhost")
      val port   = searchServer.port.getOrElse(defaultPort)

      ElasticProperties(s"$scheme://$host:$port?ssl=false")
    }

    private class RequestConfigCallbackWithTimeout extends RequestConfigCallback {
      override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder = {
        requestConfigBuilder
          .setConnectionRequestTimeout(requestTimeoutMs)
          .setSocketTimeout(requestTimeoutMs)
          .setConnectTimeout(requestTimeoutMs)
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
