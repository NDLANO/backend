/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.Props
import no.ndla.articleapi.model.api.{ArticleSummaryV2DTO, ImportException, NotFoundException}
import no.ndla.articleapi.model.domain.*
import no.ndla.articleapi.model.search.SearchableArticle
import no.ndla.articleapi.model.api
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.common
import no.ndla.common.{CirceUtil, model}
import no.ndla.common.model.{RelatedContentLink, api as commonApi}
import no.ndla.common.model.api.{Delete, DisclaimerDTO, LicenseDTO, Missing, UpdateWith}
import no.ndla.common.model.domain.{
  ArticleContent,
  ArticleMetaImage,
  Description,
  Introduction,
  RelatedContent,
  RequiredLibrary,
  Tag,
  Title,
  VisualElement,
  article
}
import no.ndla.common.model.domain.article.{
  Article,
  ArticleMetaDescriptionDTO,
  ArticleTagDTO,
  Copyright,
  PartialPublishArticleDTO
}
import no.ndla.language.Language.{
  AllLanguages,
  UnknownLanguage,
  findByLanguageOrBestEffort,
  getSupportedLanguages,
  sortLanguagesByPriority
}
import no.ndla.mapping.License.getLicense
import no.ndla.network.ApplicationUrl
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}

class ConverterService(using articleRepository: ArticleRepository, props: Props) extends StrictLogging {

  /** Attempts to extract language that hit from highlights in elasticsearch response.
    * @param result
    *   Elasticsearch hit.
    * @return
    *   Language if found.
    */
  def getLanguageFromHit(result: SearchHit): Option[String] = {
    def keyToLanguage(keys: Iterable[String]): Option[String] = {
      val keyLanguages = keys.toList.flatMap(key =>
        key.split('.').toList match {
          case _ :: language :: _ => Some(language)
          case _                  => None
        }
      )

      sortLanguagesByPriority(keyLanguages).headOption
    }

    val highlightKeys: Option[Map[String, ?]] = Option(result.highlight)
    val matchLanguage                         = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

    matchLanguage match {
      case Some(lang) =>
        Some(lang)
      case _ =>
        keyToLanguage(result.sourceAsMap.keys)
    }
  }

  /** Returns article summary from json string returned by elasticsearch. Will always return summary, even if language
    * does not exist in hitString. Language will be prioritized according to [[findByLanguageOrBestEffort]].
    * @param hitString
    *   Json string returned from elasticsearch for one article.
    * @param language
    *   Language to extract from the hitString.
    * @return
    *   Article summary extracted from hitString in specified language.
    */
  def hitAsArticleSummaryV2(hitString: String, language: String): ArticleSummaryV2DTO = {
    val searchableArticle = CirceUtil.unsafeParseAs[SearchableArticle](hitString)

    val titles        = searchableArticle.title.languageValues.map(lv => Title(lv.value, lv.language))
    val introductions =
      searchableArticle.introduction.languageValues.map(lv => Introduction(lv.value, lv.language))
    val metaDescriptions =
      searchableArticle.metaDescription.languageValues.map(lv => Description(lv.value, lv.language))
    val metaImages =
      searchableArticle.metaImage.map(image => ArticleMetaImage(image.imageId, image.altText, image.language))
    val visualElements =
      searchableArticle.visualElement.languageValues.map(lv => VisualElement(lv.value, lv.language))

    val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaImages)

    val title = findByLanguageOrBestEffort(titles, language)
      .map(toApiArticleTitle)
      .getOrElse(api.ArticleTitleDTO("", "", UnknownLanguage.toString))
    val visualElement   = findByLanguageOrBestEffort(visualElements, language).map(toApiVisualElement)
    val introduction    = findByLanguageOrBestEffort(introductions, language).map(toApiArticleIntroduction)
    val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).map(toApiArticleMetaDescription)
    val metaImage       = findByLanguageOrBestEffort(metaImages, language).map(toApiArticleMetaImage)
    val lastUpdated     = searchableArticle.lastUpdated
    val availability    = searchableArticle.availability

    ArticleSummaryV2DTO(
      searchableArticle.id,
      title,
      visualElement,
      introduction,
      metaDescription,
      metaImage,
      ApplicationUrl.get + searchableArticle.id.toString,
      searchableArticle.license,
      searchableArticle.articleType,
      lastUpdated,
      supportedLanguages,
      searchableArticle.grepCodes.getOrElse(List.empty),
      availability
    )
  }

  private[service] def oldToNewLicenseKey(license: String): String = {
    val licenses   = Map("nolaw" -> "CC0-1.0", "noc" -> "PD")
    val newLicense = licenses.getOrElse(license, license)

    if (getLicense(newLicense).isEmpty) {
      throw ImportException(s"License $license is not supported.")
    }
    newLicense
  }

  def updateExistingTagsField(existingTags: Seq[Tag], updatedTags: Seq[Tag]): Seq[Tag] = {
    val newTags    = updatedTags.filter(tag => existingTags.map(_.language).contains(tag.language))
    val tagsToKeep = existingTags.filterNot(tag => newTags.map(_.language).contains(tag.language))
    newTags ++ tagsToKeep
  }

  def updateExistingMetaDescriptionField(
      existingMetaDesc: Seq[Description],
      updatedMetaDesc: Seq[Description]
  ): Seq[Description] = {
    val newMetaDescriptions = updatedMetaDesc.filter(tag => existingMetaDesc.map(_.language).contains(tag.language))
    val metaDescToKeep = existingMetaDesc.filterNot(tag => newMetaDescriptions.map(_.language).contains(tag.language))
    newMetaDescriptions ++ metaDescToKeep
  }

  def updateArticleFields(existingArticle: Article, partialArticle: PartialPublishArticleDTO): Article = {
    val newAvailability = partialArticle.availability.getOrElse(existingArticle.availability)
    val newGrepCodes    = partialArticle.grepCodes.getOrElse(existingArticle.grepCodes)
    val newLicense      = partialArticle.license.getOrElse(existingArticle.copyright.license)

    val newMeta = partialArticle.metaDescription match {
      case Some(metaDesc) =>
        updateExistingMetaDescriptionField(
          existingArticle.metaDescription,
          metaDesc.map(m => Description(m.metaDescription, m.language))
        )
      case None => existingArticle.metaDescription
    }
    val newRelatedContent =
      partialArticle.relatedContent.map(toDomainRelatedContent).getOrElse(existingArticle.relatedContent)
    val newTags = partialArticle.tags match {
      case Some(tags) =>
        updateExistingTagsField(existingArticle.tags, tags.map(t => Tag(t.tags, t.language)))
      case None => existingArticle.tags
    }

    val newRevisionDate = partialArticle.revisionDate match {
      case Missing          => existingArticle.revisionDate
      case Delete           => None
      case UpdateWith(date) => Some(date)
    }
    val newPublishedDate = partialArticle.published.getOrElse(existingArticle.published)

    existingArticle.copy(
      availability = newAvailability,
      grepCodes = newGrepCodes,
      copyright = existingArticle.copyright.copy(license = newLicense),
      metaDescription = newMeta,
      relatedContent = newRelatedContent,
      tags = newTags,
      revisionDate = newRevisionDate,
      published = newPublishedDate
    )
  }

  private def toDomainRelatedContent(relatedContent: Seq[common.model.api.RelatedContent]): Seq[RelatedContent] = {
    relatedContent.map {
      case Left(x)  => Left(RelatedContentLink(url = x.url, title = x.title))
      case Right(x) => Right(x)
    }
  }

  def toDomainCopyright(copyright: model.api.CopyrightDTO): Copyright = {
    Copyright(
      copyright.license.license,
      copyright.origin,
      copyright.creators.map(_.toDomain),
      copyright.processors.map(_.toDomain),
      copyright.rightsholders.map(_.toDomain),
      copyright.validFrom,
      copyright.validTo,
      copyright.processed
    )
  }

  private def getMainNidUrlToOldNdla(id: Long): Option[String] = {
    // First nid in externalId's should always be mainNid after import.
    articleRepository.getExternalIdsFromId(id).map(createLinkToOldNdla).headOption
  }

  private def getSupportedArticleLanguages(article: Article): Seq[String] = {
    getSupportedLanguages(
      article.title,
      article.visualElement,
      article.introduction,
      article.metaDescription,
      article.tags,
      article.content,
      article.metaImage
    )
  }

  def toApiArticleV2(
      article: Article,
      language: String,
      fallback: Boolean
  ): Try[api.ArticleV2DTO] = {
    val supportedLanguages = getSupportedArticleLanguages(article)
    val isLanguageNeutral  = supportedLanguages.contains(UnknownLanguage.toString) && supportedLanguages.length == 1

    if (supportedLanguages.contains(language) || language == AllLanguages || isLanguageNeutral || fallback) {
      val meta = findByLanguageOrBestEffort(article.metaDescription, language)
        .map(toApiArticleMetaDescription)
        .getOrElse(ArticleMetaDescriptionDTO("", UnknownLanguage.toString))
      val tags = findByLanguageOrBestEffort(article.tags, language)
        .map(toApiArticleTag)
        .getOrElse(ArticleTagDTO(Seq(), UnknownLanguage.toString))
      val title = findByLanguageOrBestEffort(article.title, language)
        .map(toApiArticleTitle)
        .getOrElse(api.ArticleTitleDTO("", "", UnknownLanguage.toString))
      val introduction   = findByLanguageOrBestEffort(article.introduction, language).map(toApiArticleIntroduction)
      val visualElement  = findByLanguageOrBestEffort(article.visualElement, language).map(toApiVisualElement)
      val articleContent = findByLanguageOrBestEffort(article.content, language)
        .map(toApiArticleContentV2)
        .getOrElse(api.ArticleContentV2DTO("", UnknownLanguage.toString))
      val metaImage  = findByLanguageOrBestEffort(article.metaImage, language).map(toApiArticleMetaImage)
      val copyright  = toApiCopyright(article.copyright)
      val disclaimer = article.disclaimer
        .findByLanguageOrBestEffort(language)
        .map(DisclaimerDTO.fromLanguageValue)

      Success(
        api.ArticleV2DTO(
          id = article.id.get,
          oldNdlaUrl = article.id.flatMap(getMainNidUrlToOldNdla),
          revision = article.revision.get,
          title = title,
          content = articleContent,
          copyright = copyright,
          tags = tags,
          requiredLibraries = article.requiredLibraries.map(toApiRequiredLibrary),
          visualElement = visualElement,
          metaImage = metaImage,
          introduction = introduction,
          metaDescription = meta,
          created = article.created,
          updated = article.updated,
          updatedBy = article.updatedBy,
          published = article.published,
          articleType = article.articleType.entryName,
          supportedLanguages = supportedLanguages,
          grepCodes = article.grepCodes,
          conceptIds = article.conceptIds,
          availability = article.availability.toString,
          relatedContent = article.relatedContent.map(toApiRelatedContent),
          revisionDate = article.revisionDate,
          slug = article.slug,
          disclaimer = disclaimer,
          traits = article.traits
        )
      )
    } else {
      Failure(
        NotFoundException(
          s"The article with id ${article.id.get} and language $language was not found",
          supportedLanguages
        )
      )
    }
  }

  def toApiArticleTitle(title: Title): api.ArticleTitleDTO = {
    api.ArticleTitleDTO(Jsoup.parseBodyFragment(title.title).body().text(), title.title, title.language)
  }

  private def toApiArticleContentV2(content: ArticleContent): api.ArticleContentV2DTO = {
    api.ArticleContentV2DTO(
      content.content,
      content.language
    )
  }

  private def toApiRelatedContent(relatedContent: RelatedContent): common.model.api.RelatedContent = {
    relatedContent match {
      case Left(x)  => Left(common.model.api.RelatedContentLinkDTO(url = x.url, title = x.title))
      case Right(x) => Right(x)
    }

  }

  private def toApiCopyright(copyright: Copyright): commonApi.CopyrightDTO = {
    commonApi.CopyrightDTO(
      toApiLicense(copyright.license),
      copyright.origin,
      copyright.creators.map(_.toApi),
      copyright.processors.map(_.toApi),
      copyright.rightsholders.map(_.toApi),
      copyright.validFrom,
      copyright.validTo,
      copyright.processed
    )
  }

  def toApiLicense(shortLicense: String): LicenseDTO = {
    getLicense(shortLicense) match {
      case Some(l) => model.api.LicenseDTO(l.license.toString, Option(l.description), l.url)
      case None    => model.api.LicenseDTO("unknown", None, None)
    }
  }

  private def toApiArticleTag(tag: Tag): ArticleTagDTO = {
    article.ArticleTagDTO(tag.tags, tag.language)
  }

  private def toApiRequiredLibrary(required: RequiredLibrary): api.RequiredLibraryDTO = {
    api.RequiredLibraryDTO(required.mediaType, required.name, required.url)
  }

  private def toApiVisualElement(visual: VisualElement): api.VisualElementDTO = {
    api.VisualElementDTO(visual.resource, visual.language)
  }

  def toApiArticleIntroduction(intro: Introduction): api.ArticleIntroductionDTO = {
    api.ArticleIntroductionDTO(
      Jsoup.parseBodyFragment(intro.introduction).body().text(),
      intro.introduction,
      intro.language
    )
  }

  private def toApiArticleMetaDescription(metaDescription: Description): ArticleMetaDescriptionDTO = {
    article.ArticleMetaDescriptionDTO(metaDescription.content, metaDescription.language)
  }

  private def toApiArticleMetaImage(metaImage: ArticleMetaImage): api.ArticleMetaImageDTO = {
    api.ArticleMetaImageDTO(
      s"${props.externalApiUrls("raw-image")}/${metaImage.imageId}",
      metaImage.altText,
      metaImage.language
    )
  }

  private def createLinkToOldNdla(nodeId: String): String = s"//red.ndla.no/node/$nodeId"

  def toApiArticleIds(ids: ArticleIds): api.ArticleIdsDTO = api.ArticleIdsDTO(ids.articleId, ids.externalId)

  def toApiArticleTags(
      tags: Seq[String],
      tagsCount: Long,
      pageSize: Int,
      offset: Int,
      language: String
  ): api.TagsSearchResultDTO = {
    api.TagsSearchResultDTO(tagsCount, offset, pageSize, language, tags)
  }
}
