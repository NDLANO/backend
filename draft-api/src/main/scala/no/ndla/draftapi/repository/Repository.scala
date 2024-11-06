/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.repository

import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

trait Repository[T] {
  def minMaxId(implicit session: DBSession = AutoSession): (Long, Long)
  def documentsWithIdBetween(min: Long, max: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[T]
}
