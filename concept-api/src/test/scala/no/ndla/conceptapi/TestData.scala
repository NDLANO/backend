/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.domain as common
import no.ndla.conceptapi.model.api
import no.ndla.network.tapir.auth.Permission.{CONCEPT_API_ADMIN, CONCEPT_API_WRITE}
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.Missing
import no.ndla.common.model.domain.concept
import no.ndla.common.model.domain.concept.{
  Concept,
  ConceptContent,
  ConceptMetaImage,
  ConceptType,
  Status,
  VisualElement
}

object TestData {

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJjb25jZXB0OndyaXRlIl0sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.RAP-ab3l9qPOpYQRreqLi_RRmgybk-G_VKRPHIOqq5A"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6W10sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.vw9YhRtgUQr_vuDhLNHfBsZz-4XLhCc1Kwxi0w0_qGI"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJkcmFmdDp3cml0ZSJdLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.xLCyYI3le-GbQ9Y10n5Lj-8BwUIMYeaMJi2D2CwUNhU"

  val authHeaderWithAllRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJjb25jZXB0OndyaXRlIiwiY29uY2VwdDphZG1pbiJdLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.RYX5urIY8B5dRxMUgesTLbABjRZ5k-Jc3KMDLz99UnU"

  val userWithNoRoles: TokenUser               = TokenUser("unit test", Set.empty, None)
  val userWithWriteAccess: TokenUser           = TokenUser("unit test", Set(CONCEPT_API_WRITE), None)
  val userWithWriteAndPublishAccess: TokenUser = TokenUser("unit test", Set(CONCEPT_API_ADMIN, CONCEPT_API_WRITE), None)

  val today: NDLADate     = NDLADate.now().minusDays(0)
  val yesterday: NDLADate = NDLADate.now().minusDays(1)

  val visualElementString: String =
    s"""<$EmbedTagName data-caption="some capt" data-align="" data-resource_id="1" data-resource="image" data-alt="some alt" data-size="full" />"""

  val visualElementStringWithUrl: String =
    s"""<$EmbedTagName data-caption="some capt" data-align="" data-resource_id="1" data-resource="image" data-alt="some alt" data-size="full" data-url="http://api-gateway.ndla-local/image-api/v2/images/1" />"""

  val sampleNbApiConcept: api.ConceptDTO = api.ConceptDTO(
    1.toLong,
    1,
    api.ConceptTitleDTO("Tittel", "nb"),
    Some(api.ConceptContent("Innhold", "Innhold", "nb")),
    None,
    None,
    Some(api.ConceptMetaImageDTO("http://api-gateway.ndla-local/image-api/raw/id/1", "Hei", "nb")),
    Some(api.ConceptTagsDTO(Seq("stor", "kaktus"), "nb")),
    Some(Set("urn:subject:3", "urn:subject:4")),
    yesterday,
    today,
    Some(Seq("")),
    Set("nn", "nb"),
    Seq(42),
    api.StatusDTO(
      current = "IN_PROGRESS",
      other = Seq.empty
    ),
    Some(api.VisualElementDTO(visualElementStringWithUrl, "nb")),
    responsible = None,
    conceptType = "concept",
    glossData = None,
    editorNotes = Some(Seq.empty)
  )

  val sampleNbDomainConcept: Concept = Concept(
    id = Some(1),
    revision = Some(1),
    title = Seq(common.Title("Tittel", "nb")),
    content = Seq(ConceptContent("Innhold", "nb")),
    copyright = None,
    created = yesterday,
    updated = today,
    updatedBy = Seq.empty,
    metaImage = Seq(ConceptMetaImage("1", "Hei", "nb")),
    tags = Seq(common.Tag(Seq("stor", "kaktus"), "nb")),
    subjectIds = Set("urn:subject:3", "urn:subject:4"),
    articleIds = Seq(42),
    status = Status.default,
    visualElement = Seq(VisualElement(visualElementString, "nb")),
    responsible = None,
    conceptType = ConceptType.CONCEPT,
    glossData = None,
    editorNotes = Seq.empty
  )

  val sampleConcept: Concept = Concept(
    id = Some(1),
    revision = Some(1),
    title = Seq(common.Title("Tittel for begrep", "nb")),
    content = Seq(ConceptContent("Innhold for begrep", "nb")),
    copyright = Some(
      common.draft.DraftCopyright(Some("publicdomain"), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, false)
    ),
    created = NDLADate.now().minusDays(4),
    updated = NDLADate.now().minusDays(2),
    updatedBy = Seq.empty,
    metaImage = Seq(concept.ConceptMetaImage("1", "Hei", "nb")),
    tags = Seq(common.Tag(Seq("liten", "fisk"), "nb")),
    subjectIds = Set("urn:subject:3", "urn:subject:4"),
    articleIds = Seq(42),
    status = Status.default,
    visualElement = Seq(concept.VisualElement("VisualElement for begrep", "nb")),
    responsible = None,
    conceptType = concept.ConceptType.CONCEPT,
    glossData = None,
    editorNotes = Seq.empty
  )

  val domainConcept: Concept = Concept(
    id = Some(1),
    revision = Some(1),
    title = Seq(common.Title("Tittel", "nb"), common.Title("Tittelur", "nn")),
    content = Seq(concept.ConceptContent("Innhold", "nb"), concept.ConceptContent("Innhald", "nn")),
    copyright = None,
    created = yesterday,
    updated = today,
    updatedBy = Seq(""),
    metaImage = Seq(concept.ConceptMetaImage("1", "Hei", "nb"), concept.ConceptMetaImage("2", "Hej", "nn")),
    tags = Seq(common.Tag(Seq("stor", "kaktus"), "nb"), common.Tag(Seq("liten", "fisk"), "nn")),
    subjectIds = Set("urn:subject:3", "urn:subject:4"),
    articleIds = Seq(42),
    status = Status.default,
    visualElement = Seq(concept.VisualElement(visualElementString, "nb")),
    responsible = None,
    conceptType = concept.ConceptType.CONCEPT,
    glossData = None,
    editorNotes = Seq.empty
  )

  val domainConcept_toDomainUpdateWithId: Concept = Concept(
    id = None,
    revision = None,
    title = Seq.empty,
    content = Seq.empty,
    copyright = None,
    created = today,
    updated = today,
    updatedBy = Seq(""),
    metaImage = Seq.empty,
    tags = Seq.empty,
    subjectIds = Set.empty,
    articleIds = Seq.empty,
    status = Status.default,
    visualElement = Seq.empty,
    responsible = None,
    conceptType = concept.ConceptType.CONCEPT,
    glossData = None,
    editorNotes = Seq.empty
  )

  val sampleNnApiConcept: api.ConceptDTO = api.ConceptDTO(
    1.toLong,
    1,
    api.ConceptTitleDTO("Tittelur", "nn"),
    Some(api.ConceptContent("Innhald", "Innhald", "nn")),
    None,
    None,
    Some(api.ConceptMetaImageDTO("http://api-gateway.ndla-local/image-api/raw/id/2", "Hej", "nn")),
    Some(api.ConceptTagsDTO(Seq("liten", "fisk"), "nn")),
    Some(Set("urn:subject:3", "urn:subject:4")),
    yesterday,
    today,
    updatedBy = Some(Seq("")),
    Set("nn", "nb"),
    Seq(42),
    api.StatusDTO(
      current = "IN_PROGRESS",
      other = Seq.empty
    ),
    Some(api.VisualElementDTO(visualElementStringWithUrl, "nb")),
    responsible = None,
    conceptType = "concept",
    glossData = None,
    editorNotes = Some(Seq.empty)
  )

  val emptyApiUpdatedConcept: api.UpdatedConceptDTO = api.UpdatedConceptDTO(
    language = "",
    title = None,
    content = None,
    metaImage = Missing,
    copyright = None,
    tags = None,
    subjectIds = None,
    articleIds = None,
    status = None,
    visualElement = None,
    Missing,
    conceptType = None,
    glossData = None
  )

  val sampleNewConcept: api.NewConceptDTO =
    api.NewConceptDTO(
      "nb",
      "Tittel",
      Some("Innhold"),
      None,
      None,
      None,
      None,
      Some(Seq(42)),
      None,
      None,
      "concept",
      None
    )

  val emptyApiNewConcept: api.NewConceptDTO = api.NewConceptDTO(
    language = "",
    title = "",
    content = None,
    copyright = None,
    metaImage = None,
    tags = None,
    subjectIds = None,
    articleIds = None,
    visualElement = None,
    responsibleId = None,
    conceptType = "concept",
    glossData = None
  )

  val updatedConcept: api.UpdatedConceptDTO =
    api.UpdatedConceptDTO(
      "nb",
      None,
      Some("Innhold"),
      Missing,
      None,
      None,
      None,
      Some(Seq(12L)),
      None,
      None,
      Missing,
      conceptType = None,
      glossData = None
    )
  val sampleApiTagsSearchResult: api.TagsSearchResultDTO = api.TagsSearchResultDTO(10, 1, 1, "nb", Seq("a", "b"))
}
