/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import scala.util.Try

object DateParser {
  private val formatter                      = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private val formatterWithoutMillis         = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
  def fromString(str: String): LocalDateTime = Try(
    LocalDateTime.parse(str, formatterWithoutMillis)
  ).getOrElse(
    LocalDateTime.parse(str, formatter)
  )
  def fromUnixTime(timestamp: Long): LocalDateTime = {
    LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC)
  }
  def dateToString(datetime: LocalDateTime, withMillis: Boolean): String = {
    val f = if (withMillis) formatter else formatterWithoutMillis
    datetime.format(f)
  }

  def decoderWithFormat(fmt: String): Decoder[LocalDateTime] = {
    val format = DateTimeFormatter.ofPattern(fmt)
    Decoder[String].emapTry(str => Try(LocalDateTime.parse(str, format)))
  }

  object Circe {
    implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
      decoderWithFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") or
        decoderWithFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") or
        decoderWithFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'") or
        decoderWithFormat("yyyy-MM-dd'T'HH:mm:ss") or
        decoderWithFormat("yyyy-MM-dd'T'HH:mm:ss.SSS") or
        decoderWithFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS")

    implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
      Encoder.instance(x => DateParser.dateToString(x, withMillis = false).asJson)
  }
}
