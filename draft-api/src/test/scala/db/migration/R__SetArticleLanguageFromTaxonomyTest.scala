/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration
import no.ndla.draftapi.model.domain.ArticleTag
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class R__SetArticleLanguageFromTaxonomyTest extends UnitSuite with TestEnvironment {
  val migration = new R__SetArticleLanguageFromTaxonomy

  test("merge tags so that we only have distinct tags, and only in content language") {
    val oldTags = Seq(ArticleTag(Seq("one", "two", "three"), "en"))
    val newTags =
      Seq(ArticleTag(Seq("en", "to"), "nb"), ArticleTag(Seq("one", "two"), "en"), ArticleTag(Seq("uno", "dos"), "es"))
    val contentEnglish = Seq("en")
    val contentEnNb = Seq("en", "nb")
    val mergedNbEnTags = Seq(ArticleTag(Seq("en", "to"), "nb"), ArticleTag(Seq("one", "two", "three"), "en"))

    migration.mergeTags(oldTags, newTags, contentEnglish) should be(oldTags)
    migration.mergeTags(oldTags, newTags, contentEnNb).sortBy(_.language) should be(mergedNbEnTags.sortBy(_.language))
  }

}
