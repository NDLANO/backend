package no.ndla.common

import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import scala.util.Try

object DateParser {
  private val formatter              = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private val formatterWithoutMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
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
}
