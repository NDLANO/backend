/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migrationwithdependencies

import no.ndla.common.util.TraitUtil

class V79__ComputeSearchTraitsAgain(using traitUtil: TraitUtil) extends V76__ComputeSearchTraits {
  override def convertColumn(value: String): String = {
    super.convertColumn(value)
  }
}
