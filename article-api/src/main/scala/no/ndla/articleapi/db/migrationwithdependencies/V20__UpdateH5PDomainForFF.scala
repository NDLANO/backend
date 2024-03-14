/*
 * Part of NDLA article-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.articleapi.ArticleApiProperties
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V20__UpdateH5PDomainForFF(props: ArticleApiProperties) extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
