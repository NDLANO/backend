/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

import enumeratum._

sealed abstract class ConfigKey(override val entryName: String) extends EnumEntry

object ConfigKey extends Enum[ConfigKey] with CirceEnum[ConfigKey] {
  case object LearningpathWriteRestricted extends ConfigKey("LEARNINGPATH_WRITE_RESTRICTED")
  case object MyNDLAWriteRestricted       extends ConfigKey("MY_NDLA_WRITE_RESTRICTED")
  case object ArenaEnabledOrgs            extends ConfigKey("ARENA_ENABLED_ORGS")

  val values: IndexedSeq[ConfigKey] = findValues

  val all: Seq[String] = values.map(_.entryName)

  def valueOf(s: String): Option[ConfigKey] = ConfigKey.values.find(_.entryName == s)
}
