/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import com.sksamuel.elastic4s.http.search.SearchHit
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties._
import no.ndla.articleapi.auth.User
import no.ndla.articleapi.integration.DraftApiClient
import no.ndla.articleapi.model.{api, domain}
import no.ndla.articleapi.model.api.{ArticleSummaryV2, ImportException, NotFoundException, PartialPublishArticle}
import no.ndla.articleapi.model.domain.Language._
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.mapping.ISO639
import no.ndla.mapping.License.getLicense
import no.ndla.network.ApplicationUrl
import no.ndla.validation.HtmlTagRules.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.validation.{EmbedTagRules, HtmlTagRules, ResourceType, TagAttributes}
import org.json4s._
import org.json4s.native.Serialization.read

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: Clock with ArticleRepository with DraftApiClient with User =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    /**
      * Attempts to extract language that hit from highlights in elasticsearch response.
      * @param result Elasticsearch hit.
      * @return Language if found.
      */
    def getLanguageFromHit(result: SearchHit): Option[String] = {
      def keyToLanguage(keys: Iterable[String]): Option[String] = {
        val keyLanguages = keys.toList.flatMap(key =>
          key.split('.').toList match {
            case _ :: language :: _ => Some(language)
            case _                  => None
        })

        keyLanguages
          .sortBy(lang => {
            ISO639.languagePriority.reverse.indexOf(lang)
          })
          .lastOption
      }

      val highlightKeys: Option[Map[String, _]] = Option(result.highlight)
      val matchLanguage = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          keyToLanguage(result.sourceAsMap.keys)
      }
    }

    /**
      * Returns article summary from json string returned by elasticsearch.
      * Will always return summary, even if language does not exist in hitString.
      * Language will be prioritized according to [[findByLanguageOrBestEffort]].
      * @param hitString Json string returned from elasticsearch for one article.
      * @param language Language to extract from the hitString.
      * @return Article summary extracted from hitString in specified language.
      */
    def hitAsArticleSummaryV2(hitString: String, language: String): ArticleSummaryV2 = {

      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableArticle = read[SearchableArticle](hitString)

      val titles = searchableArticle.title.languageValues.map(lv => ArticleTitle(lv.value, lv.language))
      val introductions =
        searchableArticle.introduction.languageValues.map(lv => ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions =
        searchableArticle.metaDescription.languageValues.map(lv => ArticleMetaDescription(lv.value, lv.language))
      val metaImages =
        searchableArticle.metaImage.map(image => ArticleMetaImage(image.imageId, image.altText, image.language))
      val visualElements =
        searchableArticle.visualElement.languageValues.map(lv => VisualElement(lv.value, lv.language))

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaImages)

      val title = findByLanguageOrBestEffort(titles, language)
        .map(toApiArticleTitle)
        .getOrElse(api.ArticleTitle("", UnknownLanguage.toString))
      val visualElement = findByLanguageOrBestEffort(visualElements, language).map(toApiVisualElement)
      val introduction = findByLanguageOrBestEffort(introductions, language).map(toApiArticleIntroduction)
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).map(toApiArticleMetaDescription)
      val metaImage = findByLanguageOrBestEffort(metaImages, language).map(toApiArticleMetaImage)
      val lastUpdated = searchableArticle.lastUpdated
      val availability = searchableArticle.availability

      ArticleSummaryV2(
        searchableArticle.id,
        title,
        visualElement,
        introduction,
        metaDescription,
        metaImage,
        ApplicationUrl.get + searchableArticle.id.toString,
        searchableArticle.license,
        searchableArticle.articleType,
        lastUpdated.toDate,
        supportedLanguages,
        searchableArticle.grepCodes,
        availability,
      )
    }

    private[service] def oldToNewLicenseKey(license: String): String = {
      val licenses = Map("nolaw" -> "CC0-1.0", "noc" -> "PD")
      val newLicense = licenses.getOrElse(license, license)

      if (getLicense(newLicense).isEmpty) {
        throw ImportException(s"License $license is not supported.")
      }
      newLicense
    }

    private def toNewAuthorType(author: Author): Author = {
      val creatorMap = (oldCreatorTypes zip creatorTypes).toMap.withDefaultValue(None)
      val processorMap = (oldProcessorTypes zip processorTypes).toMap.withDefaultValue(None)
      val rightsholderMap = (oldRightsholderTypes zip rightsholderTypes).toMap.withDefaultValue(None)

      (creatorMap(author.`type`.toLowerCase),
       processorMap(author.`type`.toLowerCase),
       rightsholderMap(author.`type`.toLowerCase)) match {
        case (t: String, _, _) => Author(t.capitalize, author.name)
        case (_, t: String, _) => Author(t.capitalize, author.name)
        case (_, _, t: String) => Author(t.capitalize, author.name)
        case (_, _, _)         => Author(author.`type`, author.name)
      }
    }

    private[service] def mergeLanguageFields[A <: LanguageField[_]](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.isEmpty)
    }

    private[service] def mergeTags(existing: Seq[ArticleTag], updated: Seq[ArticleTag]): Seq[ArticleTag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    def updateExistingTagsField(existingTags: Seq[ArticleTag], updatedTags: Seq[ArticleTag]): Seq[ArticleTag] = {
      val newTags = updatedTags.filter(tag => existingTags.map(_.language).contains(tag.language))
      val tagsToKeep = existingTags.filterNot(tag => newTags.map(_.language).contains(tag.language))
      newTags ++ tagsToKeep
    }

    def updateExistingMetaDescriptionField(
        existingMetaDesc: Seq[ArticleMetaDescription],
        updatedMetaDesc: Seq[ArticleMetaDescription]): Seq[ArticleMetaDescription] = {
      val newMetaDescriptions = updatedMetaDesc.filter(tag => existingMetaDesc.map(_.language).contains(tag.language))
      val metaDescToKeep = existingMetaDesc.filterNot(tag => newMetaDescriptions.map(_.language).contains(tag.language))
      newMetaDescriptions ++ metaDescToKeep
    }

    def updateArticleFields(existingArticle: Article, partialArticle: PartialPublishArticle): Article = {
      val newAvailability = partialArticle.availability.getOrElse(existingArticle.availability)
      val newGrepCodes = partialArticle.grepCodes.getOrElse(existingArticle.grepCodes)
      val newLicense = partialArticle.license.getOrElse(existingArticle.copyright.license)

      val newMeta = partialArticle.metaDescription match {
        case Some(metaDesc) =>
          updateExistingMetaDescriptionField(existingArticle.metaDescription, metaDesc)
        case None => existingArticle.metaDescription
      }
      val newRelatedContent =
        partialArticle.relatedContent.map(toDomainRelatedContent).getOrElse(existingArticle.relatedContent)
      val newTags = partialArticle.tags match {
        case Some(tags) => updateExistingTagsField(existingArticle.tags, tags)
        case None       => existingArticle.tags
      }
      existingArticle.copy(
        availability = newAvailability,
        grepCodes = newGrepCodes,
        copyright = existingArticle.copyright.copy(license = newLicense),
        metaDescription = newMeta,
        relatedContent = newRelatedContent,
        tags = newTags
      )
    }

    private[service] def toDomainCopyright(license: String, authors: Seq[Author]): Copyright = {
      val origin = authors.find(author => author.`type`.toLowerCase == "opphavsmann").map(_.name).getOrElse("")

      val authorsExcludingOrigin = authors.filterNot(x => x.name != origin && x.`type` == "opphavsmann")
      val creators =
        authorsExcludingOrigin.map(toNewAuthorType).filter(a => creatorTypes.contains(a.`type`.toLowerCase))
      val processors =
        authorsExcludingOrigin.map(toNewAuthorType).filter(a => processorTypes.contains(a.`type`.toLowerCase))
      val rightsholders =
        authorsExcludingOrigin.map(toNewAuthorType).filter(a => rightsholderTypes.contains(a.`type`.toLowerCase))
      Copyright(oldToNewLicenseKey(license), origin, creators, processors, rightsholders, None, None, None)
    }

    def withAgreementCopyright(article: Article): Article = {
      val agreementCopyright = article.copyright.agreementId
        .flatMap(aid => draftApiClient.getAgreementCopyright(aid).map(toDomainCopyright))
        .getOrElse(article.copyright)

      article.copy(
        copyright = article.copyright.copy(
          license = agreementCopyright.license,
          creators = agreementCopyright.creators,
          rightsholders = agreementCopyright.rightsholders,
          validFrom = agreementCopyright.validFrom,
          validTo = agreementCopyright.validTo
        ))
    }

    def withAgreementCopyright(copyright: api.Copyright): api.Copyright = {
      val agreementCopyright =
        copyright.agreementId.flatMap(aid => draftApiClient.getAgreementCopyright(aid)).getOrElse(copyright)
      copyright.copy(
        license = agreementCopyright.license,
        creators = agreementCopyright.creators,
        rightsholders = agreementCopyright.rightsholders,
        validFrom = agreementCopyright.validFrom,
        validTo = agreementCopyright.validTo
      )
    }

    def toDomainTitle(articleTitle: api.ArticleTitle): ArticleTitle = {
      ArticleTitle(articleTitle.title, articleTitle.language)
    }

    def toDomainContent(articleContent: api.ArticleContentV2): ArticleContent = {
      ArticleContent(removeUnknownEmbedTagAttributes(articleContent.content), articleContent.language)
    }

    def toDomainTag(tag: api.ArticleTag): ArticleTag = {
      ArticleTag(tag.tags, tag.language)
    }

    def toDomainTagV2(tag: Seq[String], language: String): Seq[ArticleTag] = {
      if (tag.isEmpty) {
        Seq.empty[ArticleTag]
      } else {
        Seq(ArticleTag(tag, language))
      }
    }

    def toDomainVisualElement(visual: api.VisualElement): VisualElement = {
      VisualElement(removeUnknownEmbedTagAttributes(visual.visualElement), visual.language)
    }

    def toDomainVisualElementV2(visual: Option[String], language: String): Seq[VisualElement] = {
      if (visual.isEmpty) {
        Seq.empty[VisualElement]
      } else {
        Seq(VisualElement(removeUnknownEmbedTagAttributes(visual.getOrElse("")), language))
      }
    }

    def toDomainIntroduction(intro: api.ArticleIntroduction): ArticleIntroduction = {
      ArticleIntroduction(intro.introduction, intro.language)
    }

    def toDomainIntroductionV2(intro: Option[String], language: String): Seq[ArticleIntroduction] = {
      if (intro.isEmpty) {
        Seq.empty[ArticleIntroduction]
      } else {
        Seq(ArticleIntroduction(intro.getOrElse(""), language))
      }
    }

    def toDomainMetaDescription(meta: api.ArticleMetaDescription): ArticleMetaDescription =
      ArticleMetaDescription(meta.metaDescription, meta.language)

    def toDomainMetaImage(imageId: String, altText: String, language: String): ArticleMetaImage =
      ArticleMetaImage(imageId, altText, language)

    def toDomainMetaDescriptionV2(meta: Option[String], language: String): Seq[ArticleMetaDescription] = {
      if (meta.isEmpty) {
        Seq.empty[ArticleMetaDescription]
      } else {
        Seq(ArticleMetaDescription(meta.getOrElse(""), language))
      }
    }

    def toDomainRelatedContent(relatedContent: Seq[api.RelatedContent]): Seq[RelatedContent] = {
      relatedContent.map {
        case Left(x)  => Left(domain.RelatedContentLink(url = x.url, title = x.title))
        case Right(x) => Right(x)
      }
    }

    def toDomainCopyright(copyright: api.Copyright): Copyright = {
      Copyright(
        copyright.license.license,
        copyright.origin,
        copyright.creators.map(toDomainAuthor),
        copyright.processors.map(toDomainAuthor),
        copyright.rightsholders.map(toDomainAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
      )
    }

    def toDomainAuthor(author: api.Author): Author = Author(author.`type`, author.name)

    def toDomainRequiredLibraries(requiredLibs: api.RequiredLibrary): RequiredLibrary = {
      RequiredLibrary(requiredLibs.mediaType, requiredLibs.name, requiredLibs.url)
    }

    private def getMainNidUrlToOldNdla(id: Long): Option[String] = {
      // First nid in externalId's should always be mainNid after import.
      articleRepository.getExternalIdsFromId(id).map(createLinkToOldNdla).headOption
    }

    private def removeUnknownEmbedTagAttributes(html: String): String = {
      val document = stringToJsoupDocument(html)
      document
        .select("embed")
        .asScala
        .map(el => {
          ResourceType
            .valueOf(el.attr(TagAttributes.DataResource.toString))
            .map(EmbedTagRules.attributesForResourceType)
            .map(knownAttributes => HtmlTagRules.removeIllegalAttributes(el, knownAttributes.all.map(_.toString)))
        })

      jsoupDocumentToString(document)
    }

    def getSupportedArticleLanguages(article: Article): Seq[String] = {
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

    def toApiArticleV2(article: Article, language: String, fallback: Boolean = false): Try[api.ArticleV2] = {
      val supportedLanguages = getSupportedArticleLanguages(article)
      val isLanguageNeutral = supportedLanguages.contains(UnknownLanguage.toString) && supportedLanguages.length == 1

      if (supportedLanguages.contains(language) || language == AllLanguages || isLanguageNeutral || fallback) {
        val meta = findByLanguageOrBestEffort(article.metaDescription, language)
          .map(toApiArticleMetaDescription)
          .getOrElse(api.ArticleMetaDescription("", UnknownLanguage.toString))
        val tags = findByLanguageOrBestEffort(article.tags, language)
          .map(toApiArticleTag)
          .getOrElse(api.ArticleTag(Seq(), UnknownLanguage.toString))
        val title = findByLanguageOrBestEffort(article.title, language)
          .map(toApiArticleTitle)
          .getOrElse(api.ArticleTitle("", UnknownLanguage.toString))
        val introduction = findByLanguageOrBestEffort(article.introduction, language).map(toApiArticleIntroduction)
        val visualElement = findByLanguageOrBestEffort(article.visualElement, language).map(toApiVisualElement)
        val articleContent = findByLanguageOrBestEffort(article.content, language)
          .map(toApiArticleContentV2)
          .getOrElse(api.ArticleContentV2("", UnknownLanguage.toString))
        val metaImage = findByLanguageOrBestEffort(article.metaImage, language).map(toApiArticleMetaImage)

        Success(
          api.ArticleV2(
            article.id.get,
            article.id.flatMap(getMainNidUrlToOldNdla),
            article.revision.get,
            title,
            articleContent,
            withAgreementCopyright(toApiCopyright(article.copyright)),
            tags,
            article.requiredLibraries.map(toApiRequiredLibrary),
            visualElement,
            metaImage,
            introduction,
            meta,
            article.created,
            article.updated,
            article.updatedBy,
            article.published,
            article.articleType,
            supportedLanguages,
            article.grepCodes,
            article.conceptIds,
            availability = article.availability.toString,
            article.relatedContent.map(toApiRelatedContent)
          ))
      } else {
        Failure(
          NotFoundException(s"The article with id ${article.id.get} and language $language was not found",
                            supportedLanguages))
      }
    }

    def toApiArticleTitle(title: ArticleTitle): api.ArticleTitle = {
      api.ArticleTitle(title.title, title.language)
    }

    def toApiArticleContentV2(content: ArticleContent): api.ArticleContentV2 = {
      api.ArticleContentV2(
        content.content,
        content.language
      )
    }

    def toApiRelatedContent(relatedContent: domain.RelatedContent): api.RelatedContent = {
      relatedContent match {
        case Left(x)  => Left(api.RelatedContentLink(url = x.url, title = x.title))
        case Right(x) => Right(x)
      }

    }

    def toApiCopyright(copyright: Copyright): api.Copyright = {
      api.Copyright(
        toApiLicense(copyright.license),
        copyright.origin,
        copyright.creators.map(toApiAuthor),
        copyright.processors.map(toApiAuthor),
        copyright.rightsholders.map(toApiAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
      )
    }

    def toApiLicense(shortLicense: String): api.License = {
      getLicense(shortLicense) match {
        case Some(l) => api.License(l.license.toString, Option(l.description), l.url)
        case None    => api.License("unknown", None, None)
      }
    }

    def toApiAuthor(author: Author): api.Author = {
      api.Author(author.`type`, author.name)
    }

    def toApiArticleTag(tag: ArticleTag): api.ArticleTag = {
      api.ArticleTag(tag.tags, tag.language)
    }

    def toApiRequiredLibrary(required: RequiredLibrary): api.RequiredLibrary = {
      api.RequiredLibrary(required.mediaType, required.name, required.url)
    }

    def toApiVisualElement(visual: VisualElement): api.VisualElement = {
      api.VisualElement(visual.resource, visual.language)
    }

    def toApiArticleIntroduction(intro: ArticleIntroduction): api.ArticleIntroduction = {
      api.ArticleIntroduction(intro.introduction, intro.language)
    }

    def toApiArticleMetaDescription(metaDescription: ArticleMetaDescription): api.ArticleMetaDescription = {
      api.ArticleMetaDescription(metaDescription.content, metaDescription.language)
    }

    def toApiArticleMetaImage(metaImage: ArticleMetaImage): api.ArticleMetaImage = {
      api.ArticleMetaImage(s"${externalApiUrls("raw-image")}/${metaImage.imageId}",
                           metaImage.altText,
                           metaImage.language)
    }

    def createLinkToOldNdla(nodeId: String): String = s"//red.ndla.no/node/$nodeId"

    def toApiArticleIds(ids: ArticleIds): api.ArticleIds = api.ArticleIds(ids.articleId, ids.externalId)

    def toApiArticleTags(tags: Seq[String],
                         tagsCount: Int,
                         pageSize: Int,
                         offset: Int,
                         language: String): api.TagsSearchResult = {
      api.TagsSearchResult(tagsCount, offset, pageSize, language, tags)
    }

  }
}
