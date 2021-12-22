/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import no.ndla.conceptapi.auth.{Role, UserInfo}
import no.ndla.conceptapi.model.{api, domain}
import no.ndla.conceptapi.model.domain.{ConceptContent, ConceptTitle, Copyright, Status}
import org.joda.time.DateTime

object TestData {

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiZHJhZnRzLXRlc3Q6d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.i_wvbN24VZMqOTQPiEqvqKZy23-m-2ZxTligof8n33k3z-BjXqn4bhKTv7sFdQG9Wf9TFx8UzjoOQ6efQgpbRzl8blZ-6jAZOy6xDjDW0dIwE0zWD8riG8l27iQ88fbY_uCyIODyYp2JNbVmWZNJ9crKKevKmhcXvMRUTrcyE9g"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.fb9eTuBwIlbGDgDKBQ5FVpuSUdgDVBZjCenkOrWLzUByVCcaFhbFU8CVTWWKhKJqt6u-09-99hh86szURLqwl3F5rxSX9PrnbyhI9LsPut_3fr6vezs6592jPJRbdBz3-xLN0XY5HIiJElJD3Wb52obTqJCrMAKLZ5x_GLKGhcY"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoic29tZTpvdGhlciIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.Hbmh9KX19nx7yT3rEcP9pyzRO0uQJBRucfqH9QEZtLyXjYj_fAyOhsoicOVEbHSES7rtdiJK43-gijSpWWmGWOkE6Ym7nHGhB_nLdvp_25PDgdKHo-KawZdAyIcJFr5_t3CJ2Z2IPVbrXwUd99vuXEBaV0dMwkT0kDtkwHuS-8E"

  val authHeaderWithAllRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXJ0aWNsZXMtdGVzdDpwdWJsaXNoIGRyYWZ0cy10ZXN0OndyaXRlIGRyYWZ0cy10ZXN0OnNldF90b19wdWJsaXNoIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"

  val userWithNoRoles = UserInfo("unit test", Set.empty)
  val userWithWriteAccess = UserInfo("unit test", Set(Role.WRITE))

  val today = DateTime.now().minusDays(0).toDate
  val yesterday = DateTime.now().minusDays(1).toDate

  val sampleNbApiConcept = api.Concept(
    1.toLong,
    1,
    Some(api.ConceptTitle("Tittel", "nb")),
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
      current = "DRAFT",
      other = Seq.empty
    ),
    Some(api.VisualElement("VisueltElement", "nb"))
  )

  val sampleNbDomainConcept = domain.Concept(
    id = Some(1),
    revision = Some(1),
    title = Seq(domain.ConceptTitle("Tittel", "nb")),
    content = Seq(domain.ConceptContent("Innhold", "nb")),
    copyright = None,
    source = None,
    created = yesterday,
    updated = today,
    updatedBy = Seq.empty,
    metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb")),
    tags = Seq(domain.ConceptTags(Seq("stor", "kaktus"), "nb")),
    subjectIds = Set("urn:subject:3", "urn:subject:4"),
    articleIds = Seq(42),
    status = Status.default,
    visualElement = Seq(domain.VisualElement("VisueltElement", "nb"))
  )

  val sampleConcept = domain.Concept(
    id = Some(1),
    revision = Some(1),
    title = Seq(ConceptTitle("Tittel for begrep", "nb")),
    content = Seq(ConceptContent("Innhold for begrep", "nb")),
    copyright = Some(Copyright(Some("publicdomain"), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, None)),
    source = None,
    created = DateTime.now().minusDays(4).toDate,
    updated = DateTime.now().minusDays(2).toDate,
    updatedBy = Seq.empty,
    metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb")),
    tags = Seq(domain.ConceptTags(Seq("liten", "fisk"), "nb")),
    subjectIds = Set("urn:subject:3", "urn:subject:4"),
    articleIds = Seq(42),
    status = Status.default,
    visualElement = Seq(domain.VisualElement("VisualElement for begrep", "nb"))
  )

  val domainConcept = domain.Concept(
    id = Some(1),
    revision = Some(1),
    title = Seq(domain.ConceptTitle("Tittel", "nb"), domain.ConceptTitle("Tittelur", "nn")),
    content = Seq(domain.ConceptContent("Innhold", "nb"), domain.ConceptContent("Innhald", "nn")),
    copyright = None,
    source = None,
    created = yesterday,
    updated = today,
    updatedBy = Seq(""),
    metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb"), domain.ConceptMetaImage("2", "Hej", "nn")),
    tags = Seq(domain.ConceptTags(Seq("stor", "kaktus"), "nb"), domain.ConceptTags(Seq("liten", "fisk"), "nn")),
    subjectIds = Set("urn:subject:3", "urn:subject:4"),
    articleIds = Seq(42),
    status = Status.default,
    visualElement = Seq(domain.VisualElement("VisueltElement", "nb"))
  )

  val domainConcept_toDomainUpdateWithId = domain.Concept(
    id = None,
    revision = None,
    title = Seq.empty,
    content = Seq.empty,
    copyright = None,
    source = None,
    created = today,
    updated = today,
    updatedBy = Seq(""),
    metaImage = Seq.empty,
    tags = Seq.empty,
    subjectIds = Set.empty,
    articleIds = Seq.empty,
    status = Status.default,
    visualElement = Seq.empty
  )

  val sampleNnApiConcept = api.Concept(
    1.toLong,
    1,
    Some(api.ConceptTitle("Tittelur", "nn")),
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
      current = "DRAFT",
      other = Seq.empty
    ),
    Some(api.VisualElement("VisueltElement", "nb"))
  )

  val emptyApiUpdatedConcept = api.UpdatedConcept(
    language = "",
    title = None,
    content = None,
    metaImage = Right(None),
    copyright = None,
    source = None,
    tags = None,
    subjectIds = None,
    articleIds = None,
    status = None,
    visualElement = None
  )

  val sampleNewConcept =
    api.NewConcept("nb", "Tittel", Some("Innhold"), None, None, None, None, None, Some(Seq(42)), None)

  val emptyApiNewConcept = api.NewConcept(
    language = "",
    title = "",
    content = None,
    copyright = None,
    source = None,
    metaImage = None,
    tags = None,
    subjectIds = None,
    articleIds = None,
    visualElement = None
  )

  val updatedConcept =
    api.UpdatedConcept("nb", None, Some("Innhold"), Right(None), None, None, None, None, Some(Seq(12L)), None, None)
  val sampleApiTagsSearchResult = api.TagsSearchResult(10, 1, 1, "nb", Seq("a", "b"))
}
