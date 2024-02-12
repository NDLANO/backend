/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model

import com.scalatsi.TSType
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import org.json4s.{CustomSerializer, JString, MappingException}
import scalikejdbc.ParameterBinderFactory
import sttp.tapir.Schema

import java.time.{Instant, LocalDateTime, Month, ZoneId, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

case class NDLADate(underlying: ZonedDateTime) extends Ordered[NDLADate] {

  private def withUnderlying(f: ZonedDateTime => ZonedDateTime): NDLADate = {
    this.copy(underlying = f(underlying))
  }

  def asUtcLocalDateTime: LocalDateTime = underlying.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime

  def minusSeconds(seconds: Long): NDLADate = withUnderlying(_.minusSeconds(seconds))
  def plusSeconds(seconds: Long): NDLADate  = withUnderlying(_.plusSeconds(seconds))
  def minusDays(days: Long): NDLADate       = withUnderlying(_.minusDays(days))
  def plusDays(days: Long): NDLADate        = withUnderlying(_.plusDays(days))
  def plusYears(years: Long): NDLADate      = withUnderlying(_.plusYears(years))
  def minusYears(years: Long): NDLADate     = withUnderlying(_.minusYears(years))
  def isAfter(date: NDLADate): Boolean      = underlying.isAfter(date.underlying)
  def isBefore(date: NDLADate): Boolean     = underlying.isBefore(date.underlying)

  def withYear(year: Int): NDLADate             = withUnderlying(_.withYear(year))
  def withMonth(month: Int): NDLADate           = withUnderlying(_.withMonth(month))
  def withDayOfMonth(dayOfMonth: Int): NDLADate = withUnderlying(_.withDayOfMonth(dayOfMonth))
  def withHour(hour: Int): NDLADate             = withUnderlying(_.withHour(hour))
  def withMinute(minute: Int): NDLADate         = withUnderlying(_.withMinute(minute))
  def withSecond(second: Int): NDLADate         = withUnderlying(_.withSecond(second))
  def withNano(nanoOfSecond: Int): NDLADate     = withUnderlying(_.withNano(nanoOfSecond))

  def toEpochSecond(offset: ZoneOffset): Long = asUtcLocalDateTime.toEpochSecond(offset)
  def toUTCEpochSecond: Long                  = toEpochSecond(ZoneOffset.UTC)

  def asUTCDateWithSameTime: NDLADate = withUnderlying(_.withZoneSameLocal(ZoneOffset.UTC))

  def asString: String = NDLADate.asString(this)

  override def compare(that: NDLADate): Int = {
    this.underlying.compareTo(that.underlying)
  }
}

object NDLADate {

  implicit val typescriptType: TSType[NDLADate] = TSType.sameAs[NDLADate, String]

  case class NDLADateError(message: String) extends RuntimeException(message)

  private val baseFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private val utcZone: ZoneId                  = ZoneId.of("UTC")
  private val localZone: ZoneId                = ZoneId.systemDefault()

  val MIN: NDLADate = fromDate(LocalDateTime.MIN)
  val MAX: NDLADate = fromDate(LocalDateTime.MAX)

  private val dateFormats: List[DateTimeFormatter] =
    baseFormatter +:
      List(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS"
      ).map(DateTimeFormatter.ofPattern)

  def now(): NDLADate = NDLADate.fromDate(ZonedDateTime.now(localZone))

  def fromUtcDate(date: LocalDateTime): NDLADate = {
    val zonedDate = date.atZone(utcZone)
    new NDLADate(zonedDate.withZoneSameInstant(localZone))
  }
  def fromDate(date: LocalDateTime): NDLADate = {
    val zonedDate = date.atZone(localZone)
    new NDLADate(zonedDate)
  }

  def fromUnixTime(timestamp: Long): NDLADate = {
    val date = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC)
    fromDate(date)
  }

  def of(
      year: Int,
      month: Int,
      dayOfMonth: Int,
      hour: Int,
      minute: Int,
      second: Int
  ): NDLADate =
    fromDate(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second))
  def of(
      year: Int,
      month: Int,
      dayOfMonth: Int,
      hour: Int,
      minute: Int
  ): NDLADate = fromDate(LocalDateTime.of(year, month, dayOfMonth, hour, minute))
  def of(
      year: Int,
      month: Int,
      dayOfMonth: Int,
      hour: Int,
      minute: Int,
      second: Int,
      nanoOfSecond: Int
  ): NDLADate = fromDate(ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, utcZone))

  def of(
      year: Int,
      month: Month,
      dayOfMonth: Int,
      hour: Int,
      minute: Int,
      second: Int
  ) = fromUtcDate(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second))

  def fromDate(date: ZonedDateTime): NDLADate = new NDLADate(date)

  def fromTimestampSeconds(seconds: Long): NDLADate =
    new NDLADate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault()))

  def fromString(str: String): Try[NDLADate] = {
    @tailrec
    def _fromString(formatsToTry: List[DateTimeFormatter]): Try[NDLADate] = {
      formatsToTry match {
        case headFormatter :: Nil =>
          Try(NDLADate.fromUtcDate(LocalDateTime.parse(str, headFormatter)))
        case headFormatter :: tail =>
          Try(NDLADate.fromUtcDate(LocalDateTime.parse(str, headFormatter))) match {
            case Failure(_) =>
              _fromString(tail)
            case Success(result) =>
              Success(result)
          }
        case Nil => Failure(NDLADateError("Got past end of formatters before returning, this is a bug."))
      }
    }

    _fromString(dateFormats).recoverWith(_ => Failure(NDLADateError(s"Was unable to parse '${str}' as a date.")))
  }

  def asString(date: NDLADate): String = {
    val toFormat = date.underlying.withZoneSameInstant(utcZone)
    baseFormatter.format(toFormat)
  }

  implicit def encoder: Encoder[NDLADate] = Encoder.instance(ndlaDate => {
    asString(ndlaDate).asJson
  })

  implicit def decoder: Decoder[NDLADate] = Decoder.instanceTry(cur => {
    cur.value.asString match {
      case Some(value) => fromString(value)
      case None        => Failure(NDLADateError(s"Failed to decode ${cur.value} as `NDLADate`"))
    }
  })

  implicit val schema: Schema[NDLADate] = Schema.schemaForLocalDateTime.as[NDLADate]

  class NDLADateJson4sSerializer
      extends CustomSerializer[NDLADate](_ =>
        (
          {
            // NOTE: Unsafe as everything json4s :^)
            case JString(s) =>
              NDLADate.fromString(s) match {
                case Success(date) => date
                case Failure(exception) =>
                  throw new MappingException(
                    exception.getMessage,
                    NDLADateError(exception.getMessage)
                  )
              }
          },
          { case x: NDLADate => JString(NDLADate.asString(x)) }
        )
      )
  val Json4sSerializer = new NDLADateJson4sSerializer

  implicit val parameterBinderFactory: ParameterBinderFactory[NDLADate] = ParameterBinderFactory[NDLADate] {
    v => (ps, idx) =>
      ps.setObject(idx, v.asUtcLocalDateTime)
  }

}
