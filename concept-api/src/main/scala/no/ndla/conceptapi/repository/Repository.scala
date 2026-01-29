/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.repository

import no.ndla.common.model.domain.concept.Concept
import scalikejdbc.DBSession

trait Repository[T <: Concept] {
  def minMaxId(implicit session: DBSession): (Long, Long)
  def documentsWithIdBetween(min: Long, max: Long): Seq[T]
}
