/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.common.util.TraitUtil

class V67__ComputeSearchTraitsAgain(using TraitUtil) extends V62__ComputeSearchTraits {
  override def convertColumn(value: String): String = {
    super.convertColumn(value)
  }
}
