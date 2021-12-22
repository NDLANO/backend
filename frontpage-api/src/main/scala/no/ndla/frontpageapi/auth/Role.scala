/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.auth

object Role extends Enumeration {
  val WRITE: Role.Value = Value

  val prefix = "frontpage:"

  def valueOf(s: String): Option[Role.Value] = {
    val role = s.split(prefix)
    Role.values.find(_.toString == role.lastOption.getOrElse("").toUpperCase)
  }
}
