/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.api.learningpath.{Author, Copyright, License}
import no.ndla.searchapi.model.domain.learningpath.{LearningPathStatus, LearningPathVerificationStatus, StepType}
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.TestData._
import org.json4s.Formats
import org.json4s.native.Serialization.{read, write}

class SearchableLearningPathTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableLearningPath to json and deserializing back to object does not change content") {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    val titles =
      SearchableLanguageValues(Seq(LanguageValue("nb", "Christian Tut"), LanguageValue("en", "Christian Honk")))

    val descriptions = SearchableLanguageValues(
      Seq(
        LanguageValue("nn", "Eg kjøyrar rundt i min fine bil"),
        LanguageValue("nb", "Jeg kjører rundt i tutut"),
        LanguageValue("en", "I'm in my mums car wroomwroom")
      ))

    val tags = SearchableLanguageList(
      Seq(
        LanguageValue("en", Seq("Mum", "Car", "Wroom"))
      ))

    val learningsteps = List(
      SearchableLearningStep(
        stepType = StepType.INTRODUCTION.toString,
        title = SearchableLanguageValues(Seq(LanguageValue("nb", "Introsteg"))),
        description = SearchableLanguageValues(Seq(LanguageValue("nb", "IntroDesc")))
      ),
      SearchableLearningStep(
        stepType = StepType.SUMMARY.toString,
        title = SearchableLanguageValues(Seq(LanguageValue("nb", "Summary a gitt"))),
        description =
          SearchableLanguageValues(Seq(LanguageValue("nb", "Summariereire"), LanguageValue("en", "Summingz")))
      ),
      SearchableLearningStep(
        stepType = StepType.TEXT.toString,
        title = SearchableLanguageValues(Seq(LanguageValue("en", "TEXTITexTy"))),
        description = SearchableLanguageValues(Seq(LanguageValue("en", "TextarUøø")))
      )
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
      copyright = Copyright(License("by-sa", Some("bysasaa"), None),
                            Seq(Author("Supplier", "Jonas"), Author("Originator", "Kakemonsteret"))),
      isBasedOn = Some(1001),
      supportedLanguages = List("nb", "en", "nn"),
      authors = List("Yap"),
      contexts = searchableTaxonomyContexts,
      embedResourcesAndIds = List.empty,
      license = "by-sa"
    )

    val json = write(original)
    val deserialized = read[SearchableLearningPath](json)

    deserialized should be(original)
  }
}
