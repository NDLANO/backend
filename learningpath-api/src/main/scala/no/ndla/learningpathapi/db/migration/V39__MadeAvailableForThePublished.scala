/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.common.model.domain.learningpath.LearningPathStatus.{PUBLISHED, UNLISTED}
import no.ndla.database.DocumentMigration

class V39__MadeAvailableForThePublished extends DocumentMigration {
  override val columnName: String = "document"
  override val tableName: String  = "learningpaths"

  protected def convertColumn(document: String): String = {
    val oldLp = CirceUtil.unsafeParseAs[LearningPath](document)
    val madeAvailable = oldLp.status match {
      case UNLISTED | PUBLISHED => Some(oldLp.lastUpdated)
      case _                    => None
    }

    val newLearningPath = oldLp.copy(madeAvailable = madeAvailable)
    CirceUtil.toJsonString(newLearningPath)
  }
}
