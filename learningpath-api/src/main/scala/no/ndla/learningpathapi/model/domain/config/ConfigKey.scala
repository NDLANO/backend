/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

import enumeratum._

sealed abstract class ConfigKey(override val entryName: String) extends EnumEntry

object ConfigKey extends Enum[ConfigKey] {
  case object IsWriteRestricted extends ConfigKey("IS_WRITE_RESTRICTED")

  val values: IndexedSeq[ConfigKey] = findValues

  val all: Seq[String] = values.map(_.entryName)

  def valueOf(s: String): Option[ConfigKey] = ConfigKey.values.find(_.entryName == s)

}
