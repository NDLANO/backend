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
import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.common.implicits.OptionImplicit
import no.ndla.common.model.domain.learningpath
import no.ndla.common.model.domain.learningpath.{
  Description,
  EmbedType,
  EmbedUrl,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningStep,
  StepStatus,
  StepType
}
import no.ndla.common.model.{api as commonApi, domain as common}
import no.ndla.common.{Clock, errors}
import no.ndla.language.Language.*
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.integration.*
import no.ndla.learningpathapi.model.api.{LearningPathStatus as _, *}
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathCombinedUser
import no.ndla.learningpathapi.model.domain.ImplicitLearningPath.ImplicitLearningPathMethods
import no.ndla.learningpathapi.model.{api, domain}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.validation.{LanguageValidator, LearningPathValidator}
import no.ndla.mapping.License.getLicense
import no.ndla.network.ApplicationUrl
import no.ndla.network.model.{CombinedUser, CombinedUserRequired}

import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: LearningPathRepositoryComponent & LanguageValidator & LearningPathValidator & OembedProxyClient & Clock &
    Props =>

  val converterService: ConverterService

  class ConverterService {
    import props.*

    def asEmbedUrlV2(embedUrl: api.EmbedUrlV2, language: String): EmbedUrl = {
      learningpath.EmbedUrl(embedUrl.url, language, EmbedType.valueOfOrError(embedUrl.embedType))
    }

    def asDescription(description: api.Description): learningpath.Description = {
      Description(description.description, description.language)
    }

    def asTitle(title: api.Title): common.Title = {
      common.Title(title.title, title.language)
    }

    def asLearningPathTags(tags: api.LearningPathTags): common.Tag = {
      common.Tag(tags.tags, tags.language)
    }

    private def asApiLearningPathTags(tags: common.Tag): api.LearningPathTags = {
      api.LearningPathTags(tags.tags, tags.language)
    }

    def asApiCopyright(copyright: learningpath.LearningpathCopyright): api.Copyright = {
      api.Copyright(asApiLicense(copyright.license), copyright.contributors.map(_.toApi))
    }

    def asApiLicense(license: String): commonApi.License =
      getLicense(license) match {
        case Some(l) => commonApi.License(l.license.toString, Option(l.description), l.url)
        case None    => commonApi.License(license, Some("Invalid license"), None)
      }

    def asAuthor(user: domain.NdlaUserName): commonApi.Author = {
      val names = Array(user.first_name, user.middle_name, user.last_name)
        .filter(_.isDefined)
        .map(_.get)
      commonApi.Author("Forfatter", names.mkString(" "))
    }

    def asCoverPhoto(imageId: String): Option[CoverPhoto] = {
      val metaUrl  = createUrlToImageApi(imageId)
      val imageUrl = createUrlToImageApiRaw(imageId)

      Some(api.CoverPhoto(imageUrl, metaUrl))
    }

    private def asCopyright(copyright: api.Copyright): learningpath.LearningpathCopyright = {
      learningpath.LearningpathCopyright(copyright.license.license, copyright.contributors.map(_.toDomain))
    }

    def asApiLearningpathV2(
        lp: LearningPath,
        language: String,
        fallback: Boolean,
        userInfo: CombinedUser
    ): Try[api.LearningPathV2] = {
      val supportedLanguages = lp.supportedLanguages
      if (languageIsSupported(supportedLanguages, language) || fallback) {

        val searchLanguage = getSearchLanguage(language, supportedLanguages)

        val title = findByLanguageOrBestEffort(lp.title, language)
          .map(asApiTitle)
          .getOrElse(api.Title("", DefaultLanguage))
        val description =
          findByLanguageOrBestEffort(lp.description, language)
            .map(asApiDescription)
            .getOrElse(api.Description("", DefaultLanguage))

        val tags = findByLanguageOrBestEffort(lp.tags, language)
          .map(asApiLearningPathTags)
          .getOrElse(api.LearningPathTags(Seq(), DefaultLanguage))
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
          api.LearningPathV2(
            lp.id.get,
            lp.revision.get,
            lp.isBasedOn,
            title,
            description,
            createUrlToLearningPath(lp),
            learningSteps,
            createUrlToLearningSteps(lp),
            lp.coverPhotoId.flatMap(asCoverPhoto),
            lp.duration,
            lp.status.toString,
            lp.verificationStatus.toString,
            lp.created,
            lp.lastUpdated,
            tags,
            asApiCopyright(lp.copyright),
            lp.canEdit(userInfo),
            supportedLanguages,
            owner,
            message
          )
        )
      } else
        Failure(
          NotFoundException(s"Language '$language' is not supported for learningpath with id '${lp.id.getOrElse(-1)}'.")
        )
    }

    private def asApiMessage(message: learningpath.Message): api.Message =
      api.Message(message.message, message.date)

    private def extractImageId(url: String): Option[String] = {
      learningPathValidator.validateCoverPhoto(url) match {
        case Some(err) => throw errors.ValidationException(errors = Seq(err))
        case _         =>
      }

      val pattern = """.*/images/(\d+)""".r
      pattern.findFirstMatchIn(url.path.toString).map(_.group(1))
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
        updated: UpdatedLearningPathV2,
        userInfo: CombinedUser
    ): LearningPath = {
      val status = mergeStatus(existing, userInfo)

      val titles = updated.title match {
        case None => Seq.empty
        case Some(value) =>
          Seq(common.Title(value, updated.language))
      }

      val descriptions = updated.description match {
        case None => Seq.empty
        case Some(value) =>
          Seq(Description(value, updated.language))
      }

      val tags = updated.tags match {
        case None => Seq.empty
        case Some(value) =>
          Seq(common.Tag(value, updated.language))
      }

      val message = existing.message.filterNot(_ => updated.deleteMessage.getOrElse(false))

      existing.copy(
        revision = Some(updated.revision),
        title = mergeLanguageFields(existing.title, titles),
        description = mergeLanguageFields(existing.description, descriptions),
        coverPhotoId = updated.coverPhotoMetaUrl
          .map(extractImageId)
          .getOrElse(existing.coverPhotoId),
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
        message = message
      )
    }

    def asDomainLearningStep(newLearningStep: NewLearningStepV2, learningPath: LearningPath): Try[LearningStep] = {
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

      embedUrlT.map(embedUrl =>
        LearningStep(
          None,
          None,
          None,
          learningPath.id,
          newSeqNo,
          Seq(common.Title(newLearningStep.title, newLearningStep.language)),
          description,
          embedUrl,
          StepType.valueOfOrError(newLearningStep.`type`),
          newLearningStep.license,
          newLearningStep.showTitle
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
      val steps =
        if (StepStatus.ACTIVE == updatedStep.status) existingLearningSteps :+ updatedStep else existingLearningSteps

      learningPath.copy(learningsteps = Some(steps), status = status, lastUpdated = clock.now())
    }

    def mergeLearningSteps(existing: LearningStep, updated: UpdatedLearningStepV2): Try[LearningStep] = {
      val titles = updated.title match {
        case None => existing.title
        case Some(value) =>
          mergeLanguageFields(existing.title, Seq(common.Title(value, updated.language)))
      }

      val descriptions = updated.description match {
        case None => existing.description
        case Some(value) =>
          mergeLanguageFields(existing.description, Seq(Description(value, updated.language)))
      }

      val embedUrlsT = updated.embedUrl match {
        case None => Success(existing.embedUrl)
        case Some(value) =>
          converterService
            .asDomainEmbedUrl(value, updated.language)
            .map(newEmbedUrl => mergeLanguageFields(existing.embedUrl, Seq(newEmbedUrl)))
      }

      embedUrlsT.map(embedUrls =>
        existing.copy(
          revision = Some(updated.revision),
          title = titles,
          description = descriptions,
          embedUrl = embedUrls,
          showTitle = updated.showTitle.getOrElse(existing.showTitle),
          `type` = updated.`type`
            .map(learningpath.StepType.valueOfOrError)
            .getOrElse(existing.`type`),
          license = updated.license
        )
      )
    }

    private def getVerificationStatus(user: CombinedUser): LearningPathVerificationStatus =
      if (user.isNdla)
        LearningPathVerificationStatus.CREATED_BY_NDLA
      else LearningPathVerificationStatus.EXTERNAL

    def newFromExistingLearningPath(
        existing: LearningPath,
        newLearningPath: NewCopyLearningPathV2,
        user: CombinedUser
    ): Try[LearningPath] = {
      val oldTitle = Seq(common.Title(newLearningPath.title, newLearningPath.language))

      val oldDescription = newLearningPath.description match {
        case None => Seq.empty
        case Some(value) =>
          Seq(Description(value, newLearningPath.language))
      }

      val oldTags = newLearningPath.tags match {
        case None => Seq.empty
        case Some(value) =>
          Seq(common.Tag(value, newLearningPath.language))
      }

      user.id.toTry(AccessDeniedException("User id not found")).map { ownerId =>
        val title       = mergeLanguageFields(existing.title, oldTitle)
        val description = mergeLanguageFields(existing.description, oldDescription)
        val tags        = converterService.mergeLearningPathTags(existing.tags, oldTags)
        val coverPhotoId = newLearningPath.coverPhotoMetaUrl
          .map(converterService.extractImageId)
          .getOrElse(existing.coverPhotoId)
        val duration =
          if (newLearningPath.duration.nonEmpty) newLearningPath.duration
          else existing.duration
        val copyright = newLearningPath.copyright
          .map(converterService.asCopyright)
          .getOrElse(existing.copyright)

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
          owner = ownerId,
          copyright = copyright,
          learningsteps = existing.learningsteps
            .map(ls => ls.map(_.copy(id = None, revision = None, externalId = None, learningPathId = None))),
          tags = tags,
          coverPhotoId = coverPhotoId,
          duration = duration
        )
      }
    }

    def newLearningPath(newLearningPath: NewLearningPathV2, user: CombinedUser): Try[LearningPath] = {
      val domainTags =
        if (newLearningPath.tags.isEmpty) Seq.empty
        else
          Seq(common.Tag(newLearningPath.tags, newLearningPath.language))

      user.id.toTry(AccessDeniedException("User id not found")).map { ownerId =>
        LearningPath(
          None,
          None,
          None,
          None,
          Seq(common.Title(newLearningPath.title, newLearningPath.language)),
          Seq(Description(newLearningPath.description, newLearningPath.language)),
          newLearningPath.coverPhotoMetaUrl.flatMap(converterService.extractImageId),
          newLearningPath.duration,
          learningpath.LearningPathStatus.PRIVATE,
          getVerificationStatus(user),
          clock.now(),
          clock.now(),
          domainTags,
          ownerId,
          converterService.asCopyright(newLearningPath.copyright),
          Some(Seq.empty)
        )
      }
    }

    def getApiIntroduction(learningSteps: Seq[LearningStep]): Seq[api.Introduction] = {
      learningSteps
        .find(_.`type` == learningpath.StepType.INTRODUCTION)
        .toList
        .flatMap(x => x.description)
        .map(x => api.Introduction(x.description, x.language))
    }

    def languageIsNotSupported(supportedLanguages: Seq[String], language: String): Boolean = {
      supportedLanguages.isEmpty || (!supportedLanguages.contains(language) && language != AllLanguages)
    }

    def asApiLearningpathSummaryV2(
        learningpath: LearningPath,
        user: CombinedUser
    ): Try[api.LearningPathSummaryV2] = {
      val supportedLanguages = learningpath.supportedLanguages

      val title = findByLanguageOrBestEffort(learningpath.title, AllLanguages)
        .map(asApiTitle)
        .getOrElse(api.Title("", DefaultLanguage))
      val description = findByLanguageOrBestEffort(learningpath.description, AllLanguages)
        .map(asApiDescription)
        .getOrElse(api.Description("", DefaultLanguage))
      val tags = findByLanguageOrBestEffort(learningpath.tags, AllLanguages)
        .map(asApiLearningPathTags)
        .getOrElse(api.LearningPathTags(Seq(), DefaultLanguage))
      val introduction =
        findByLanguageOrBestEffort(getApiIntroduction(learningpath.learningsteps.getOrElse(Seq.empty)), AllLanguages)
          .getOrElse(api.Introduction("", DefaultLanguage))

      val message = learningpath.message.filter(_ => learningpath.canEdit(user)).map(_.message)

      Success(
        api.LearningPathSummaryV2(
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
    ): Try[api.LearningStepV2] = {
      val supportedLanguages = ls.supportedLanguages

      if (languageIsSupported(supportedLanguages, language) || fallback) {
        val title = findByLanguageOrBestEffort(ls.title, language)
          .map(asApiTitle)
          .getOrElse(api.Title("", DefaultLanguage))
        val description =
          findByLanguageOrBestEffort(ls.description, language)
            .map(asApiDescription)
        val embedUrl = findByLanguageOrBestEffort(ls.embedUrl, language)
          .map(asApiEmbedUrlV2)
          .map(createEmbedUrl)

        Success(
          api.LearningStepV2(
            ls.id.get,
            ls.revision.get,
            ls.seqNo,
            title,
            description,
            embedUrl,
            ls.showTitle,
            ls.`type`.toString,
            ls.license.map(asApiLicense),
            createUrlToLearningStep(ls, lp),
            lp.canEdit(user),
            ls.status.entryName,
            supportedLanguages
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
    ): Option[api.LearningStepSummaryV2] = {
      findByLanguageOrBestEffort(ls.title, language).map(title =>
        api.LearningStepSummaryV2(
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
    ): Try[api.LearningStepContainerSummary] = {
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
          api.LearningStepContainerSummary(
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
        allTags: List[api.LearningPathTags],
        language: String,
        fallback: Boolean
    ): Option[api.LearningPathTagsSummary] = {
      val supportedLanguages = allTags.map(_.language).distinct

      if (languageIsSupported(supportedLanguages, language) || fallback) {
        val searchLanguage = getSearchLanguage(language, supportedLanguages)
        val tags = allTags
          .filter(_.language == searchLanguage)
          .flatMap(_.tags)

        Some(
          api.LearningPathTagsSummary(
            searchLanguage,
            supportedLanguages,
            tags
          )
        )
      } else
        None
    }

    private def asApiTitle(title: common.Title): api.Title = {
      api.Title(title.title, title.language)
    }

    private def asApiDescription(description: Description): api.Description = {
      api.Description(description.description, description.language)
    }

    private def asApiEmbedUrlV2(embedUrl: EmbedUrl): api.EmbedUrlV2 = {
      api.EmbedUrlV2(embedUrl.url, embedUrl.embedType.toString)
    }

    def asDomainEmbedUrl(embedUrl: api.EmbedUrlV2, language: String): Try[EmbedUrl] = {
      val hostOpt = embedUrl.url.hostOption
      hostOpt match {
        case Some(host) if NdlaFrontendHostNames.contains(host.toString) =>
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
        case _ =>
          Success(
            learningpath.EmbedUrl(
              embedUrl.url,
              language,
              EmbedType.valueOfOrError(embedUrl.embedType)
            )
          )
      }
    }

    private def createUrlToLearningStep(ls: LearningStep, lp: LearningPath): String = {
      s"${createUrlToLearningSteps(lp)}/${ls.id.get}"
    }

    private def createUrlToLearningSteps(lp: LearningPath): String = {
      s"${createUrlToLearningPath(lp)}/learningsteps"
    }

    def createUrlToLearningPath(lp: LearningPath): String = {
      s"${ApplicationUrl.get}${lp.id.get}"
    }

    def createUrlToLearningPath(lp: api.LearningPathV2): String = {
      s"${ApplicationUrl.get}${lp.id}"
    }

    private def createUrlToImageApi(imageId: String): String = {
      s"${ExternalApiUrls.ImageApiUrl}/$imageId"
    }
    private def createUrlToImageApiRaw(imageId: String): String = {
      s"${ExternalApiUrls.ImageApiRawUrl}/id/$imageId"
    }

    private def createEmbedUrl(embedUrlOrPath: EmbedUrlV2): EmbedUrlV2 = {
      embedUrlOrPath.url.hostOption match {
        case Some(_) => embedUrlOrPath
        case None =>
          embedUrlOrPath.copy(url = s"$NdlaFrontendProtocol://$NdlaFrontendHost${embedUrlOrPath.url}")
      }
    }

  }
}
