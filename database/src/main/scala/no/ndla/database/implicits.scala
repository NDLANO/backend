/*
 * Part of NDLA database
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import scalikejdbc.*

object implicits {
  extension (sc: StringContext) {
    def tsql[A](args: Any*): TrySql[A, NoExtractor] = TrySql(sc.sql(args*))
  }

  implicit def dbSessionToUnspecifiedSession(using session: DBSession): UnspecifiedDbSession =
    session.asInstanceOf[UnspecifiedDbSession]
}
