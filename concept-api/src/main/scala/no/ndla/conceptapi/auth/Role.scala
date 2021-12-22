/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.auth

object Role extends Enumeration {
  val WRITE, ADMIN = Value

  def valueOf(s: String): Option[Role.Value] = {
    val role = s.split("concept:")
    Role.values.find(_.toString == role.lastOption.getOrElse("").toUpperCase)
  }
}
