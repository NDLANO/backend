/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

trait Content {
  def id: Option[Long]
  def revision: Option[Int]
}
