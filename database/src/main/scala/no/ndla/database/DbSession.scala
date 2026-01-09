/*
 * Part of NDLA database
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import scalikejdbc.DBSession

/** A readable [[scalikejdbc.DBSession]]. */
type ReadableDbSession <: DBSession

object ReadableDbSession {
  // Unsafely cast a DBSession to a ReadableDbSession. Only to be used within this package.
  private[database] def apply(s: DBSession): ReadableDbSession = s.asInstanceOf[ReadableDbSession]
}

/** A writeable [[scalikejdbc.DBSession]]. */
type WriteableDbSession <: ReadableDbSession

object WriteableDbSession {
  // Unsafely cast a DBSession to a WriteableDbSession. Only to be used within this package.
  private[database] def apply(s: DBSession): WriteableDbSession = s.asInstanceOf[WriteableDbSession]
}

/** A [[scalikejdbc.DBSession]] that is not known whether it is only readable, or also writeable. As such, it is assumed
  * to be writeable.
  *
  * An implicit conversion from [[scalikejdbc.DBSession]] to this type is provided in [[no.ndla.database.implicits]].
  */
type UnspecifiedDbSession <: WriteableDbSession
