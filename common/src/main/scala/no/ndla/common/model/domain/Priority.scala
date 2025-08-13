/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import enumeratum.*
import no.ndla.common.errors.ValidationException

import scala.util.{Failure, Success, Try}
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class Priority(override val entryName: String) extends EnumEntry

object Priority extends Enum[Priority] with CirceEnum[Priority] {
  case object Prioritized extends Priority("prioritized")
  case object OnHold      extends Priority("on-hold")
  case object Unspecified extends Priority("unspecified")

  val values: IndexedSeq[Priority] = findValues

  def all: Seq[String]                     = Priority.values.map(_.entryName)
  def valueOf(s: String): Option[Priority] = Priority.withNameOption(s)

  def valueOfOrError(s: String): Try[Priority] =
    valueOf(s) match {
      case Some(p) => Success(p)
      case None    =>
        val validPriorities = values.map(_.toString).mkString(", ")
        Failure(
          ValidationException("priority", s"'$s' is not a valid priority. Must be one of $validPriorities")
        )
    }

  implicit val schema: Schema[Priority]    = schemaForEnumEntry[Priority]
  implicit val codec: PlainCodec[Priority] = plainCodecEnumEntry[Priority]
}
