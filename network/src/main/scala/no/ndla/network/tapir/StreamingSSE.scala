/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import cats.implicits.catsSyntaxOptionId
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import ox.Chunk
import ox.flow.Flow
import sttp.model.sse.ServerSentEvent
import sttp.tapir.*
import sttp.tapir.server.netty.OxServerSentEvents
import sttp.tapir.server.netty.sync.OxStreams

import java.nio.charset.StandardCharsets

/** Typed Server-Sent-Events bodies for tapir. The wire format is plain SSE, but the value type the endpoint exposes is
  * `Flow[T]` instead of `Flow[ServerSentEvent]` — so the schema flowing into the OpenAPI spec describes `T` directly,
  * and callers handle the typed value rather than poking at SSE strings.
  */
object StreamingSSE {

  /** A streaming body that wraps a JSON-encoded `T` in each SSE event. The OpenAPI schema is `Schema[T]`, the value
    * passed to/from `serverLogic` is `Flow[T]`, and we keep per-event control over the SSE `event:` field via
    * `eventType`.
    *
    * On decode (client side) any SSE that fails to parse as `T` is dropped silently — the server is the source of truth
    * and its writes are validated by the encoder.
    */
  def jsonSSEBody[T](eventType: T => String)(using
      encoder: Encoder[T],
      decoder: Decoder[T],
      schema: Schema[T],
  ): StreamBodyIO[Flow[Chunk[Byte]], Flow[T], OxStreams] = {
    val tToServerSentEvent: Flow[T] => Flow[ServerSentEvent] =
      _.map(t => ServerSentEvent(data = Some(t.asJson.noSpaces), eventType = eventType(t).some))

    val serverSentEventToT: Flow[ServerSentEvent] => Flow[T] =
      _.mapConcat(sse => sse.data.flatMap(decode[T](_).toOption))

    StreamBodyIO(
      OxStreams,
      Codec.id(CodecFormat.TextEventStream(), schema.as[Flow[Chunk[Byte]]]),
      EndpointIO.Info.empty,
      Some(StandardCharsets.UTF_8),
      Nil,
    ).map(OxServerSentEvents.parseBytesToSSE.andThen(serverSentEventToT))(
      tToServerSentEvent.andThen(OxServerSentEvents.serializeSSEToBytes)
    )
  }
}
