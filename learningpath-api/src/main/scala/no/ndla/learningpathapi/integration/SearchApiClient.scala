/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.network.tapir.auth.TokenUser

import scala.concurrent.Future
import scala.util.Try

trait SearchApiClient {
  def deleteLearningPathDocument(id: Long, user: Option[TokenUser]): Try[?]
  def indexLearningPathDocument(document: LearningPath, user: Option[TokenUser]): Future[Try[?]]
}
