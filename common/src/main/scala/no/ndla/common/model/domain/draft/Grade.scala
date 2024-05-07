/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import com.scalatsi.TypescriptType.{TSLiteralNumber, TSUnion}
import com.scalatsi.{TSNamedType, TSType}
import enumeratum.values.{IntCirceEnum, IntEnum, IntEnumEntry}
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class Grade(val value: Int) extends IntEnumEntry

object Grade extends IntEnum[Grade] with IntCirceEnum[Grade] {

  val values: IndexedSeq[Grade]      = findValues
  implicit val schema: Schema[Grade] = schemaForIntEnumEntry[Grade]

  case object One   extends Grade(1)
  case object Two   extends Grade(2)
  case object Three extends Grade(3)
  case object Four  extends Grade(4)
  case object Five  extends Grade(5)

  implicit val enumTsType: TSNamedType[Grade] = TSType.alias[Grade](
    "Grade",
    TSUnion(values.map(e => TSLiteralNumber(e.value)))
  )
}
