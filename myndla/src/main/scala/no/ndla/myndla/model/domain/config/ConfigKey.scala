/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain.config

import com.scalatsi.{TSNamedType, TSType}
import com.scalatsi.TypescriptType.TSEnum
import enumeratum._

sealed abstract class ConfigKey(override val entryName: String) extends EnumEntry

object ConfigKey extends Enum[ConfigKey] with CirceEnum[ConfigKey] {
  case object LearningpathWriteRestricted extends ConfigKey("LEARNINGPATH_WRITE_RESTRICTED")
  case object MyNDLAWriteRestricted       extends ConfigKey("MY_NDLA_WRITE_RESTRICTED")
  case object ArenaEnabledOrgs            extends ConfigKey("ARENA_ENABLED_ORGS")
  case object ArenaEnabledUsers           extends ConfigKey("ARENA_ENABLED_USERS")
  case object AiEnabledOrgs               extends ConfigKey("AI_ENABLED_ORGS")

  val values: IndexedSeq[ConfigKey] = findValues

  val all: Seq[String] = values.map(_.entryName)

  def valueOf(s: String): Option[ConfigKey] = ConfigKey.values.find(_.entryName == s)

  private val tsEnumValues: Seq[(String, String)] = values.map(e => e.entryName -> e.entryName)
  implicit val enumTsType: TSNamedType[ConfigKey] = TSType.alias(
    "ConfigKey",
    TSEnum.string("ConfigKeyEnum", tsEnumValues: _*)
  )
}
