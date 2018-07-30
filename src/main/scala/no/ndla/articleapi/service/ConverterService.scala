/*
 * Part of NDLA article_api.
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
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.{ArticleSummaryV2, ImportException, NotFoundException}
import no.ndla.articleapi.model.domain.Language._
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.mapping.ISO639
import no.ndla.mapping.License.getLicense
import no.ndla.network.ApplicationUrl
import no.ndla.validation.{EmbedTagRules, HtmlTagRules, ResourceType, TagAttributes}
import no.ndla.validation.HtmlTagRules.{jsoupDocumentToString, stringToJsoupDocument}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.read

import scala.collection.JavaConverters._
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
        val keyLanguages = keys.toList.flatMap(key => key.split('.').toList match {
          case _ :: language :: _ => Some(language)
          case _ => None
        })

        keyLanguages.sortBy(lang => {
          ISO639.languagePriority.reverse.indexOf(lang)
        }).lastOption
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
      val introductions = searchableArticle.introduction.languageValues.map(lv => ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions = searchableArticle.metaDescription.languageValues.map(lv => ArticleMetaDescription(lv.value, lv.language))
      val metaImages = searchableArticle.metaImage.map(
        image => ArticleMetaImage(image.imageId, image.altText, image.language))
      val visualElements = searchableArticle.visualElement.languageValues.map(lv => VisualElement(lv.value, lv.language))

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions)

      val title = findByLanguageOrBestEffort(titles, language).map(toApiArticleTitle).getOrElse(api.ArticleTitle("", UnknownLanguage))
      val visualElement = findByLanguageOrBestEffort(visualElements, language).map(toApiVisualElement)
      val introduction = findByLanguageOrBestEffort(introductions, language).map(toApiArticleIntroduction)
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).map(toApiArticleMetaDescription)
      val metaImage = findByLanguageOrBestEffort(metaImages, language).map(toApiArticleMetaImage)
      val lastUpdated = searchableArticle.lastUpdated

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
        supportedLanguages
      )
    }

    private[service] def oldToNewLicenseKey(license: String): String = {
      val licenses = Map("nolaw" -> "cc0", "noc" -> "pd")
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

      (creatorMap(author.`type`.toLowerCase), processorMap(author.`type`.toLowerCase), rightsholderMap(author.`type`.toLowerCase)) match {
        case (t: String, _, _) => Author(t.capitalize, author.name)
        case (_, t: String, _) => Author(t.capitalize, author.name)
        case (_, _, t: String) => Author(t.capitalize, author.name)
        case (_, _, _) => Author(author.`type`, author.name)
      }
    }

    def toDomainConcept(concept: api.NewConcept): Concept = {
      Concept(
        None,
        Seq(ConceptTitle(concept.title, concept.language)),
        concept.content.map(content => Seq(ConceptContent(content, concept.language))).getOrElse(Seq.empty),
        concept.copyright.map(toDomainCopyright),
        clock.now(),
        clock.now()
      )
    }

    def toDomainConcept(toMergeInto: Concept, updateConcept: api.UpdatedConcept): Concept = {
      val domainTitle = updateConcept.title.map(t => ConceptTitle(t, updateConcept.language)).toSeq
      val domainContent = updateConcept.content.map(c => ConceptContent(c, updateConcept.language)).toSeq

      toMergeInto.copy(
        title=mergeLanguageFields(toMergeInto.title, domainTitle),
        content=mergeLanguageFields(toMergeInto.content, domainContent),
        copyright=updateConcept.copyright.map(toDomainCopyright).orElse(toMergeInto.copyright),
        created=toMergeInto.created,
        updated=clock.now()
      )
    }

    def toDomainArticle(toMergeInto: Article, updatedApiArticle: api.UpdatedArticleV2): Article = {
      val lang = updatedApiArticle.language
      toMergeInto.copy(
        revision = Option(updatedApiArticle.revision),
        title = mergeLanguageFields(toMergeInto.title, updatedApiArticle.title.toSeq.map(t => converterService.toDomainTitle(api.ArticleTitle(t, lang)))),
        content = mergeLanguageFields(toMergeInto.content, updatedApiArticle.content.toSeq.map(c => converterService.toDomainContent(api.ArticleContentV2(c, lang)))),
        copyright = updatedApiArticle.copyright.map(c => converterService.toDomainCopyright(c)).getOrElse(toMergeInto.copyright),
        tags = mergeTags(toMergeInto.tags, converterService.toDomainTagV2(updatedApiArticle.tags, lang)),
        requiredLibraries = updatedApiArticle.requiredLibraries.map(converterService.toDomainRequiredLibraries),
        visualElement = mergeLanguageFields(toMergeInto.visualElement, updatedApiArticle.visualElement.map(c => converterService.toDomainVisualElementV2(Some(c), lang)).getOrElse(Seq())),
        introduction = mergeLanguageFields(toMergeInto.introduction, updatedApiArticle.introduction.map(i => converterService.toDomainIntroductionV2(Some(i), lang)).getOrElse(Seq())),
        metaDescription = mergeLanguageFields(toMergeInto.metaDescription, updatedApiArticle.metaDescription.map(m => converterService.toDomainMetaDescriptionV2(Some(m), lang)).getOrElse(Seq())),
        metaImage = mergeLanguageFields(toMergeInto.metaImage, updatedApiArticle.metaImage.map(m => toDomainMetaImage(m.id, m.alt, lang)).toSeq),
        updated = clock.now(),
        updatedBy = authUser.userOrClientid()
      )
    }

    private[service] def mergeLanguageFields[A <: LanguageField](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.isEmpty)
    }

    private def mergeTags(existing: Seq[ArticleTag], updated: Seq[ArticleTag]): Seq[ArticleTag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    private[service] def toDomainCopyright(license: String, authors: Seq[Author]): Copyright = {
      val origin = authors.find(author => author.`type`.toLowerCase == "opphavsmann").map(_.name).getOrElse("")


      val authorsExcludingOrigin = authors.filterNot(x => x.name != origin && x.`type` == "opphavsmann")
      val creators = authorsExcludingOrigin.map(toNewAuthorType).filter(a => creatorTypes.contains(a.`type`.toLowerCase))
      val processors = authorsExcludingOrigin.map(toNewAuthorType).filter(a => processorTypes.contains(a.`type`.toLowerCase))
      val rightsholders = authorsExcludingOrigin.map(toNewAuthorType).filter(a => rightsholderTypes.contains(a.`type`.toLowerCase))
      Copyright(oldToNewLicenseKey(license), origin, creators, processors, rightsholders, None, None, None)
    }

    def toDomainArticle(newArticle: api.NewArticleV2): Article = {
      val domainTitle = Seq(ArticleTitle(newArticle.title, newArticle.language))
      val domainContent = Seq(ArticleContent(
        removeUnknownEmbedTagAttributes(newArticle.content),
        newArticle.language)
      )

      Article(
        id=None,
        revision=None,
        title=domainTitle,
        content=domainContent,
        copyright=toDomainCopyright(newArticle.copyright),
        tags=toDomainTagV2(newArticle.tags, newArticle.language),
        requiredLibraries=newArticle.requiredLibraries.getOrElse(Seq()).map(toDomainRequiredLibraries),
        visualElement=toDomainVisualElementV2(newArticle.visualElement, newArticle.language),
        introduction=toDomainIntroductionV2(newArticle.introduction, newArticle.language),
        metaDescription=toDomainMetaDescriptionV2(newArticle.metaDescription, newArticle.language),
        metaImage=newArticle.metaImage.map(meta => toDomainMetaImage(meta.id, meta.alt, newArticle.language)).toSeq,
        created=clock.now(),
        updated=clock.now(),
        updatedBy=authUser.userOrClientid(),
        newArticle.articleType
      )
    }

    def withAgreementCopyright(article: Article): Article = {
      val agreementCopyright = article.copyright.agreementId.flatMap(aid =>
        draftApiClient.getAgreementCopyright(aid).map(toDomainCopyright)
      ).getOrElse(article.copyright)

      article.copy(copyright = article.copyright.copy(
        license = agreementCopyright.license,
        creators = agreementCopyright.creators,
        rightsholders = agreementCopyright.rightsholders,
        validFrom = agreementCopyright.validFrom,
        validTo = agreementCopyright.validTo
      ))
    }

    def withAgreementCopyright(copyright: api.Copyright): api.Copyright = {
      val agreementCopyright = copyright.agreementId.flatMap(aid => draftApiClient.getAgreementCopyright(aid)).getOrElse(copyright)
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

    def toDomainMetaDescription(meta: api.ArticleMetaDescription): ArticleMetaDescription = ArticleMetaDescription(meta.metaDescription, meta.language)

    def toDomainMetaImage(imageId: String, altText: String, language: String): ArticleMetaImage = ArticleMetaImage(imageId, altText, language)

    def toDomainMetaDescriptionV2(meta: Option[String], language: String): Seq[ArticleMetaDescription] = {
      if (meta.isEmpty) {
        Seq.empty[ArticleMetaDescription]
      } else {
        Seq(ArticleMetaDescription(meta.getOrElse(""), language))
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
        copyright.validTo)
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
      document.select("embed").asScala.map(el => {
        ResourceType.valueOf(el.attr(TagAttributes.DataResource.toString))
          .map(EmbedTagRules.attributesForResourceType)
          .map(knownAttributes => HtmlTagRules.removeIllegalAttributes(el, knownAttributes.all.map(_.toString)))
      })

      jsoupDocumentToString(document)
    }

    def toApiArticleV2(article: Article, language: String, fallback: Boolean = false): Try[api.ArticleV2] = {
      val supportedLanguages = getSupportedLanguages(
        article.title, article.visualElement, article.introduction, article.metaDescription, article.tags, article.content
      )
      val isLanguageNeutral = supportedLanguages.contains(UnknownLanguage) && supportedLanguages.length == 1

      if (supportedLanguages.contains(language) || language == AllLanguages || isLanguageNeutral || fallback) {
        val meta = findByLanguageOrBestEffort(article.metaDescription, language).map(toApiArticleMetaDescription).getOrElse(api.ArticleMetaDescription("", UnknownLanguage))
        val tags = findByLanguageOrBestEffort(article.tags, language).map(toApiArticleTag).getOrElse(api.ArticleTag(Seq(), UnknownLanguage))
        val title = findByLanguageOrBestEffort(article.title, language).map(toApiArticleTitle).getOrElse(api.ArticleTitle("", UnknownLanguage))
        val introduction = findByLanguageOrBestEffort(article.introduction, language).map(toApiArticleIntroduction)
        val visualElement = findByLanguageOrBestEffort(article.visualElement, language).map(toApiVisualElement)
        val articleContent = findByLanguageOrBestEffort(article.content, language).map(toApiArticleContentV2).getOrElse(api.ArticleContentV2("", UnknownLanguage))
        val metaImage = findByLanguageOrBestEffort(article.metaImage, language).map(toApiArticleMetaImage)

        Success(api.ArticleV2(
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
          article.articleType,
          supportedLanguages
        ))
      } else  {
        Failure(NotFoundException(s"The article with id ${article.id.get} and language $language was not found", supportedLanguages))
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
        case Some(l) => api.License(l.license, Option(l.description), l.url)
        case None => api.License("unknown", None, None)
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
      api.ArticleMetaImage(s"${externalApiUrls("raw-image")}/${metaImage.imageId}", metaImage.altText, metaImage.language)
    }

    def createLinkToOldNdla(nodeId: String): String = s"//red.ndla.no/node/$nodeId"

    def toApiConcept(concept: Concept, language: String, fallback: Boolean): Try[api.Concept] = {
      val supportedLanguages = getSupportedLanguages(
        concept.title, concept.content
      )

      if (supportedLanguages.contains(language) || language == AllLanguages || fallback) {
        val title = findByLanguageOrBestEffort(concept.title, language).map(toApiConceptTitle).getOrElse(api.ConceptTitle("", Language.UnknownLanguage))
        val content = findByLanguageOrBestEffort(concept.content, language).map(toApiConceptContent).getOrElse(api.ConceptContent("", Language.UnknownLanguage))

        Success(api.Concept(
          concept.id.get,
          title,
          content,
          concept.copyright.map(toApiCopyright),
          concept.created,
          concept.updated,
          supportedLanguages
        ))
      } else {
        Failure(NotFoundException(s"The concept with id ${concept.id.get} and language $language was not found", supportedLanguages))
      }

    }

    def toApiConceptTitle(title: ConceptTitle): api.ConceptTitle = api.ConceptTitle(title.title, title.language)

    def toApiConceptContent(title: ConceptContent): api.ConceptContent = api.ConceptContent(title.content, title.language)

    def toApiArticleIds(ids: ArticleIds): api.ArticleIds = api.ArticleIds(ids.articleId, ids.externalId)

  }
}
