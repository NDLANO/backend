/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.LearningStep
import scalikejdbc.*

object DBLearningStep extends SQLSyntaxSupport[LearningStep] {
  override val tableName                                                                  = "learningsteps"
  def fromResultSet(ls: SyntaxProvider[LearningStep])(rs: WrappedResultSet): LearningStep =
    fromResultSet(ls.resultName)(rs)

  def fromResultSet(ls: ResultName[LearningStep])(rs: WrappedResultSet): LearningStep = {
    val jsonStr = rs.string(ls.c("document"))
    val meta    = CirceUtil.unsafeParseAs[LearningStep](jsonStr)
    LearningStep(
      Some(rs.long(ls.c("id"))),
      Some(rs.int(ls.c("revision"))),
      rs.stringOpt(ls.c("external_id")),
      Some(rs.long(ls.c("learning_path_id"))),
      meta.seqNo,
      meta.title,
      meta.introduction,
      meta.description,
      meta.embedUrl,
      meta.articleId,
      meta.`type`,
      meta.copyright,
      meta.created,
      meta.lastUpdated,
      meta.owner,
      meta.showTitle,
      meta.status
    )
  }

  def opt(ls: ResultName[LearningStep])(rs: WrappedResultSet): Option[LearningStep] =
    rs.longOpt(ls.c("id")).map(_ => fromResultSet(ls)(rs))
}
