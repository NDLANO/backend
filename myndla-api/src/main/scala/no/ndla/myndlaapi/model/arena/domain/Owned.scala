/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.arena.domain

trait Owned {
  def id: Long
  def ownerId: Option[Long]

  def locked: Boolean = false
}
