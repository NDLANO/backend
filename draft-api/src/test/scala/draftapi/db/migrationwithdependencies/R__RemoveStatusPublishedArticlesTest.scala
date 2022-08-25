/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package draftapi.db.migrationwithdependencies

import no.ndla.common.model.domain.Status
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class R__RemoveStatusPublishedArticlesTest extends UnitSuite with TestEnvironment {
  val migration = new R__RemoveStatusPublishedArticles(props)

  test("published articles should only have imported as other status") {
    val importedAndQuality =
      Status(current = DraftStatus.PUBLISHED, other = Set(DraftStatus.IMPORTED, DraftStatus.QUALITY_ASSURED))
    val imported = Status(current = DraftStatus.PUBLISHED, other = Set(DraftStatus.IMPORTED))
    val publishedNotImported =
      Status(current = DraftStatus.PUBLISHED, other = Set(DraftStatus.QUALITY_ASSURED, DraftStatus.PROPOSAL))
    val published = Status(current = DraftStatus.PUBLISHED, other = Set())
    val notPublished =
      Status(current = DraftStatus.DRAFT, other = Set(DraftStatus.QUALITY_ASSURED, DraftStatus.IMPORTED))

    migration.updateStatus(importedAndQuality) should be(imported)
    migration.updateStatus(imported) should be(imported)
    migration.updateStatus(publishedNotImported) should be(published)
    migration.updateStatus(notPublished) should be(notPublished)
  }

}
