/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import cats.implicits.*
import io.lemonlabs.uri.typesafe.dsl.*
import no.ndla.common.converter.CommonConverter
import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.common.implicits.*
import no.ndla.common.model.api.{Delete, Missing, ResponsibleDTO, UpdateOrDelete, UpdateWith}
import no.ndla.common.model.domain.{ContributorType, Responsible, RevisionMeta, learningpath}
import no.ndla.common.model.domain.learningpath.{
  Description,
  EmbedType,
  EmbedUrl,
  Introduction,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningStep,
  LearningpathCopyright,
  StepStatus,
  StepType
}
import no.ndla.common.model.{api as commonApi, domain as common}
import no.ndla.common.{Clock, errors}
import no.ndla.language.Language.*
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.integration.*
import no.ndla.learningpathapi.model.api.{LearningPathStatusDTO as _, *}
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathCombinedUser
import no.ndla.learningpathapi.model.domain.ImplicitLearningPath.ImplicitLearningPathMethods
import no.ndla.learningpathapi.model.{api, domain}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.validation.{LanguageValidator, LearningPathValidator}
import no.ndla.mapping.License
import no.ndla.mapping.License.getLicense
import no.ndla.network.model.{CombinedUser, CombinedUserRequired, HttpRequestException}

import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: LearningPathRepositoryComponent & LanguageValidator & LearningPathValidator & OembedProxyClient & Clock &
    CommonConverter & Props =>

  lazy val converterService: ConverterService

  class ConverterService {
    def asEmbedUrlV2(embedUrl: api.EmbedUrlV2DTO, language: String): EmbedUrl = {
      learningpath.EmbedUrl(embedUrl.url, language, EmbedType.valueOfOrError(embedUrl.embedType))
    }

    def asDescription(description: api.DescriptionDTO): learningpath.Description = {
      Description(description.description, description.language)
    }

    def asTitle(title: api.TitleDTO): common.Title = {
      common.Title(title.title, title.language)
    }

    def asLearningPathTags(tags: api.LearningPathTagsDTO): common.Tag = {
      common.Tag(tags.tags, tags.language)
    }

    private def asApiLearningPathTags(tags: common.Tag): api.LearningPathTagsDTO = {
      api.LearningPathTagsDTO(tags.tags, tags.language)
    }

    def asApiCopyright(copyright: learningpath.LearningpathCopyright): api.CopyrightDTO = {
      api.CopyrightDTO(asApiLicense(copyright.license), copyright.contributors.map(_.toApi))
    }

    def asApiLicense(license: String): commonApi.LicenseDTO =
      getLicense(license) match {
        case Some(l) => commonApi.LicenseDTO(l.license.toString, Option(l.description), l.url)
        case None    => commonApi.LicenseDTO(license, Some("Invalid license"), None)
      }

    def asAuthor(user: domain.NdlaUserName): commonApi.AuthorDTO = {
      val names = Array(user.first_name, user.middle_name, user.last_name)
        .filter(_.isDefined)
        .map(_.get)
      commonApi.AuthorDTO(ContributorType.Writer, names.mkString(" "))
    }

    def asCoverPhoto(imageId: String): Option[CoverPhotoDTO] = {
      val metaUrl  = createUrlToImageApi(imageId)
      val imageUrl = createUrlToImageApiRaw(imageId)

      Some(api.CoverPhotoDTO(imageUrl, metaUrl))
    }

    private def asCopyright(copyright: api.CopyrightDTO): learningpath.LearningpathCopyright = {
      learningpath.LearningpathCopyright(copyright.license.license, copyright.contributors.map(_.toDomain))
    }

    private def getNewResponsible(toMergeInto: LearningPath, updated: UpdatedLearningPathV2DTO) =
      (updated.responsibleId, toMergeInto.responsible) match {
        case (Delete, _)                       => None
        case (UpdateWith(responsibleId), None) => Some(Responsible(responsibleId, clock.now()))
        case (UpdateWith(responsibleId), Some(existing)) if existing.responsibleId != responsibleId =>
          Some(Responsible(responsibleId, clock.now()))
        case (_, existing) => existing
      }

    private def asApiResponsible(responsible: Responsible): ResponsibleDTO =
      ResponsibleDTO(
        responsibleId = responsible.responsibleId,
        lastUpdated = responsible.lastUpdated
      )

    def asApiLearningpathV2(
        lp: LearningPath,
        language: String,
        fallback: Boolean,
        userInfo: CombinedUser
    ): Try[api.LearningPathV2DTO] = {
      val supportedLanguages = lp.supportedLanguages
      if (languageIsSupported(supportedLanguages, language) || fallback) {

        val searchLanguage = getSearchLanguage(language, supportedLanguages)

        val title = findByLanguageOrBestEffort(lp.title, language)
          .map(asApiTitle)
          .getOrElse(api.TitleDTO("", DefaultLanguage))
        val description =
          findByLanguageOrBestEffort(lp.description, language)
            .map(asApiDescription)
            .getOrElse(api.DescriptionDTO("", DefaultLanguage))
        val introduction = findByLanguageOrBestEffort(lp.introduction, language)
          .map(asApiIntroduction)
          .getOrElse(api.IntroductionDTO("", DefaultLanguage))
        val tags = findByLanguageOrBestEffort(lp.tags, language)
          .map(asApiLearningPathTags)
          .getOrElse(api.LearningPathTagsDTO(Seq(), DefaultLanguage))
        val learningSteps = lp.learningsteps
          .map(lsteps => {
            lsteps
              .flatMap(ls => asApiLearningStepV2(ls, lp, searchLanguage, fallback, userInfo).toOption)
              .toList
              .sortBy(_.seqNo)
          })
          .getOrElse(Seq.empty)

        val message = lp.message.filter(_ => lp.canEdit(userInfo)).map(asApiMessage)
        val owner   = Some(lp.owner).filter(_ => userInfo.isAdmin)
        Success(
          LearningPathV2DTO(
            id = lp.id.get,
            revision = lp.revision.get,
            isBasedOn = lp.isBasedOn,
            title = title,
            description = description,
            metaUrl = createUrlToLearningPath(lp),
            learningsteps = learningSteps,
            learningstepUrl = createUrlToLearningSteps(lp),
            coverPhoto = lp.coverPhotoId.flatMap(asCoverPhoto),
            duration = lp.duration,
            status = lp.status.toString,
            verificationStatus = lp.verificationStatus.toString,
            created = lp.created,
            lastUpdated = lp.lastUpdated,
            tags = tags,
            copyright = asApiCopyright(lp.copyright),
            canEdit = lp.canEdit(userInfo),
            supportedLanguages = supportedLanguages,
            ownerId = owner,
            message = message,
            madeAvailable = lp.madeAvailable,
            isMyNDLAOwner = lp.isMyNDLAOwner,
            responsible = lp.responsible.map(asApiResponsible),
            comments = lp.comments.map(CommonConverter.commentDomainToApi),
            priority = lp.priority,
            revisions = lp.revisionMeta.map(CommonConverter.revisionMetaDomainToApi),
            introduction = introduction
          )
        )
      } else
        Failure(
          NotFoundException(s"Language '$language' is not supported for learningpath with id '${lp.id.getOrElse(-1)}'.")
        )
    }

    private def asApiMessage(message: learningpath.Message): api.MessageDTO =
      api.MessageDTO(message.message, message.date)

    private def extractImageId(url: String): Option[String] = {
      learningPathValidator.validateCoverPhoto(url) match {
        case Some(err) => throw errors.ValidationException(errors = Seq(err))
        case _         =>
      }

      val pattern = """.*/images/(\d+)""".r
      pattern.findFirstMatchIn(url.path.toString).map(_.group(1))
    }

    private def updateImageId(existing: Option[String], url: UpdateOrDelete[String]): Option[String] = {
      url match {
        case Delete            => None
        case Missing           => existing
        case UpdateWith(value) =>
          (existing, extractImageId(value)) match {
            case (None, Some(newId))                                    => Some(newId)
            case (Some(existingId), Some(newId)) if existingId != newId => Some(newId)
            case (existing, _)                                          => existing
          }
      }
    }

    private def mergeLearningPathTags(
        existing: Seq[common.Tag],
        updated: Seq[common.Tag]
    ): Seq[common.Tag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    private def mergeStatus(existing: LearningPath, user: CombinedUser): LearningPathStatus = {
      existing.status match {
        case LearningPathStatus.PUBLISHED if existing.canSetStatus(LearningPathStatus.PUBLISHED, user).isFailure =>
          LearningPathStatus.UNLISTED
        case existingStatus => existingStatus
      }
    }

    def mergeLearningPaths(
        existing: LearningPath,
        updated: UpdatedLearningPathV2DTO,
        userInfo: CombinedUser
    ): LearningPath = {
      val status = mergeStatus(existing, userInfo)

      val titles = updated.title match {
        case None        => Seq.empty
        case Some(value) =>
          Seq(common.Title(value, updated.language))
      }

      val descriptions = updated.description match {
        case None        => Seq.empty
        case Some(value) =>
          Seq(Description(value, updated.language))
      }

      val introductions = updated.introduction match {
        case None        => Seq.empty
        case Some(value) => Seq(Introduction(value, updated.language))
      }

      val tags = updated.tags match {
        case None        => Seq.empty
        case Some(value) =>
          Seq(common.Tag(value, updated.language))
      }

      val updatedComments = updated.comments
        .map(comments => CommonConverter.mergeUpdatedCommentsWithExisting(comments, existing.comments))
        .getOrElse(existing.comments)

      val message = existing.message.filterNot(_ => updated.deleteMessage.getOrElse(false))

      val updatedRevision =
        updated.revisionMeta.map(_.map(CommonConverter.revisionMetaApiToDomain)).getOrElse(existing.revisionMeta)

      LearningPath(
        id = existing.id,
        externalId = existing.externalId,
        isBasedOn = existing.isBasedOn,
        verificationStatus = existing.verificationStatus,
        created = existing.created,
        learningsteps = existing.learningsteps,
        madeAvailable = existing.madeAvailable,
        owner = existing.owner,
        isMyNDLAOwner = existing.isMyNDLAOwner,
        revision = Some(updated.revision),
        title = mergeLanguageFields(existing.title, titles),
        description = mergeLanguageFields(existing.description, descriptions),
        introduction = mergeLanguageFields(existing.introduction, introductions),
        coverPhotoId = updateImageId(existing.coverPhotoId, updated.coverPhotoMetaUrl),
        duration =
          if (updated.duration.isDefined)
            updated.duration
          else existing.duration,
        tags = mergeLearningPathTags(existing.tags, tags),
        status = status,
        copyright =
          if (updated.copyright.isDefined)
            converterService.asCopyright(updated.copyright.get)
          else existing.copyright,
        lastUpdated = clock.now(),
        message = message,
        responsible = getNewResponsible(existing, updated),
        comments = updatedComments,
        priority = updated.priority.getOrElse(existing.priority),
        revisionMeta = updatedRevision
      )
    }

    def asDomainLearningStep(newLearningStep: NewLearningStepV2DTO, learningPath: LearningPath): Try[LearningStep] = {
      val introduction = newLearningStep.introduction
        .filterNot(_.isEmpty)
        .map(Introduction(_, newLearningStep.language))
        .toSeq

      val description = newLearningStep.description
        .map(Description(_, newLearningStep.language))
        .toSeq

      val embedUrlT = newLearningStep.embedUrl
        .map(converterService.asDomainEmbedUrl(_, newLearningStep.language))
        .map(t => t.map(embed => Seq(embed)))
        .getOrElse(Success(Seq.empty))

      val listOfLearningSteps = learningPath.learningsteps.getOrElse(Seq.empty)

      val newSeqNo =
        if (listOfLearningSteps.isEmpty) 0
        else listOfLearningSteps.map(_.seqNo).max + 1

      val copyright = newLearningStep.copyright match {
        case Some(copyright) => Some(asCopyright(copyright))
        case None            => newLearningStep.license.map(l => LearningpathCopyright(l, Seq.empty))
      }

      embedUrlT.map(embedUrl =>
        LearningStep(
          id = None,
          revision = None,
          externalId = None,
          learningPathId = learningPath.id,
          seqNo = newSeqNo,
          title = Seq(common.Title(newLearningStep.title, newLearningStep.language)),
          introduction = introduction,
          description = description,
          embedUrl = embedUrl,
          articleId = newLearningStep.articleId,
          `type` = StepType.valueOfOrError(newLearningStep.`type`),
          copyright = copyright,
          showTitle = newLearningStep.showTitle
        )
      )
    }

    def insertLearningSteps(
        learningPath: LearningPath,
        steps: Seq[LearningStep],
        user: CombinedUserRequired
    ): LearningPath = {
      steps.foldLeft(learningPath) { (lp, ls) =>
        insertLearningStep(lp, ls, user)
      }
    }

    def insertLearningStep(
        learningPath: LearningPath,
        updatedStep: LearningStep,
        user: CombinedUserRequired
    ): LearningPath = {
      val status                = mergeStatus(learningPath, user)
      val existingLearningSteps = learningPath.learningsteps.getOrElse(Seq.empty).filterNot(_.id == updatedStep.id)
      val steps                 =
        if (StepStatus.ACTIVE == updatedStep.status) existingLearningSteps :+ updatedStep else existingLearningSteps

      learningPath.copy(learningsteps = Some(steps), status = status, lastUpdated = clock.now())
    }

    def deleteLearningStepLanguage(step: LearningStep, language: String): Try[LearningStep] = {
      step.title.size match {
        case 1 =>
          Failure(
            errors.OperationNotAllowedException(s"Cannot delete last title for step with id ${step.id.getOrElse(-1)}")
          )
        case _ =>
          Success(
            step.copy(
              title = step.title.filterNot(_.language == language),
              introduction = step.introduction.filterNot(_.language == language),
              description = step.description.filterNot(_.language == language),
              embedUrl = step.embedUrl.filterNot(_.language == language)
            )
          )
      }
    }

    def deleteLearningPathLanguage(learningPath: LearningPath, language: String): Try[LearningPath] = {
      learningPath.title.size match {
        case 1 =>
          Failure(
            errors.OperationNotAllowedException(
              s"Cannot delete last language for learning path with id ${learningPath.id.getOrElse(-1)}"
            )
          )
        case _ =>
          Success(
            learningPath.copy(
              title = learningPath.title.filterNot(_.language == language),
              description = learningPath.description.filterNot(_.language == language),
              tags = learningPath.tags.filterNot(_.language == language)
            )
          )
      }
    }

    def mergeLearningSteps(existing: LearningStep, updated: UpdatedLearningStepV2DTO): Try[LearningStep] = {
      val titles = updated.title match {
        case None        => existing.title
        case Some(value) =>
          mergeLanguageFields(existing.title, Seq(common.Title(value, updated.language)))
      }

      val introductions = updated.introduction match {
        case Missing           => existing.introduction
        case Delete            => existing.introduction.filterNot(_.language == updated.language)
        case UpdateWith(value) => mergeLanguageFields(existing.introduction, Seq(Introduction(value, updated.language)))
      }

      val descriptions = updated.description match {
        case Missing           => existing.description
        case Delete            => existing.description.filterNot(_.language == updated.language)
        case UpdateWith(value) =>
          mergeLanguageFields(existing.description, Seq(Description(value, updated.language)))
      }

      val embedUrlsT = updated.embedUrl match {
        case Missing           => Success(existing.embedUrl)
        case Delete            => Success(existing.embedUrl.filterNot(_.language == updated.language))
        case UpdateWith(value) =>
          converterService
            .asDomainEmbedUrl(value, updated.language)
            .map(newEmbedUrl => mergeLanguageFields(existing.embedUrl, Seq(newEmbedUrl)))
      }

      val articleId = updated.articleId match {
        case Missing           => existing.articleId
        case Delete            => None
        case UpdateWith(value) => Some(value)
      }

      val stepType = updated.`type`.map(learningpath.StepType.valueOfOrError).getOrElse(existing.`type`)

      val copyright = (updated.copyright, updated.license) match {
        case (Missing, Some(license)) => LearningpathCopyright(license, Seq.empty).some
        case (Missing, _)             => existing.copyright
        case (Delete, _)              => None
        case (UpdateWith(value), _)   => Some(converterService.asCopyright(value))
      }

      embedUrlsT.map(embedUrls =>
        existing.copy(
          revision = Some(updated.revision),
          title = titles,
          introduction = introductions,
          description = descriptions,
          embedUrl = embedUrls,
          articleId = articleId,
          showTitle = updated.showTitle.getOrElse(existing.showTitle),
          `type` = stepType,
          copyright = copyright
        )
      )
    }

    private def getVerificationStatus(user: CombinedUser): LearningPathVerificationStatus =
      if (user.isNdla)
        LearningPathVerificationStatus.CREATED_BY_NDLA
      else LearningPathVerificationStatus.EXTERNAL

    def newFromExistingLearningPath(
        existing: LearningPath,
        newLearningPath: NewCopyLearningPathV2DTO,
        user: CombinedUser
    ): Try[LearningPath] = {
      val oldTitle = Seq(common.Title(newLearningPath.title, newLearningPath.language))

      val oldDescription = newLearningPath.description match {
        case None        => Seq.empty
        case Some(value) =>
          Seq(Description(value, newLearningPath.language))
      }

      val oldTags = newLearningPath.tags match {
        case None        => Seq.empty
        case Some(value) =>
          Seq(common.Tag(value, newLearningPath.language))
      }

      user.id.toTry(AccessDeniedException("User id not found")).map { ownerId =>
        val title        = mergeLanguageFields(existing.title, oldTitle)
        val description  = mergeLanguageFields(existing.description, oldDescription)
        val tags         = converterService.mergeLearningPathTags(existing.tags, oldTags)
        val coverPhotoId = newLearningPath.coverPhotoMetaUrl
          .map(converterService.extractImageId)
          .getOrElse(existing.coverPhotoId)
        val duration =
          if (newLearningPath.duration.nonEmpty) newLearningPath.duration
          else existing.duration
        val copyright = newLearningPath.copyright
          .map(converterService.asCopyright)
          .getOrElse(existing.copyright)

        val existingPathCopyright = existing.copyright.some

        val steps = existing.learningsteps
          .map(ls =>
            ls.map { step =>
              val stepCopyright = step.`type` match {
                case StepType.TEXT => step.copyright.orElse(existingPathCopyright)
                case _             => step.copyright
              }

              step.copy(
                id = None,
                revision = None,
                externalId = None,
                learningPathId = None,
                copyright = stepCopyright
              )
            }
          )

        existing.copy(
          id = None,
          revision = None,
          externalId = None,
          isBasedOn = if (existing.isPrivate) None else existing.id,
          title = title,
          description = description,
          status = LearningPathStatus.PRIVATE,
          verificationStatus = getVerificationStatus(user),
          lastUpdated = clock.now(),
          madeAvailable = None,
          owner = ownerId,
          copyright = copyright,
          learningsteps = steps,
          tags = tags,
          coverPhotoId = coverPhotoId,
          duration = duration
        )
      }
    }

    def newLearningPath(newLearningPath: NewLearningPathV2DTO, user: CombinedUser): Try[LearningPath] = {
      val domainTags =
        if (newLearningPath.tags.isEmpty) Seq.empty
        else
          Seq(common.Tag(newLearningPath.tags.getOrElse(List()), newLearningPath.language))
      val description  = newLearningPath.description.map(Description(_, newLearningPath.language)).toSeq
      val introduction = newLearningPath.introduction.map(Introduction(_, newLearningPath.language)).toSeq

      val copyright = newLearningPath.copyright.getOrElse(newDefaultCopyright(user))

      val priority = newLearningPath.priority.getOrElse(common.Priority.Unspecified)

      val revisionMeta = newLearningPath.revisionMeta match {
        case Some(revs) if revs.nonEmpty =>
          newLearningPath.revisionMeta
            .map(_.map(CommonConverter.revisionMetaApiToDomain))
            .getOrElse(RevisionMeta.default)
        case _ => RevisionMeta.default
      }

      user.id.toTry(AccessDeniedException("User id not found")).map { ownerId =>
        LearningPath(
          id = None,
          revision = None,
          externalId = None,
          isBasedOn = None,
          title = Seq(common.Title(newLearningPath.title, newLearningPath.language)),
          description = description,
          coverPhotoId = newLearningPath.coverPhotoMetaUrl.flatMap(converterService.extractImageId),
          duration = newLearningPath.duration,
          status = learningpath.LearningPathStatus.PRIVATE,
          verificationStatus = getVerificationStatus(user),
          created = clock.now(),
          lastUpdated = clock.now(),
          tags = domainTags,
          owner = ownerId,
          copyright = converterService.asCopyright(copyright),
          isMyNDLAOwner = user.isMyNDLAUser,
          learningsteps = Some(Seq.empty),
          message = None,
          madeAvailable = None,
          responsible = newLearningPath.responsibleId
            .map(responsibleId => Responsible(responsibleId = responsibleId, lastUpdated = clock.now())),
          comments = newLearningPath.comments
            .map(comments => comments.map(CommonConverter.newCommentApiToDomain))
            .getOrElse(Seq.empty),
          priority = priority,
          revisionMeta = revisionMeta,
          introduction = introduction
        )
      }
    }

    private def newDefaultCopyright(user: CombinedUser): CopyrightDTO = {
      val contributors =
        user.myndlaUser
          .map(_.displayName)
          .map(name => Seq(commonApi.AuthorDTO(ContributorType.Writer, name)))
          .getOrElse(Seq.empty)
      CopyrightDTO(asApiLicense(License.CC_BY.toString), contributors)
    }

    def getApiIntroduction(learningSteps: Seq[LearningStep]): Seq[api.IntroductionDTO] = {
      learningSteps
        .find(_.`type` == learningpath.StepType.INTRODUCTION)
        .toList
        .flatMap(x => x.description)
        .map(x => api.IntroductionDTO(x.description, x.language))
    }

    def languageIsNotSupported(supportedLanguages: Seq[String], language: String): Boolean = {
      supportedLanguages.isEmpty || (!supportedLanguages.contains(language) && language != AllLanguages)
    }

    def asApiLearningpathSummaryV2(
        learningpath: LearningPath,
        user: CombinedUser
    ): Try[api.LearningPathSummaryV2DTO] = {
      val supportedLanguages = learningpath.supportedLanguages

      val title = findByLanguageOrBestEffort(learningpath.title, AllLanguages)
        .map(asApiTitle)
        .getOrElse(api.TitleDTO("", DefaultLanguage))
      val description = findByLanguageOrBestEffort(learningpath.description, AllLanguages)
        .map(asApiDescription)
        .getOrElse(api.DescriptionDTO("", DefaultLanguage))
      val tags = findByLanguageOrBestEffort(learningpath.tags, AllLanguages)
        .map(asApiLearningPathTags)
        .getOrElse(api.LearningPathTagsDTO(Seq(), DefaultLanguage))
      val introduction =
        findByLanguageOrBestEffort(learningpath.introduction.map(asApiIntroduction), AllLanguages).getOrElse(
          findByLanguageOrBestEffort(getApiIntroduction(learningpath.learningsteps.getOrElse(Seq.empty)), AllLanguages)
            .getOrElse(api.IntroductionDTO("", DefaultLanguage))
        )

      val message = learningpath.message.filter(_ => learningpath.canEdit(user)).map(_.message)

      Success(
        api.LearningPathSummaryV2DTO(
          learningpath.id.get,
          revision = learningpath.revision,
          title,
          description,
          introduction,
          createUrlToLearningPath(learningpath),
          learningpath.coverPhotoId.flatMap(asCoverPhoto).map(_.url),
          learningpath.duration,
          learningpath.status.toString,
          learningpath.created,
          learningpath.lastUpdated,
          tags,
          asApiCopyright(learningpath.copyright),
          supportedLanguages,
          learningpath.isBasedOn,
          message
        )
      )
    }

    private def languageIsSupported(supportedLangs: Seq[String], language: String): Boolean = {
      val isLanguageNeutral = supportedLangs.contains(UnknownLanguage.toString) && supportedLangs.length == 1

      supportedLangs.contains(language) || language == AllLanguages || isLanguageNeutral
    }

    def asApiLearningStepV2(
        ls: LearningStep,
        lp: LearningPath,
        language: String,
        fallback: Boolean,
        user: CombinedUser
    ): Try[api.LearningStepV2DTO] = {
      val supportedLanguages = ls.supportedLanguages

      if (languageIsSupported(supportedLanguages, language) || fallback) {
        val title = findByLanguageOrBestEffort(ls.title, language)
          .map(asApiTitle)
          .getOrElse(api.TitleDTO("", DefaultLanguage))
        val introduction =
          findByLanguageOrBestEffort(ls.introduction, language)
            .map(asApiIntroduction)
        val description =
          findByLanguageOrBestEffort(ls.description, language)
            .map(asApiDescription)
        val embedUrl = findByLanguageOrBestEffort(ls.embedUrl, language)
          .map(asApiEmbedUrlV2)
          .map(createEmbedUrl)

        val copyright = ls.copyright.map(asApiCopyright)

        Success(
          api.LearningStepV2DTO(
            id = ls.id.get,
            revision = ls.revision.get,
            seqNo = ls.seqNo,
            title = title,
            introduction = introduction,
            description = description,
            embedUrl = embedUrl,
            articleId = ls.articleId,
            showTitle = ls.showTitle,
            `type` = ls.`type`.toString,
            license = copyright.map(_.license),
            copyright = copyright,
            metaUrl = createUrlToLearningStep(ls, lp),
            canEdit = lp.canEdit(user),
            status = ls.status.entryName,
            supportedLanguages = supportedLanguages
          )
        )
      } else {
        Failure(
          NotFoundException(
            s"Learningstep with id '${ls.id.getOrElse(-1)}' in learningPath '${lp.id.getOrElse(-1)}' and language $language not found."
          )
        )
      }
    }

    def asApiLearningStepSummaryV2(
        ls: LearningStep,
        lp: LearningPath,
        language: String
    ): Option[api.LearningStepSummaryV2DTO] = {
      findByLanguageOrBestEffort(ls.title, language).map(title =>
        api.LearningStepSummaryV2DTO(
          ls.id.get,
          ls.seqNo,
          asApiTitle(title),
          ls.`type`.toString,
          createUrlToLearningStep(ls, lp)
        )
      )
    }

    def asLearningStepContainerSummary(
        status: StepStatus,
        learningPath: LearningPath,
        language: String,
        fallback: Boolean
    ): Try[api.LearningStepContainerSummaryDTO] = {
      val learningSteps = learningPathRepository
        .learningStepsFor(learningPath.id.get)
        .filter(_.status == status)
      val supportedLanguages =
        learningSteps.flatMap(_.title).map(_.language).distinct

      if ((languageIsSupported(supportedLanguages, language) || fallback) && learningSteps.nonEmpty) {
        val searchLanguage =
          if (supportedLanguages.contains(language) || language == AllLanguages)
            getSearchLanguage(language, supportedLanguages)
          else language

        Success(
          api.LearningStepContainerSummaryDTO(
            searchLanguage,
            learningSteps
              .flatMap(ls =>
                converterService
                  .asApiLearningStepSummaryV2(ls, learningPath, searchLanguage)
              )
              .sortBy(_.seqNo),
            supportedLanguages
          )
        )
      } else
        Failure(
          NotFoundException(s"Learningpath with id ${learningPath.id.getOrElse(-1)} and language $language not found")
        )
    }

    def asApiLearningPathTagsSummary(
        allTags: List[api.LearningPathTagsDTO],
        language: String,
        fallback: Boolean
    ): Option[api.LearningPathTagsSummaryDTO] = {
      val supportedLanguages = allTags.map(_.language).distinct

      if (languageIsSupported(supportedLanguages, language) || fallback) {
        val searchLanguage = getSearchLanguage(language, supportedLanguages)
        val tags           = allTags
          .filter(_.language == searchLanguage)
          .flatMap(_.tags)

        Some(
          api.LearningPathTagsSummaryDTO(
            searchLanguage,
            supportedLanguages,
            tags
          )
        )
      } else
        None
    }

    private def asApiTitle(title: common.Title): api.TitleDTO = {
      api.TitleDTO(title.title, title.language)
    }

    private def asApiIntroduction(introduction: Introduction): api.IntroductionDTO = {
      api.IntroductionDTO(introduction.introduction, introduction.language)
    }

    private def asApiDescription(description: Description): api.DescriptionDTO = {
      api.DescriptionDTO(description.description, description.language)
    }

    private def asApiEmbedUrlV2(embedUrl: EmbedUrl): api.EmbedUrlV2DTO = {
      api.EmbedUrlV2DTO(embedUrl.url, embedUrl.embedType.toString)
    }

    def asDomainEmbedUrl(embedUrl: api.EmbedUrlV2DTO, language: String): Try[EmbedUrl] = {
      val hostOpt = embedUrl.url.hostOption

      lazy val domainEmbedUrl = Success(
        learningpath.EmbedUrl(
          embedUrl.url,
          language,
          EmbedType.valueOfOrError(embedUrl.embedType)
        )
      )

      hostOpt match {
        case Some(host) if props.NdlaFrontendHostNames.contains(host.toString) =>
          oembedProxyClient
            .getIframeUrl(embedUrl.url)
            .map(newUrl => {
              val pathAndQueryParams: String = newUrl.url.path.toString
                .withQueryString(newUrl.url.query)
                .toString
              learningpath.EmbedUrl(
                url = pathAndQueryParams,
                language = language,
                embedType = EmbedType.IFrame
              )
            })
            .recoverWith { case e: HttpRequestException if 400 until 500 contains e.code => domainEmbedUrl }
        case _ => domainEmbedUrl
      }
    }

    private def createUrlToLearningStep(ls: LearningStep, lp: LearningPath): String = {
      s"${createUrlToLearningSteps(lp)}/${ls.id.get}"
    }

    private def createUrlToLearningSteps(lp: LearningPath): String = {
      s"${createUrlToLearningPath(lp)}/learningsteps"
    }

    def createUrlToLearningPath(lp: LearningPath): String = {
      s"${props.Domain}${props.LearningpathControllerPath}${lp.id.get}"
    }

    def createUrlToLearningPath(lp: api.LearningPathV2DTO): String = {
      s"${props.Domain}${props.LearningpathControllerPath}${lp.id}"
    }

    private def createUrlToImageApi(imageId: String): String = {
      s"${props.ExternalApiUrls.ImageApiUrl}/$imageId"
    }
    private def createUrlToImageApiRaw(imageId: String): String = {
      s"${props.ExternalApiUrls.ImageApiRawUrl}/id/$imageId"
    }

    private def createEmbedUrl(embedUrlOrPath: EmbedUrlV2DTO): EmbedUrlV2DTO = {
      embedUrlOrPath.url.hostOption match {
        case Some(_) => embedUrlOrPath
        case None    =>
          embedUrlOrPath.copy(url = s"${props.NdlaFrontendProtocol}://${props.NdlaFrontendHost}${embedUrlOrPath.url}")
      }
    }

  }
}
