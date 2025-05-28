/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import enumeratum.*
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed trait RobotStatus extends EnumEntry
object RobotStatus extends Enum[RobotStatus] with CirceEnum[RobotStatus] {
  override def values: IndexedSeq[RobotStatus] = findValues
  case object PRIVATE extends RobotStatus
  case object SHARED  extends RobotStatus

  implicit val codec: PlainCodec[RobotStatus] = plainCodecEnumEntry[RobotStatus]
  implicit val schema: Schema[RobotStatus]    = schemaForEnumEntry[RobotStatus]
}
