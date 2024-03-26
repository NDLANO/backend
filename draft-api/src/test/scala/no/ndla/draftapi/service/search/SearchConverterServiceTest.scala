/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service.search

import no.ndla.common.model.domain.{EditorNote, Status}
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}

class SearchConverterServiceTest extends UnitSuite with TestEnvironment {
  val service = new SearchConverterService

  test("That converting to searchable article creates list of users") {
    val article = TestData.sampleDomainArticle.copy(
      notes = Seq(EditorNote("Note", "user", Status(DraftStatus.PLANNED, Set.empty), TestData.today))
    )
    val converted = service.asSearchableArticle(article)
    converted.notes should be(Seq("Note"))
    converted.users should be(Seq("ndalId54321", "user"))
  }
}
