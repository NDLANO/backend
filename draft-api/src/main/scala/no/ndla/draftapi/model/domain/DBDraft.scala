/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.domain

import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Responsible
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.Props
import scalikejdbc.*

class DBDraft(using props: Props) extends SQLSyntaxSupport[Draft] {
  override def tableName                  = "articledata"
  override def schemaName: Option[String] = Some(props.MetaSchema)

  def fromResultSet(dr: SyntaxProvider[Draft])(rs: WrappedResultSet): Draft = fromResultSet(dr.resultName)(rs)

  def fromResultSet(dr: ResultName[Draft])(rs: WrappedResultSet): Draft = {
    val meta        = CirceUtil.unsafeParseAs[Draft](rs.string(dr.c("document")))
    val slug        = rs.stringOpt(dr.c("slug"))
    val externalIds = rs
      .arrayOpt(dr.c("external_id"))
      .map(_.getArray.asInstanceOf[Array[String]].toList.flatMap(Option(_)))
      .filter(_.nonEmpty)
    val responsibleId        = rs.stringOpt(dr.c("responsible"))
    val responsibleUpdatedAt = rs.getOpt[NDLADate](dr.c("responsible_updated_at"))
    val responsible          = responsibleId.zip(responsibleUpdatedAt).map(Responsible.apply)

    meta.copy(
      id = Some(rs.long(dr.c("article_id"))),
      revision = Some(rs.int(dr.c("revision"))),
      externalIds = externalIds,
      slug = slug,
      responsible = responsible,
    )
  }
}
