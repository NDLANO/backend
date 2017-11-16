/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import org.json4s.FieldSerializer
import org.json4s.FieldSerializer.{renameFrom, renameTo}

case class Iso3166Def(code: String, name: String)
object Iso3166Def {
  val Serializer: FieldSerializer[Iso3166Def] = FieldSerializer[Iso3166Def](renameTo("code", "Code") orElse renameTo("name", "Name"), renameFrom("Code", "code") orElse renameFrom("Name", "name"))
}
