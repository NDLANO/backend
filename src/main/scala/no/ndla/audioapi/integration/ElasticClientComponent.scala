/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.audioapi.integration

import java.time.{LocalDateTime, ZoneOffset}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.google.common.base.Supplier
import _root_.io.searchbox.client.JestClient
import _root_.io.searchbox.client.config.HttpClientConfig
import no.ndla.audioapi.AudioApiProperties
import org.apache.http.impl.client.{DefaultHttpRequestRetryHandler, HttpClientBuilder}
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import vc.inreach.aws.request.{AWSSigner, AWSSigningRequestInterceptor}

trait ElasticClientComponent {
  val jestClient: JestClient
}

object JestClientFactory {
  def getClient(searchServer: String = AudioApiProperties.SearchServer): JestClient = {
    AudioApiProperties.RunWithSignedSearchRequests match {
      case true => getSigningClient(searchServer)
      case false => getNonSigningClient(searchServer)
    }
  }

  private def getNonSigningClient(searchServer: String): JestClient = {
    val factory = new io.searchbox.client.JestClientFactory()
    factory.setHttpClientConfig(new HttpClientConfig.Builder(searchServer).build())
    factory.getObject
  }

  private def getSigningClient(searchServer: String): JestClient = {
    val clock: Supplier[LocalDateTime] = new Supplier[LocalDateTime] {
      override def get(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
    }

    val awsSigner = new AWSSigner(new DefaultAWSCredentialsProviderChain(), AudioApiProperties.SearchRegion, "es", clock);
    val requestInterceptor = new AWSSigningRequestInterceptor(awsSigner)

    val factory = new io.searchbox.client.JestClientFactory() {
      override def configureHttpClient(builder: HttpClientBuilder): HttpClientBuilder = {
        builder.addInterceptorLast(requestInterceptor)
        builder.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
        builder
      }

      override def configureHttpClient(builder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
        builder.addInterceptorLast(requestInterceptor)
        builder
      }
    }

    factory.setHttpClientConfig(new HttpClientConfig.Builder(searchServer).build())
    factory.getObject
  }
}
