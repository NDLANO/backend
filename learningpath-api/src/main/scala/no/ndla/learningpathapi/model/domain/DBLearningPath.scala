/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.LearningPath
import no.ndla.learningpathapi.Props
import scalikejdbc.*

class DBLearningPath(using props: Props) extends SQLSyntaxSupport[LearningPath] {
  override val tableName                  = "learningpaths"
  override val schemaName: Option[String] = Some(props.MetaSchema)

  def fromResultSet(lp: SyntaxProvider[LearningPath])(rs: WrappedResultSet): LearningPath =
    fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[LearningPath])(rs: WrappedResultSet): LearningPath = {
    val jsonStr = rs.string(lp.c("document"))
    val meta    = CirceUtil.unsafeParseAs[LearningPath](jsonStr)
    meta.copy(
      id = Some(rs.long(lp.c("id"))),
      revision = Some(rs.int(lp.c("revision"))),
      externalId = rs.stringOpt(lp.c("external_id")),
    )
  }
}
