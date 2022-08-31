/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migrationwithdependencies

import no.ndla.common.model.domain.Tag
import no.ndla.draftapi.db.migrationwithdependencies.R__SetArticleLanguageFromTaxonomy
import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class R__SetArticleLanguageFromTaxonomyTest extends UnitSuite with TestEnvironment {
  val migration = new R__SetArticleLanguageFromTaxonomy(props)

  test("merge tags so that we only have distinct tags, and only in content language") {
    val oldTags = Seq(Tag(Seq("one", "two", "three"), "en"))
    val newTags =
      Seq(Tag(Seq("en", "to"), "nb"), Tag(Seq("one", "two"), "en"), Tag(Seq("uno", "dos"), "es"))
    val contentEnglish = Seq("en")
    val contentEnNb    = Seq("en", "nb")
    val mergedNbEnTags = Seq(Tag(Seq("en", "to"), "nb"), Tag(Seq("one", "two", "three"), "en"))

    migration.mergeTags(oldTags, newTags, contentEnglish) should be(oldTags)
    migration.mergeTags(oldTags, newTags, contentEnNb).sortBy(_.language) should be(mergedNbEnTags.sortBy(_.language))
  }

}
