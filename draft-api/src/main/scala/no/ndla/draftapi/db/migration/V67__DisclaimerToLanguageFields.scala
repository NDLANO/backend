/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import no.ndla.database.LanguageFieldMigration

class V67__DisclaimerToLanguageFields extends LanguageFieldMigration {
  override val columnName: String = "document"
  override val tableName: String  = "articledata"
  override val fieldName: String  = "disclaimer"
}
