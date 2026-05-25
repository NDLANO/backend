/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.integration

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.Title
import no.ndla.network.tapir.auth.TokenUser

import scala.util.Try

trait LearningpathApiClient {
  def getLearningpathsWithId(articleId: Long, user: TokenUser): Try[Seq[LearningPath]]
}

case class LearningPath(id: Long, title: Title)

object LearningPath {
  implicit val encoder: Encoder[LearningPath] = deriveEncoder
  implicit val decoder: Decoder[LearningPath] = deriveDecoder
}
