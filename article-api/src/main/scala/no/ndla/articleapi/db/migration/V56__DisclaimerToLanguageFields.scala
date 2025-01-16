/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import no.ndla.database.LanguageFieldMigration

class V56__DisclaimerToLanguageFields extends LanguageFieldMigration {
  override val columnName: String = "document"
  override val tableName: String  = "contentdata"
  override val fieldName: String  = "disclaimer"
}
