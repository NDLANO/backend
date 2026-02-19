/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.domain

import no.ndla.common.model.domain.frontpage.SubjectPage
import no.ndla.frontpageapi.Props
import scalikejdbc.*

import scala.util.Try

class DBSubjectPage(using props: Props) {

  object DBSubjectPage extends SQLSyntaxSupport[SubjectPage] {
    override val tableName                  = "subjectpage"
    override val schemaName: Option[String] = Some(props.MetaSchema)

    def fromDb(lp: SyntaxProvider[SubjectPage])(rs: WrappedResultSet): Try[SubjectPage] = fromDb(lp.resultName)(rs)

    private def fromDb(lp: ResultName[SubjectPage])(rs: WrappedResultSet): Try[SubjectPage] = {
      val id       = rs.long(lp.c("id"))
      val document = rs.string(lp.c("document"))

      SubjectPage.decodeJson(document, id)
    }

  }
}
