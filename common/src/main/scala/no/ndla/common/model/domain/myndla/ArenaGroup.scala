/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.myndla

import com.scalatsi.TypescriptType.{TSLiteralString, TSUnion}
import com.scalatsi.{TSNamedType, TSType}
import enumeratum.*

sealed trait ArenaGroup extends EnumEntry
object ArenaGroup extends Enum[ArenaGroup] with CirceEnum[ArenaGroup] {
  case object ADMIN extends ArenaGroup
  override def values: IndexedSeq[ArenaGroup] = findValues

  implicit val enumTsType: TSNamedType[ArenaGroup] =
    TSType.alias[ArenaGroup]("ArenaGroup", TSUnion(values.map(e => TSLiteralString(e.entryName))))
}
