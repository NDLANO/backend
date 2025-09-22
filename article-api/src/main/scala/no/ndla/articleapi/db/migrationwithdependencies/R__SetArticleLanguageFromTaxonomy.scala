/*
 * Part of NDLA article-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.ArticleApiProperties
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class R__SetArticleLanguageFromTaxonomy(properties: ArticleApiProperties) extends BaseJavaMigration {
  given props: ArticleApiProperties = properties
  val dbArticle                     = new DBArticle
  override def getChecksum: Integer = 1 // Change this to something else if you want to repeat migration

  override def migrate(context: Context): Unit = {}
}
