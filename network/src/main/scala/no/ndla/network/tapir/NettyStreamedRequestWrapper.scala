/*
 * Part of NDLA network
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import io.netty.buffer.ByteBufUtil
import io.netty.handler.codec.DecoderResult
import io.netty.handler.codec.http.*
import org.playframework.netty.http.StreamedHttpRequest
import org.reactivestreams.{Subscriber, Subscription}
import ox.Chunk
import ox.channels.Sink

import scala.annotation.nowarn

case class NettyStreamedRequestWrapper(request: StreamedHttpRequest, bodySink: Sink[Chunk[Byte]], maxBodyBytes: Int)
    extends StreamedHttpRequest {

  override def subscribe(s: Subscriber[? >: HttpContent]): Unit = {
    val wrappedSubscriber: Subscriber[HttpContent] = new Subscriber[HttpContent] {
      private var bytesRead = 0

      override def onNext(t: HttpContent): Unit = {
        if (bytesRead < maxBodyBytes) {
          val byteBuf = t.content()
          val chunk   = Chunk.fromArray(ByteBufUtil.getBytes(byteBuf))
          bytesRead += chunk.size
          bodySink.send(chunk)
        }

        s.onNext(t)
      }

      override def onComplete(): Unit = {
        bodySink.done()
        s.onComplete()
      }

      override def onError(t: Throwable): Unit = {
        bodySink.done()
        s.onError(t)
      }

      override def onSubscribe(sub: Subscription): Unit = s.onSubscribe(sub)
    }

    request.subscribe(wrappedSubscriber)
  }

  @nowarn("cat=deprecation")
  override def getMethod: HttpMethod = request.getMethod

  override def method(): HttpMethod = request.method

  override def setMethod(method: HttpMethod): HttpRequest = request.setMethod(method)

  @nowarn("cat=deprecation")
  override def getUri: String = request.getUri

  override def uri(): String = request.uri

  override def setUri(uri: String): HttpRequest = request.setUri(uri)

  override def setProtocolVersion(version: HttpVersion): HttpRequest = request.setProtocolVersion(version)

  @nowarn("cat=deprecation")
  override def getProtocolVersion: HttpVersion = request.getProtocolVersion

  override def protocolVersion(): HttpVersion = request.protocolVersion

  override def headers(): HttpHeaders = request.headers

  @nowarn("cat=deprecation")
  override def getDecoderResult: DecoderResult = request.getDecoderResult

  override def decoderResult(): DecoderResult = request.decoderResult

  override def setDecoderResult(result: DecoderResult): Unit = request.setDecoderResult(result)
}
