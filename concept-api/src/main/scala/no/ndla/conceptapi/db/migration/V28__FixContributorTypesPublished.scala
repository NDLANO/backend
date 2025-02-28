/*
 * Part of NDLA concept-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.db.migration

class V28__FixContributorTypesPublished extends V27__FixContributorTypes {
  override val tableName: String  = "publishedconceptdata"
  override val columnName: String = "document"

  override def convertColumn(value: String): String = {
    super.convertColumn(value)
  }
}
