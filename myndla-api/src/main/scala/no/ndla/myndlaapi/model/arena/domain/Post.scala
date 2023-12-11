/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.domain

import scalikejdbc._

case class Post (

                )

object Post extends SQLSyntaxSupport[Post] {
  override val tableName: String = "posts"
}
