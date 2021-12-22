/*
 * Part of NDLA search-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.auth

object Role extends Enumeration {
  val DRAFTWRITE: Role.Value = Value("drafts:write")

  def valueOf(s: String): Option[Role.Value] = {
    Role.values.find(_.toString == s)
  }
}
