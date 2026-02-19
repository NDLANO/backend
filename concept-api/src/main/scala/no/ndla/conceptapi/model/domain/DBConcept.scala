/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.domain

import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.concept.Concept
import no.ndla.conceptapi.Props
import scalikejdbc.*

class DBConcept(using props: Props) extends SQLSyntaxSupport[Concept] {
  override val tableName                  = "conceptdata"
  override val schemaName: Option[String] = Some(props.MetaSchema)

  def fromResultSet(lp: SyntaxProvider[Concept])(rs: WrappedResultSet): Concept = fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[Concept])(rs: WrappedResultSet): Concept = {

    val id       = rs.long(lp.c("id"))
    val revision = rs.int(lp.c("revision"))
    val jsonStr  = rs.string(lp.c("document"))

    val meta = CirceUtil.unsafeParseAs[Concept](jsonStr)

    new Concept(
      id = Some(id),
      revision = Some(revision),
      meta.title,
      meta.content,
      meta.copyright,
      meta.created,
      meta.updated,
      meta.updatedBy,
      meta.tags,
      meta.status,
      meta.visualElement,
      meta.responsible,
      meta.conceptType,
      meta.glossData,
      meta.editorNotes,
    )
  }
}

class PublishedConcept(using props: Props) extends SQLSyntaxSupport[Concept] {
  override val tableName                  = "publishedconceptdata"
  override val schemaName: Option[String] = Some(props.MetaSchema)
}
