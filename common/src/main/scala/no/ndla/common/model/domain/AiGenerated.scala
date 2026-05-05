/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import enumeratum.*
import no.ndla.common.errors.ValidationException
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

import scala.util.{Failure, Success, Try}

sealed abstract class AiGenerated(override val entryName: String) extends EnumEntry

object AiGenerated extends Enum[AiGenerated] with CirceEnum[AiGenerated] {
  case object Partial extends AiGenerated("partial")
  case object Yes     extends AiGenerated("yes")
  case object No      extends AiGenerated("no")

  val values: IndexedSeq[AiGenerated] = findValues

  def all: Seq[String]                        = AiGenerated.values.map(_.entryName)
  def valueOf(s: String): Option[AiGenerated] = AiGenerated.withNameOption(s)

  def valueOfOrError(s: String): Try[AiGenerated] = valueOf(s) match {
    case Some(p) => Success(p)
    case None    =>
      val validGeneratedValues = values.map(_.toString).mkString(", ")
      Failure(ValidationException("aiGenerated", s"'$s' is not a valid priority. Must be one of $validGeneratedValues"))
  }

  implicit def schema: Schema[AiGenerated]    = schemaForEnumEntry[AiGenerated]
  implicit def codec: PlainCodec[AiGenerated] = plainCodecEnumEntry[AiGenerated]
}
