/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.ArticleContent
import no.ndla.common.util.TraitUtil
import no.ndla.database.DocumentMigration
import io.circe.syntax.*

class V62__ComputeSearchTraits(using traitUtil: TraitUtil) extends DocumentMigration {
  override val columnName: String = "document"
  override val tableName: String  = "contentdata"

  override def convertColumn(value: String): String = {
    val oldDocument = CirceUtil.unsafeParse(value)
    val contents    = oldDocument.hcursor.get[Seq[ArticleContent]]("content").toTry.getOrElse(Seq.empty)
    val traits      = traitUtil.getArticleTraits(contents)
    val newDocument = oldDocument.hcursor
      .withFocus(_.mapObject(_.add("traits", traits.asJson)))

    newDocument.top.get.noSpaces
  }
}
