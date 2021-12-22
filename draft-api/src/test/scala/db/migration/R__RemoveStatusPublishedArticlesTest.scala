/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration
import no.ndla.draftapi.model.domain.{Article, ArticleStatus, Status}
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class R__RemoveStatusPublishedArticlesTest extends UnitSuite with TestEnvironment {
  val migration = new R__RemoveStatusPublishedArticles

  test("published articles should only have imported as other status") {
    val importedAndQuality =
      Status(current = ArticleStatus.PUBLISHED, other = Set(ArticleStatus.IMPORTED, ArticleStatus.QUALITY_ASSURED))
    val imported = Status(current = ArticleStatus.PUBLISHED, other = Set(ArticleStatus.IMPORTED))
    val publishedNotImported =
      Status(current = ArticleStatus.PUBLISHED, other = Set(ArticleStatus.QUALITY_ASSURED, ArticleStatus.PROPOSAL))
    val published = Status(current = ArticleStatus.PUBLISHED, other = Set())
    val notPublished =
      Status(current = ArticleStatus.DRAFT, other = Set(ArticleStatus.QUALITY_ASSURED, ArticleStatus.IMPORTED))

    migration.updateStatus(importedAndQuality) should be(imported)
    migration.updateStatus(imported) should be(imported)
    migration.updateStatus(publishedNotImported) should be(published)
    migration.updateStatus(notPublished) should be(notPublished)
  }

}
