/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.repository

import scalikejdbc.DBSession
import scala.util.Try

trait Repository[T] {
  def minMaxId(using session: DBSession): Try[(Long, Long)]
  def documentsWithIdBetween(min: Long, max: Long)(using session: DBSession): Try[Seq[T]]
}
