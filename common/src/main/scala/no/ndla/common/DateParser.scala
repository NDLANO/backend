package no.ndla.common

import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

object DateParser {
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  def fromString(str: String): LocalDateTime = LocalDateTime.parse(str, formatter)
  def fromUnixTime(timestamp: Long): LocalDateTime = {
    LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC)
  }
}
