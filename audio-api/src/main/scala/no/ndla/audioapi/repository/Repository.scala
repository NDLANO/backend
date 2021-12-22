/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.repository

import scalikejdbc.{AutoSession, DBSession}

import scala.util.Try

trait Repository[T] {
  def minMaxId(implicit session: DBSession = AutoSession): Try[(Long, Long)]
  def documentsWithIdBetween(min: Long, max: Long): Try[Seq[T]]
}
