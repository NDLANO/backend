/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.common.CirceUtil
import no.ndla.common.model.api.{Author, License}
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.api.learningpath.Copyright
import no.ndla.searchapi.model.domain.learningpath.{LearningPathStatus, LearningPathVerificationStatus, StepType}
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.TestData.*
import no.ndla.searchapi.model.domain.LearningResourceType

class SearchableLearningPathTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableLearningPath to json and deserializing back to object does not change content") {
    val titles =
      SearchableLanguageValues(Seq(LanguageValue("nb", "Christian Tut"), LanguageValue("en", "Christian Honk")))

    val descriptions = SearchableLanguageValues(
      Seq(
        LanguageValue("nn", "Eg kjøyrar rundt i min fine bil"),
        LanguageValue("nb", "Jeg kjører rundt i tutut"),
        LanguageValue("en", "I'm in my mums car wroomwroom")
      )
    )

    val tags = SearchableLanguageList(
      Seq(
        LanguageValue("en", Seq("Mum", "Car", "Wroom"))
      )
    )

    val learningsteps = List(
      SearchableLearningStep(stepType = StepType.INTRODUCTION.toString),
      SearchableLearningStep(stepType = StepType.SUMMARY.toString),
      SearchableLearningStep(stepType = StepType.TEXT.toString)
    )

    val original = SearchableLearningPath(
      id = 101,
      title = titles,
      content = SearchableLanguageValues(Seq.empty),
      description = descriptions,
      coverPhotoId = Some("10"),
      duration = Some(10),
      status = LearningPathStatus.PUBLISHED.toString,
      verificationStatus = LearningPathVerificationStatus.CREATED_BY_NDLA.toString,
      lastUpdated = TestData.today,
      defaultTitle = Some("Christian Tut"),
      tags = tags,
      learningsteps = learningsteps,
      copyright = Copyright(
        License("by-sa", Some("bysasaa"), None),
        Seq(Author("Supplier", "Jonas"), Author("Originator", "Kakemonsteret"))
      ),
      isBasedOn = Some(1001),
      supportedLanguages = List("nb", "en", "nn"),
      authors = List("Yap"),
      contexts = searchableTaxonomyContexts,
      license = "by-sa",
      favorited = 0,
      learningResourceType = LearningResourceType.LearningPath
    )

    val json         = CirceUtil.toJsonString(original)
    val deserialized = CirceUtil.unsafeParseAs[SearchableLearningPath](json)

    deserialized should be(original)
  }
}
