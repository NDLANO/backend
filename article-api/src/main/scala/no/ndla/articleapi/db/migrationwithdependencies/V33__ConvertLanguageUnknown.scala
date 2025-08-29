/*
 * Part of NDLA article-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.articleapi.ArticleApiProperties
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V33__ConvertLanguageUnknown(properties: ArticleApiProperties) extends BaseJavaMigration {
  given props: ArticleApiProperties            = properties
  override def migrate(context: Context): Unit = {}
}
