/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi

import no.ndla.common.model.domain.learningpath
import no.ndla.common.model.domain.learningpath.{
  Description,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningStep,
  LearningpathCopyright,
  StepType
}
import no.ndla.language.Language.DefaultLanguage
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.mapping.License.CC_BY
import no.ndla.learningpathapi.model.domain.{SearchSettings, Sort}

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

  val domainLearningStep1: LearningStep = LearningStep(
    None,
    None,
    None,
    None,
    1,
    List(common.Title("Step1Title", "nb")),
    List.empty,
    List(Description("Step1Description", "nb")),
    List(),
    StepType.INTRODUCTION,
    None
  )

  val domainLearningStep2: LearningStep = LearningStep(
    None,
    None,
    None,
    None,
    2,
    List(common.Title("Step2Title", "nb")),
    List.empty,
    List(Description("Step2Description", "nb")),
    List(),
    learningpath.StepType.TEXT,
    None
  )

  val sampleDomainLearningPath: LearningPath = LearningPath(
    Some(1),
    Some(1),
    None,
    None,
    List(common.Title("tittel", DefaultLanguage)),
    List(Description("deskripsjon", DefaultLanguage)),
    None,
    Some(60),
    LearningPathStatus.PUBLISHED,
    LearningPathVerificationStatus.CREATED_BY_NDLA,
    today,
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
    status = List(learningpath.LearningPathStatus.PUBLISHED)
  )
}
