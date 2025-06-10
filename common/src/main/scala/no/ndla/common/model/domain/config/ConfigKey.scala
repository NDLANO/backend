/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.config

import enumeratum.*
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class ConfigKey(override val entryName: String) extends EnumEntry

object ConfigKey extends Enum[ConfigKey] with CirceEnum[ConfigKey] {
  case object LearningpathWriteRestricted extends ConfigKey("LEARNINGPATH_WRITE_RESTRICTED")
  case object MyNDLAWriteRestricted       extends ConfigKey("MY_NDLA_WRITE_RESTRICTED")

  val values: IndexedSeq[ConfigKey] = findValues

  val all: Seq[String] = values.map(_.entryName)

  def valueOf(s: String): Option[ConfigKey] = ConfigKey.values.find(_.entryName == s)
  val schema: Schema[ConfigKey]             = schemaForEnumEntry[ConfigKey]
}
