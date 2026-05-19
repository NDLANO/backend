/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import no.ndla.common.model.domain.{RevisionMeta, RevisionStatus, learningpath}
import no.ndla.common.model.domain.learningpath.{
  Description,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningStep,
  LearningpathCopyright,
  StepType,
}
import no.ndla.language.Language.DefaultLanguage
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.mapping.License.CC_BY
import no.ndla.learningpathapi.model.domain.{SearchSettings, Sort}
import common.learningpath.Introduction

import java.util.UUID

object TestData {

  val today: NDLADate = NDLADate.now().withNano(0)

  val domainLearningStep1: LearningStep = LearningStep(
    id = None,
    revision = None,
    externalId = None,
    learningPathId = None,
    seqNo = 1,
    title = List(common.Title("Step1Title", "nb")),
    introduction = List.empty,
    description = List(Description("Step1Description", "nb")),
    embedUrl = List(),
    articleId = None,
    `type` = StepType.TEXT,
    copyright = None,
    created = today,
    lastUpdated = today,
    owner = "me",
  )
  val domainLearningStep2: LearningStep = LearningStep(
    id = None,
    revision = None,
    externalId = None,
    learningPathId = None,
    seqNo = 2,
    title = List(common.Title("Step2Title", "nb")),
    introduction = List.empty,
    description = List(Description("Step2Description", "nb")),
    embedUrl = List(),
    articleId = None,
    `type` = learningpath.StepType.TEXT,
    copyright = None,
    created = today,
    lastUpdated = today,
    owner = "me",
  )

  val revisionMetaSeq = Seq(
    RevisionMeta(id = UUID.randomUUID(), today.plusYears(5), RevisionMeta.defaultNote, RevisionStatus.NeedsRevision)
  )

  val sampleDomainLearningPath: LearningPath = LearningPath(
    id = Some(1),
    revision = Some(1),
    externalId = None,
    isBasedOn = None,
    title = List(common.Title("tittel", DefaultLanguage)),
    description = List(Description("deskripsjon", DefaultLanguage)),
    introduction = List(Introduction("<section><p>introduction</p></section>", DefaultLanguage)),
    coverPhotoId = None,
    duration = Some(60),
    status = LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.CREATED_BY_NDLA,
    created = today,
    lastUpdated = today,
    tags = List(common.Tag(List("tag"), DefaultLanguage)),
    owner = "me",
    copyright = LearningpathCopyright(CC_BY.toString, List.empty),
    isMyNDLAOwner = false,
    learningsteps = Seq(domainLearningStep1, domainLearningStep2),
    responsible = None,
    comments = Seq.empty,
    priority = common.Priority.Unspecified,
    revisionMeta = revisionMetaSeq,
    grepCodes = Seq.empty,
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
    articleId = None,
    status = List(learningpath.LearningPathStatus.PUBLISHED),
    grepCodes = List.empty,
  )
}
