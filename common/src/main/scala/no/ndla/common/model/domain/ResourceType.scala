/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import com.scalatsi.TypescriptType.{TSLiteralString, TSUnion}
import com.scalatsi.{TSNamedType, TSType}
import enumeratum.*
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class ResourceType(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}

object ResourceType extends Enum[ResourceType] with CirceEnum[ResourceType] {
  override val values: IndexedSeq[ResourceType] = findValues

  implicit val codec: PlainCodec[ResourceType] = plainCodecEnumEntry[ResourceType]
  implicit val schema: Schema[ResourceType]    = schemaForEnumEntry[ResourceType]
  implicit val enumTsType: TSNamedType[ResourceType] =
    TSType.alias[ResourceType]("ResourceType", TSUnion(values.map(e => TSLiteralString(e.entryName))))

  case object Concept           extends ResourceType("concept")
  case object Image             extends ResourceType("image")
  case object Audio             extends ResourceType("audio")
  case object Multidisciplinary extends ResourceType("multidisciplinary")
  case object Article           extends ResourceType("article")
  case object Learningpath      extends ResourceType("learningpath")
  case object Video             extends ResourceType("video")
  case object Folder            extends ResourceType("folder")
}
