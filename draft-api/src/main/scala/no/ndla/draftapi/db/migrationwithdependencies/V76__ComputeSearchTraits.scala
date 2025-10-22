/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migrationwithdependencies

import io.circe.syntax.*
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.ArticleContent
import no.ndla.common.util.TraitUtil
import no.ndla.database.DocumentMigration

class V76__ComputeSearchTraits(using traitUtil: TraitUtil) extends DocumentMigration {
  override val columnName: String = "document"
  override val tableName: String  = "articledata"

  override def convertColumn(value: String): String = {
    val oldDocument = CirceUtil.unsafeParse(value)
    val contents    = oldDocument.hcursor.get[Seq[ArticleContent]]("content").toTry.getOrElse(Seq.empty)
    val traits      = traitUtil.getArticleTraits(contents)
    val newDocument = oldDocument.hcursor.withFocus(_.mapObject(_.add("traits", traits.asJson)))

    newDocument.top.get.noSpaces
  }
}
