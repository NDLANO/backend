/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.{
  LanguageValue,
  LearningResourceType,
  SearchableLanguageList,
  SearchableLanguageValues,
}
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.common.model.domain.{
  EditorNote,
  Priority,
  Responsible,
  Status as CommonStatus,
  RevisionMeta,
  RevisionStatus,
}
import no.ndla.mapping.License
import no.ndla.search.model.domain.EmbedValues
import no.ndla.searchapi.TestData.*
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}

import java.util.UUID

class SearchableDraftTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableDraft to json and deserializing back to object does not change content") {
    val titles =
      SearchableLanguageValues(Seq(LanguageValue("nb", "Christian Tut"), LanguageValue("en", "Christian Honk")))

    val contents = SearchableLanguageValues(
      Seq(
        LanguageValue("nn", "Eg kjøyrar rundt i min fine bil"),
        LanguageValue("nb", "Jeg kjører rundt i tutut"),
        LanguageValue("en", "I'm in my mums car wroomwroom"),
      )
    )

    val introductions = SearchableLanguageValues(Seq(LanguageValue("en", "Wroom wroom")))

    val metaDescriptions = SearchableLanguageValues(Seq(LanguageValue("nb", "Mammas bil")))

    val tags = SearchableLanguageList(Seq(LanguageValue("en", Seq("Mum", "Car", "Wroom"))))

    val embedAttrs = SearchableLanguageList(
      Seq(LanguageValue("nb", Seq("En norsk", "To norsk")), LanguageValue("en", Seq("One english")))
    )

    val embedResourcesAndIds =
      List(EmbedValues(resource = Some("test resource 1"), id = List("test id 1"), language = "nb"))

    val today   = NDLADate.now().withNano(0)
    val olddate = today.minusDays(5)

    val revisionMeta = List(
      RevisionMeta(
        id = UUID.randomUUID(),
        revisionDate = today,
        note = "some note",
        status = RevisionStatus.NeedsRevision,
      ),
      RevisionMeta(
        id = UUID.randomUUID(),
        revisionDate = olddate,
        note = "some other note",
        status = RevisionStatus.NeedsRevision,
      ),
    )
    val original = SearchableDraft(
      id = 100,
      title = titles,
      content = contents,
      introduction = introductions,
      metaDescription = metaDescriptions,
      tags = tags,
      lastUpdated = TestData.today,
      license = Some(License.CC_BY_SA.toString),
      authors = List("Jonas", "Papi"),
      articleType = LearningResourceType.Article.toString,
      defaultTitle = Some("Christian Tut"),
      supportedLanguages = List("en", "nb", "nn"),
      notes = List("Note1", "note2"),
      nodeIds = searchableTaxonomyContexts.map(_.publicId),
      context = searchableTaxonomyContexts.headOption,
      contexts = searchableTaxonomyContexts,
      contextids = searchableTaxonomyContexts.map(_.contextId),
      draftStatus = SearchableStatus(DraftStatus.PLANNED.toString, Seq(DraftStatus.IN_PROGRESS.toString)),
      status = DraftStatus.PLANNED.toString,
      users = List("ndalId54321", "ndalId12345"),
      previousVersionsNotes = List("OldNote"),
      grepContexts = List(
        SearchableGrepContext("K123", Some("some title"), "Published"),
        SearchableGrepContext("K456", Some("some title 2"), "Published"),
      ),
      traits = List.empty,
      embedAttributes = embedAttrs,
      embedResourcesAndIds = embedResourcesAndIds,
      revisionMeta = revisionMeta,
      nextRevision = revisionMeta.lastOption,
      responsible = Some(Responsible("some responsible", TestData.today)),
      domainObject = TestData
        .draft1
        .copy(
          status = CommonStatus(DraftStatus.IN_PROGRESS, Set(DraftStatus.PUBLISHED)),
          notes = Seq(
            EditorNote(
              note = "Hei",
              user = "user",
              timestamp = TestData.today,
              status = CommonStatus(current = DraftStatus.IN_PROGRESS, other = Set(DraftStatus.PUBLISHED)),
            )
          ),
        ),
      priority = Priority.Unspecified,
      defaultParentTopicName = titles.defaultValue,
      parentTopicName = titles,
      defaultRoot = titles.defaultValue,
      primaryRoot = titles,
      resourceTypeName = titles,
      defaultResourceTypeName = titles.defaultValue,
      published = TestData.today,
      favorited = 0,
      learningResourceType = LearningResourceType.Article,
      typeName = List.empty,
    )

    val json         = CirceUtil.toJsonString(original)
    val deserialized = CirceUtil.unsafeParseAs[SearchableDraft](json)

    deserialized should be(original)
  }
}
