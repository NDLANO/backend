/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import no.ndla.common.CirceUtil
import no.ndla.common.model.api.search.LearningResourceType
import no.ndla.common.model.api.{AuthorDTO, LicenseDTO}
import no.ndla.common.model.domain.{ContributorType, Priority, Responsible, RevisionMeta, getNextRevision}
import no.ndla.common.model.domain.learningpath.{LearningPathStatus, LearningPathVerificationStatus, StepType}
import no.ndla.mapping.License
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.api.learningpath.CopyrightDTO
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.TestData.*

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

    val introductions = SearchableLanguageValues(
      Seq(
        LanguageValue("nb", "<section><p>Dette er en introduksjon</p></section>"),
        LanguageValue("nn", "<section><p>Dette er ein introduksjon</p></section>"),
        LanguageValue("en", "<section><p>This is an introduction</p></section>")
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
      introduction = introductions,
      description = descriptions,
      coverPhotoId = Some("10"),
      duration = Some(10),
      status = LearningPathStatus.PUBLISHED.toString,
      owner = "xxxyyy",
      verificationStatus = LearningPathVerificationStatus.CREATED_BY_NDLA.toString,
      lastUpdated = TestData.today,
      defaultTitle = Some("Christian Tut"),
      tags = tags,
      learningsteps = learningsteps,
      license = License.CC_BY_SA.toString,
      copyright = CopyrightDTO(
        LicenseDTO(License.CC_BY_SA.toString, Some("bysasaa"), None),
        Seq(AuthorDTO(ContributorType.Supplier, "Jonas"), AuthorDTO(ContributorType.Originator, "Kakemonsteret"))
      ),
      isBasedOn = Some(1001),
      supportedLanguages = List("nb", "en", "nn"),
      authors = List("Yap"),
      context = searchableTaxonomyContexts.headOption,
      contexts = searchableTaxonomyContexts,
      contextids = searchableTaxonomyContexts.map(_.contextId),
      favorited = 0,
      learningResourceType = LearningResourceType.LearningPath,
      typeName = List.empty,
      priority = Priority.Unspecified,
      revisionMeta = RevisionMeta.default.toList,
      nextRevision = RevisionMeta.default.getNextRevision,
      responsible = Some(Responsible("some responsible", TestData.today))
    )

    val json         = CirceUtil.toJsonString(original)
    val deserialized = CirceUtil.unsafeParseAs[SearchableLearningPath](json)

    deserialized should be(original)
  }
}
