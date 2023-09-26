/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.{domain => common}
import no.ndla.conceptapi.model.{api, domain}
import no.ndla.conceptapi.model.domain.{ConceptContent, Status}
import no.ndla.network.tapir.auth.Permission.{CONCEPT_API_ADMIN, CONCEPT_API_WRITE}
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.common.model.NDLADate

object TestData {

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJjb25jZXB0OndyaXRlIl0sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.RAP-ab3l9qPOpYQRreqLi_RRmgybk-G_VKRPHIOqq5A"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6W10sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.vw9YhRtgUQr_vuDhLNHfBsZz-4XLhCc1Kwxi0w0_qGI"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJkcmFmdDp3cml0ZSJdLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.xLCyYI3le-GbQ9Y10n5Lj-8BwUIMYeaMJi2D2CwUNhU"

  val authHeaderWithAllRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJjb25jZXB0OndyaXRlIiwiY29uY2VwdDphZG1pbiJdLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.RYX5urIY8B5dRxMUgesTLbABjRZ5k-Jc3KMDLz99UnU"

  val userWithNoRoles               = TokenUser("unit test", Set.empty, None)
  val userWithWriteAccess           = TokenUser("unit test", Set(CONCEPT_API_WRITE), None)
  val userWithWriteAndPublishAccess = TokenUser("unit test", Set(CONCEPT_API_ADMIN, CONCEPT_API_WRITE), None)

  val today     = NDLADate.now().minusDays(0)
  val yesterday = NDLADate.now().minusDays(1)

  val visualElementString =
    s"""<$EmbedTagName data-caption="some capt" data-align="" data-resource_id="1" data-resource="image" data-alt="some alt" data-size="full" />"""

  val visualElementStringWithUrl =
    s"""<$EmbedTagName data-caption="some capt" data-align="" data-resource_id="1" data-resource="image" data-alt="some alt" data-size="full" data-url="http://api-gateway.ndla-local/image-api/v2/images/1" />"""

  val sampleNbApiConcept = api.Concept(
    1.toLong,
    1,
    api.ConceptTitle("Tittel", "nb"),
    Some(api.ConceptContent("Innhold", "nb")),
    None,
    None,
    Some(api.ConceptMetaImage("http://api-gateway.ndla-local/image-api/raw/id/1", "Hei", "nb")),
    Some(api.ConceptTags(Seq("stor", "kaktus"), "nb")),
    Some(Set("urn:subject:3", "urn:subject:4")),
    yesterday,
    today,
    Some(Seq("")),
    Set("nn", "nb"),
    Seq(42),
    api.Status(
      current = "IN_PROGRESS",
      other = Seq.empty
    ),
    Some(api.VisualElement(visualElementStringWithUrl, "nb")),
    responsible = None,
    conceptType = "concept",
    glossData = None
  )

  val sampleNbDomainConcept = domain.Concept(
    id = Some(1),
    revision = Some(1),
    title = Seq(common.Title("Tittel", "nb")),
    content = Seq(domain.ConceptContent("Innhold", "nb")),
    copyright = None,
    created = yesterday,
    updated = today,
    updatedBy = Seq.empty,
    metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb")),
    tags = Seq(common.Tag(Seq("stor", "kaktus"), "nb")),
    subjectIds = Set("urn:subject:3", "urn:subject:4"),
    articleIds = Seq(42),
    status = Status.default,
    visualElement = Seq(domain.VisualElement(visualElementString, "nb")),
    responsible = None,
    conceptType = domain.ConceptType.CONCEPT,
    glossData = None
  )

  val sampleConcept = domain.Concept(
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
    metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb")),
    tags = Seq(common.Tag(Seq("liten", "fisk"), "nb")),
    subjectIds = Set("urn:subject:3", "urn:subject:4"),
    articleIds = Seq(42),
    status = Status.default,
    visualElement = Seq(domain.VisualElement("VisualElement for begrep", "nb")),
    responsible = None,
    conceptType = domain.ConceptType.CONCEPT,
    glossData = None
  )

  val domainConcept = domain.Concept(
    id = Some(1),
    revision = Some(1),
    title = Seq(common.Title("Tittel", "nb"), common.Title("Tittelur", "nn")),
    content = Seq(domain.ConceptContent("Innhold", "nb"), domain.ConceptContent("Innhald", "nn")),
    copyright = None,
    created = yesterday,
    updated = today,
    updatedBy = Seq(""),
    metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb"), domain.ConceptMetaImage("2", "Hej", "nn")),
    tags = Seq(common.Tag(Seq("stor", "kaktus"), "nb"), common.Tag(Seq("liten", "fisk"), "nn")),
    subjectIds = Set("urn:subject:3", "urn:subject:4"),
    articleIds = Seq(42),
    status = Status.default,
    visualElement = Seq(domain.VisualElement(visualElementString, "nb")),
    responsible = None,
    conceptType = domain.ConceptType.CONCEPT,
    glossData = None
  )

  val domainConcept_toDomainUpdateWithId = domain.Concept(
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
    conceptType = domain.ConceptType.CONCEPT,
    glossData = None
  )

  val sampleNnApiConcept = api.Concept(
    1.toLong,
    1,
    api.ConceptTitle("Tittelur", "nn"),
    Some(api.ConceptContent("Innhald", "nn")),
    None,
    None,
    Some(api.ConceptMetaImage("http://api-gateway.ndla-local/image-api/raw/id/2", "Hej", "nn")),
    Some(api.ConceptTags(Seq("liten", "fisk"), "nn")),
    Some(Set("urn:subject:3", "urn:subject:4")),
    yesterday,
    today,
    updatedBy = Some(Seq("")),
    Set("nn", "nb"),
    Seq(42),
    api.Status(
      current = "IN_PROGRESS",
      other = Seq.empty
    ),
    Some(api.VisualElement(visualElementStringWithUrl, "nb")),
    responsible = None,
    conceptType = "concept",
    glossData = None
  )

  val emptyApiUpdatedConcept = api.UpdatedConcept(
    language = "",
    title = None,
    content = None,
    metaImage = Right(None),
    copyright = None,
    tags = None,
    subjectIds = None,
    articleIds = None,
    status = None,
    visualElement = None,
    Right(None),
    conceptType = None,
    glossData = None
  )

  val sampleNewConcept =
    api.NewConcept(
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

  val emptyApiNewConcept = api.NewConcept(
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

  val updatedConcept =
    api.UpdatedConcept(
      "nb",
      None,
      Some("Innhold"),
      Right(None),
      None,
      None,
      None,
      Some(Seq(12L)),
      None,
      None,
      Right(None),
      conceptType = None,
      glossData = None
    )
  val sampleApiTagsSearchResult = api.TagsSearchResult(10, 1, 1, "nb", Seq("a", "b"))
}
