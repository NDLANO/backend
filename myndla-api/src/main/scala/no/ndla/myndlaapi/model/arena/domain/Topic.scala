/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.domain

import scalikejdbc._

case class Topic(
)

object Topic extends SQLSyntaxSupport[Topic] {
  override val tableName: String = "topics"
}
