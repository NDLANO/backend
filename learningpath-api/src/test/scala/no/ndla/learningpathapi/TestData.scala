/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi

import no.ndla.common.model.domain.learningpath.LearningpathCopyright
import no.ndla.language.Language.DefaultLanguage

import java.util.UUID
import no.ndla.common.model.{NDLADate, domain => common}
import no.ndla.mapping.License.CC_BY
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.{LearningPath, LearningStep, SearchSettings, Sort}
import no.ndla.myndla.model.domain.{
  Folder,
  FolderStatus,
  MyNDLAUser,
  NewFolderData,
  Resource,
  ResourceDocument,
  UserRole
}
import no.ndla.myndla.model.{api => myndlaapi}
import no.ndla.myndla.model.domain.config.{BooleanValue, ConfigKey, ConfigMeta}

object TestData {

  val today: NDLADate = NDLADate.now()

  val emptyScopeClientToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6IlNvbWVOZGxhSWQiLCJodHRwczovL25kbGEubm8vdXNlcl9uYW1lIjoiU29tZSBjb29sIHVzZXIiLCJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoiU29tZUNsaWVudElkIiwiaXNzIjoiaHR0cHM6Ly9uZGxhLXRlc3QuZXUuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8U29tZUdvb2dsZU51bWJlciIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTU2MTc0MjQ5LCJleHAiOjE1NTYxNzQ4NDksImF6cCI6IlNvbWVDbGllbnRJZCIsInNjb3BlIjoiIn0=.EAdVD10b_55kPhUx_GtR7ntmEKtPjrRyZ5AnFA6HaXKGgpLu9etHEGJcb54Y9-HnMvqqkzOAr_gIrevcREbeOfd6naKLtb2EMAZIWDaa3cjymHuTo6zBFIQzsuWNmBk9jfIHAhW3sL03KTIbK-kFIjTFt2oBkbBs0caSBXZGjv1xiUuCZ7OSxftT14q2Fq6gXK9uuDmqmEHjGp6vAqd7yC06rfTIT1uH2lrE3nATxZq7QCyLavpEmS1uwDZDH0W5Gla5GtCyEQTDpbL31yxMLwkNOhfU1yTZgRYCf-Ijlc_rhrFR9o2kudelbhhAj8UxHv0QSehBGel22D3e7m4IxQ"

  val writeScopeClientToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6IlNvbWVOZGxhSWQiLCJodHRwczovL25kbGEubm8vdXNlcl9uYW1lIjoiU29tZSBjb29sIHVzZXIiLCJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoiU29tZUNsaWVudElkIiwiaXNzIjoiaHR0cHM6Ly9uZGxhLXRlc3QuZXUuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8U29tZUdvb2dsZU51bWJlciIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTU2MTc0MjQ5LCJleHAiOjE1NTYxNzQ4NDksImF6cCI6IlNvbWVDbGllbnRJZCIsInNjb3BlIjoiIGxlYXJuaW5ncGF0aDp3cml0ZSAifQ==.EAdVD10b_55kPhUx_GtR7ntmEKtPjrRyZ5AnFA6HaXKGgpLu9etHEGJcb54Y9-HnMvqqkzOAr_gIrevcREbeOfd6naKLtb2EMAZIWDaa3cjymHuTo6zBFIQzsuWNmBk9jfIHAhW3sL03KTIbK-kFIjTFt2oBkbBs0caSBXZGjv1xiUuCZ7OSxftT14q2Fq6gXK9uuDmqmEHjGp6vAqd7yC06rfTIT1uH2lrE3nATxZq7QCyLavpEmS1uwDZDH0W5Gla5GtCyEQTDpbL31yxMLwkNOhfU1yTZgRYCf-Ijlc_rhrFR9o2kudelbhhAj8UxHv0QSehBGel22D3e7m4IxQ"

  val adminScopeClientToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6IlNvbWVOZGxhSWQiLCJodHRwczovL25kbGEubm8vdXNlcl9uYW1lIjoiU29tZSBjb29sIHVzZXIiLCJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoiU29tZUNsaWVudElkIiwiaXNzIjoiaHR0cHM6Ly9uZGxhLXRlc3QuZXUuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8U29tZUdvb2dsZU51bWJlciIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTU2MTc0MjQ5LCJleHAiOjE1NTYxNzQ4NDksImF6cCI6IlNvbWVDbGllbnRJZCIsInNjb3BlIjoibGVhcm5pbmdwYXRoOmFkbWluICJ9.EAdVD10b_55kPhUx_GtR7ntmEKtPjrRyZ5AnFA6HaXKGgpLu9etHEGJcb54Y9-HnMvqqkzOAr_gIrevcREbeOfd6naKLtb2EMAZIWDaa3cjymHuTo6zBFIQzsuWNmBk9jfIHAhW3sL03KTIbK-kFIjTFt2oBkbBs0caSBXZGjv1xiUuCZ7OSxftT14q2Fq6gXK9uuDmqmEHjGp6vAqd7yC06rfTIT1uH2lrE3nATxZq7QCyLavpEmS1uwDZDH0W5Gla5GtCyEQTDpbL31yxMLwkNOhfU1yTZgRYCf-Ijlc_rhrFR9o2kudelbhhAj8UxHv0QSehBGel22D3e7m4IxQ"

  val adminAndWriteScopeClientToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6IlNvbWVOZGxhSWQiLCJodHRwczovL25kbGEubm8vdXNlcl9uYW1lIjoiU29tZSBjb29sIHVzZXIiLCJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoiU29tZUNsaWVudElkIiwiaXNzIjoiaHR0cHM6Ly9uZGxhLXRlc3QuZXUuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8U29tZUdvb2dsZU51bWJlciIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTU2MTc0MjQ5LCJleHAiOjE1NTYxNzQ4NDksImF6cCI6IlNvbWVDbGllbnRJZCIsInNjb3BlIjoibGVhcm5pbmdwYXRoOmFkbWluIGxlYXJuaW5ncGF0aDp3cml0ZSAifQ==.EAdVD10b_55kPhUx_GtR7ntmEKtPjrRyZ5AnFA6HaXKGgpLu9etHEGJcb54Y9-HnMvqqkzOAr_gIrevcREbeOfd6naKLtb2EMAZIWDaa3cjymHuTo6zBFIQzsuWNmBk9jfIHAhW3sL03KTIbK-kFIjTFt2oBkbBs0caSBXZGjv1xiUuCZ7OSxftT14q2Fq6gXK9uuDmqmEHjGp6vAqd7yC06rfTIT1uH2lrE3nATxZq7QCyLavpEmS1uwDZDH0W5Gla5GtCyEQTDpbL31yxMLwkNOhfU1yTZgRYCf-Ijlc_rhrFR9o2kudelbhhAj8UxHv0QSehBGel22D3e7m4IxQ"

  val emptyScopeAuthMap: Map[String, String] = Map("Authorization" -> s"Bearer $emptyScopeClientToken")
  val writeScopeAuthMap: Map[String, String] = Map("Authorization" -> s"Bearer $writeScopeClientToken")
  val adminScopeAuthMap: Map[String, String] = Map("Authorization" -> s"Bearer $adminScopeClientToken")

  val testConfigMeta: ConfigMeta = ConfigMeta(
    ConfigKey.LearningpathWriteRestricted,
    value = BooleanValue(true),
    today,
    "EnKulFyr"
  )

  val domainLearningStep1: LearningStep = domain.LearningStep(
    None,
    None,
    None,
    None,
    1,
    List(common.Title("Step1Title", "nb")),
    List(domain.Description("Step1Description", "nb")),
    List(),
    domain.StepType.INTRODUCTION,
    None
  )

  val domainLearningStep2: LearningStep = domain.LearningStep(
    None,
    None,
    None,
    None,
    2,
    List(common.Title("Step2Title", "nb")),
    List(domain.Description("Step2Description", "nb")),
    List(),
    domain.StepType.TEXT,
    None
  )

  val sampleDomainLearningPath: LearningPath = domain.LearningPath(
    Some(1),
    Some(1),
    None,
    None,
    List(common.Title("tittel", DefaultLanguage)),
    List(domain.Description("deskripsjon", DefaultLanguage)),
    None,
    Some(60),
    domain.LearningPathStatus.PUBLISHED,
    domain.LearningPathVerificationStatus.CREATED_BY_NDLA,
    today,
    List(common.Tag(List("tag"), DefaultLanguage)),
    "me",
    LearningpathCopyright(CC_BY.toString, List.empty),
    Some(List(domainLearningStep1, domainLearningStep2))
  )

  val searchSettings: SearchSettings = SearchSettings(
    query = None,
    withIdIn = List.empty,
    withPaths = List.empty,
    taggedWith = None,
    language = Some(DefaultLanguage),
    sort = Sort.ByIdAsc,
    page = None,
    pageSize = None,
    fallback = false,
    verificationStatus = None,
    shouldScroll = false,
    status = List(domain.LearningPathStatus.PUBLISHED)
  )

  val emptyDomainResource: Resource = Resource(
    id = UUID.randomUUID(),
    feideId = "",
    resourceType = "",
    path = "",
    created = NDLADate.now(),
    tags = List.empty,
    resourceId = "1",
    connection = None
  )

  val emptyDomainFolder: Folder = Folder(
    id = UUID.randomUUID(),
    feideId = "",
    parentId = None,
    name = "",
    status = FolderStatus.PRIVATE,
    subfolders = List.empty,
    resources = List.empty,
    rank = None,
    created = today,
    updated = today,
    shared = None,
    description = None
  )

  val baseFolderDocument: NewFolderData = NewFolderData(
    parentId = None,
    name = "some-name",
    status = FolderStatus.PRIVATE,
    rank = None,
    description = None
  )

  val baseResourceDocument: ResourceDocument = ResourceDocument(
    tags = List.empty,
    resourceId = "1"
  )

  val emptyApiFolder: myndlaapi.Folder = myndlaapi.Folder(
    id = "",
    name = "",
    status = "",
    subfolders = List.empty,
    resources = List.empty,
    breadcrumbs = List.empty,
    parentId = None,
    rank = None,
    created = today,
    updated = today,
    shared = None,
    description = None,
    owner = None
  )

  val emptyMyNDLAUser: MyNDLAUser = MyNDLAUser(
    id = 1,
    feideId = "",
    favoriteSubjects = Seq.empty,
    userRole = UserRole.EMPLOYEE,
    lastUpdated = today,
    organization = "",
    groups = Seq.empty,
    username = "",
    email = "",
    arenaEnabled = false,
    displayName = "",
    shareName = false
  )

}
