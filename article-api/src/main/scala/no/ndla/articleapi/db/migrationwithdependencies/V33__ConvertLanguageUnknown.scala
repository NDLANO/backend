/*
 * Part of NDLA article-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.articleapi.{ArticleApiProperties, Props}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V33__ConvertLanguageUnknown(properties: ArticleApiProperties) extends BaseJavaMigration with Props {
  override val props: ArticleApiProperties     = properties
  override def migrate(context: Context): Unit = {}
}
