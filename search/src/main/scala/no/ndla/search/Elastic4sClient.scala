/*
 * Part of NDLA search
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.search

import com.sksamuel.elastic4s.*
import com.sksamuel.elastic4s.http.{JavaClient, JavaClientExceptionWrapper}
import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.common.configuration.BaseProps
import no.ndla.search.model.domain.DocumentConflictException
import org.apache.http.client.config.RequestConfig
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}
import scala.reflect.ClassTag

case class ExecutionOptions(retryLimit: Int = 5)
case class NdlaE4sClient(searchServer: String)(using props: BaseProps) extends StrictLogging {
  private var client: ElasticClient  = Elastic4sClientFactory.getNonSigningClient(searchServer)
  private def recreateClient(): Unit = {
    client = Elastic4sClientFactory.getNonSigningClient(searchServer)
  }

  private val elasticTimeout = 10.minutes
  private val backoffTimeout = 2.seconds
  private val warnTimeout    = 30.seconds

  def showQuery[T](t: T)(implicit handler: Handler[T, ?]): String = client.show(t)

  private val clientExecutionContext: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newWorkStealingPool(props.MAX_SEARCH_THREADS))

  def executeAsync[T, U: ClassTag](
      request: T,
      options: ExecutionOptions
  )(implicit
      handler: Handler[T, U],
      ec: ExecutionContext
  ): Future[Try[RequestSuccess[U]]] = {
    val response = client.execute(request)
    val result   = response.map {
      case RequestSuccess(status, body, headers, result) =>
        Success(RequestSuccess[U](status, body, headers, result))
      case RequestFailure(status, _, _, error) if status == 409 =>
        Failure(DocumentConflictException(error.reason))
      case failure: RequestFailure => Failure(NdlaSearchException(request, failure))
    }

    result.onComplete {
      case Success(Success(_: RequestSuccess[U])) =>
      case _                                      => recreateClient()
    }

    result.recoverWith {
      case ex: JavaClientExceptionWrapper if options.retryLimit > 0 =>
        logger.warn(s"Connection to Elasticsearch failed, retrying in $backoffTimeout", ex)
        Thread.sleep(backoffTimeout.toMillis)
        executeAsync(request, options.copy(retryLimit = options.retryLimit - 1))
    }
  }

  def executeBlocking[T, U](
      request: T,
      options: ExecutionOptions
  )(implicit handler: Handler[T, U], ct: ClassTag[U], ec: ExecutionContext): Try[RequestSuccess[U]] = {
    Try(Await.result(this.executeAsync(request, options), elasticTimeout)).flatten
  }

  def execute[T, U](request: T)(implicit handler: Handler[T, U], ct: ClassTag[U]): Try[RequestSuccess[U]] = {
    implicit val ec: ExecutionContextExecutor = clientExecutionContext
    executeBlocking(request, ExecutionOptions())
  }
}

object Elastic4sClientFactory {
  private val requestTimeoutMs = 10.minutes.toMillis.toInt

  def getClient(searchServer: String)(using props: BaseProps): NdlaE4sClient = NdlaE4sClient(searchServer)

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
