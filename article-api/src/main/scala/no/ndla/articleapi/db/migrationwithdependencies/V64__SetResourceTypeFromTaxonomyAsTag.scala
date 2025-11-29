/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.database.FinishedMigration

case class DocumentRow(id: Long, document: String, article_id: Long)

class V64__SetResourceTypeFromTaxonomyAsTag extends FinishedMigration {}
