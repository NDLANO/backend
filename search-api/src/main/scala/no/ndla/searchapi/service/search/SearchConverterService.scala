/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.http.search.SearchHit
import com.typesafe.scalalogging.LazyLogging
import no.ndla.language.model.Iso639
import no.ndla.mapping.ISO639
import no.ndla.mapping.License.getLicense
import no.ndla.searchapi.SearchApiProperties
import no.ndla.searchapi.SearchApiProperties.DefaultLanguage
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api._
import no.ndla.searchapi.model.api.article.ArticleSummary
import no.ndla.searchapi.model.api.draft.DraftSummary
import no.ndla.searchapi.model.api.learningpath.LearningPathSummary
import no.ndla.searchapi.model.domain.Language
import no.ndla.searchapi.model.domain.Language.{findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.searchapi.model.domain.article.{LearningResourceType, _}
import no.ndla.searchapi.model.domain.draft.Draft
import no.ndla.searchapi.model.domain.learningpath.{LearningPath, LearningStep, StepType}
import no.ndla.searchapi.model.grep._
import no.ndla.searchapi.model.search._
import no.ndla.searchapi.model.taxonomy._
import no.ndla.searchapi.model.{api, domain, search}
import no.ndla.searchapi.service.ConverterService
import org.json4s.Formats
import org.json4s.native.Serialization.read
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait SearchConverterService {
  this: DraftApiClient with TaxonomyApiClient with ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def getParentTopicsAndPaths(topic: Topic,
                                bundle: TaxonomyBundle,
                                path: List[String]): List[(Topic, List[String])] = {
      val parentConnections = bundle.topicSubtopicConnections.filter(_.subtopicid == topic.id)
      val parents = bundle.topics.filter(t => parentConnections.map(_.topicid).contains(t.id))

      parents.flatMap(parent => getParentTopicsAndPaths(parent, bundle, path :+ parent.id)) :+ (topic, path)
    }

    private def parseHtml(html: String) = {
      val document = Jsoup.parseBodyFragment(html)
      document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
      document.body()
    }

    def getArticleTraits(contents: Seq[ArticleContent]): Seq[String] = {
      contents.flatMap(content => {
        val traits = ListBuffer[String]()
        parseHtml(content.content)
          .select("embed")
          .forEach(embed => {
            val dataResource = embed.attr("data-resource")
            dataResource match {
              case "h5p"                => traits += "H5P"
              case "brightcove" | "nrk" => traits += "VIDEO"
              case "external" | "iframe" =>
                val dataUrl = embed.attr("data-url")
                if (dataUrl.contains("youtu") || dataUrl.contains("vimeo") || dataUrl
                      .contains("filmiundervisning") || dataUrl.contains("imdb") || dataUrl
                      .contains("nrk") || dataUrl.contains("khanacademy")) {
                  traits += "VIDEO"
                }
              case _ => // Do nothing
            }
          })
        traits
      })
    }

    private[service] def getAttributes(html: String): List[String] = {
      parseHtml(html)
        .select("embed")
        .asScala
        .flatMap(getAttributes)
        .toList
    }

    private def getAttributes(embed: Element): List[String] = {
      val attributesToKeep = List(
        "data-title",
        "data-caption",
        "data-alt",
        "data-link-text",
        "data-edition",
        "data-publisher",
        "data-authors"
      )

      attributesToKeep.flatMap(attr =>
        embed.attr(attr) match {
          case "" => None
          case a  => Some(a)
      })
    }

    private def getEmbedValuesFromEmbed(embed: Element, language: String): EmbedValues = {
      EmbedValues(resource = getEmbedResource(embed), id = getEmbedIds(embed), language = language)
    }

    private[service] def getEmbedValues(html: String, language: String): List[EmbedValues] = {
      parseHtml(html)
        .select("embed")
        .asScala
        .flatMap(embed => Some(getEmbedValuesFromEmbed(embed, language)))
        .toList
    }

    private def getEmbedResource(embed: Element): Option[String] = {

      embed.attr("data-resource") match {
        case "" => None
        case a  => Some(a)
      }
    }

    private def getEmbedIds(embed: Element): List[String] = {
      val attributesToKeep = List(
        "data-videoid",
        "data-url",
        "data-resource_id",
        "data-content-id",
        "data-article-id"
      )

      attributesToKeep.flatMap(attr =>
        embed.attr(attr) match {
          case "" => None
          case a  => Some(a)
      })
    }

    private def getAttributesToIndex(content: Seq[ArticleContent],
                                     visualElement: Seq[VisualElement]): SearchableLanguageList = {
      val contentTuples = content.map(c => c.language -> getAttributes(c.content))
      val visualElementTuples = visualElement.map(v => v.language -> getAttributes(v.resource))
      val attrsGroupedByLanguage = (contentTuples ++ visualElementTuples).groupBy(_._1)

      val languageValues = attrsGroupedByLanguage.map {
        case (language, values) => LanguageValue(language, values.flatMap(_._2))
      }

      SearchableLanguageList(languageValues.toSeq)
    }

    private def getEmbedResourcesAndIdsToIndex(content: Seq[ArticleContent],
                                               visualElement: Seq[VisualElement],
                                               metaImage: Seq[ArticleMetaImage]): List[EmbedValues] = {
      val contentTuples = content.flatMap(c => getEmbedValues(c.content, c.language))
      val visualElementTuples = visualElement.flatMap(v => getEmbedValues(v.resource, v.language))
      val metaImageTuples =
        metaImage.map(m => EmbedValues(id = List(m.imageId), resource = Some("image"), language = m.language))
      (contentTuples ++ visualElementTuples ++ metaImageTuples).toList

    }

    def asSearchableArticle(ai: Article,
                            taxonomyBundle: TaxonomyBundle,
                            grepBundle: Option[GrepBundle]): Try[SearchableArticle] = {
      val taxonomyForArticle = getTaxonomyContexts(ai.id.get, "article", taxonomyBundle, filterVisibles = true)
      val traits = getArticleTraits(ai.content)
      val embedAttributes = getAttributesToIndex(ai.content, ai.visualElement)
      val embedResourcesAndIds = getEmbedResourcesAndIdsToIndex(ai.content, ai.visualElement, ai.metaImage)

      val articleWithAgreement = converterService.withAgreementCopyright(ai)

      val defaultTitle = articleWithAgreement.title
        .sortBy(title => {
          ISO639.languagePriority.reverse.indexOf(title.language)
        })
        .lastOption

      val supportedLanguages = Language
        .getSupportedLanguages(ai.title, ai.visualElement, ai.introduction, ai.metaDescription, ai.content, ai.tags)
        .toList

      Success(
        SearchableArticle(
          id = articleWithAgreement.id.get,
          title = SearchableLanguageValues(articleWithAgreement.title.map(title =>
            LanguageValue(title.language, title.title))),
          visualElement = SearchableLanguageValues(articleWithAgreement.visualElement.map(visual =>
            LanguageValue(visual.language, visual.resource))),
          introduction = SearchableLanguageValues(articleWithAgreement.introduction.map(intro =>
            LanguageValue(intro.language, intro.introduction))),
          metaDescription = SearchableLanguageValues(articleWithAgreement.metaDescription.map(meta =>
            LanguageValue(meta.language, meta.content))),
          content = SearchableLanguageValues(articleWithAgreement.content.map(article =>
            LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))),
          tags = SearchableLanguageList(articleWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
          lastUpdated = articleWithAgreement.updated,
          license = articleWithAgreement.copyright.license,
          authors = (articleWithAgreement.copyright.creators.map(_.name) ++ articleWithAgreement.copyright.processors
            .map(_.name) ++ articleWithAgreement.copyright.rightsholders.map(_.name)).toList,
          articleType = articleWithAgreement.articleType.toString,
          metaImage = articleWithAgreement.metaImage.toList,
          defaultTitle = defaultTitle.map(t => t.title),
          supportedLanguages = supportedLanguages,
          contexts = taxonomyForArticle.getOrElse(List.empty),
          grepContexts = getGrepContexts(ai.grepCodes, grepBundle),
          traits = traits.toList.distinct,
          embedAttributes = embedAttributes,
          embedResourcesAndIds = embedResourcesAndIds,
          availability = articleWithAgreement.availability.toString
        ))

    }

    def asSearchableLearningPath(lp: LearningPath, taxonomyBundle: TaxonomyBundle): Try[SearchableLearningPath] = {
      val taxonomyForLearningPath =
        getTaxonomyContexts(lp.id.get, "learningpath", taxonomyBundle, filterVisibles = true)

      val supportedLanguages = Language.getSupportedLanguages(lp.title, lp.description).toList
      val defaultTitle = lp.title.sortBy(title => ISO639.languagePriority.reverse.indexOf(title.language)).lastOption
      val license = api.learningpath.Copyright(
        asLearningPathApiLicense(lp.copyright.license),
        lp.copyright.contributors.map(c => api.learningpath.Author(c.`type`, c.name)))

      Success(
        SearchableLearningPath(
          id = lp.id.get,
          title = SearchableLanguageValues(lp.title.map(t => LanguageValue(t.language, t.title))),
          content = SearchableLanguageValues(lp.title.map(t => LanguageValue(t.language, "*"))), // Make suggestion on content work
          description = SearchableLanguageValues(lp.description.map(d => LanguageValue(d.language, d.description))),
          coverPhotoId = lp.coverPhotoId,
          duration = lp.duration,
          status = lp.status.toString,
          verificationStatus = lp.verificationStatus.toString,
          lastUpdated = lp.lastUpdated,
          defaultTitle = defaultTitle.map(_.title),
          tags = SearchableLanguageList(lp.tags.map(tag => LanguageValue(tag.language, tag.tags))),
          learningsteps = lp.learningsteps.map(asSearchableLearningStep),
          copyright = license,
          license = lp.copyright.license,
          isBasedOn = lp.isBasedOn,
          supportedLanguages = supportedLanguages,
          authors = lp.copyright.contributors.map(_.name).toList,
          contexts = taxonomyForLearningPath.getOrElse(List.empty),
          embedResourcesAndIds = List.empty
        ))
    }

    def asSearchableDraft(draft: Draft,
                          taxonomyBundle: TaxonomyBundle,
                          grepBundle: Option[GrepBundle]): Try[SearchableDraft] = {
      val taxonomyForDraft = getTaxonomyContexts(draft.id.get, "article", taxonomyBundle, filterVisibles = false)
      val traits = getArticleTraits(draft.content)
      val embedAttributes = getAttributesToIndex(draft.content, draft.visualElement)
      val embedResourcesAndIds = getEmbedResourcesAndIdsToIndex(draft.content, draft.visualElement, draft.metaImage)

      val defaultTitle = draft.title
        .sortBy(title => {
          ISO639.languagePriority.reverse.indexOf(title.language)
        })
        .lastOption

      val supportedLanguages = Language
        .getSupportedLanguages(
          draft.title,
          draft.visualElement,
          draft.introduction,
          draft.metaDescription,
          draft.content,
          draft.tags
        )
        .toList

      val authors = (
        draft.copyright.map(_.creators).toList ++
          draft.copyright.map(_.processors).toList ++
          draft.copyright.map(_.rightsholders).toList
      ).flatten.map(_.name)

      val notes: List[String] = draft.notes.map(_.note)
      val users: List[String] = List(draft.updatedBy) ++ draft.notes.map(_.user) ++ draft.previousVersionsNotes.map(
        _.user)

      Success(
        SearchableDraft(
          id = draft.id.get,
          draftStatus = search.Status(draft.status.current.toString, draft.status.other.map(_.toString).toSeq),
          title = SearchableLanguageValues(draft.title.map(title => LanguageValue(title.language, title.title))),
          content = SearchableLanguageValues(draft.content.map(article =>
            LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))),
          visualElement = SearchableLanguageValues(draft.visualElement.map(visual =>
            LanguageValue(visual.language, visual.resource))),
          introduction = SearchableLanguageValues(draft.introduction.map(intro =>
            LanguageValue(intro.language, intro.introduction))),
          metaDescription = SearchableLanguageValues(draft.metaDescription.map(meta =>
            LanguageValue(meta.language, meta.content))),
          tags = SearchableLanguageList(draft.tags.map(tag => LanguageValue(tag.language, tag.tags))),
          lastUpdated = draft.updated,
          license = draft.copyright.flatMap(_.license),
          authors = authors,
          articleType = draft.articleType.toString,
          metaImage = draft.metaImage.toList,
          defaultTitle = defaultTitle.map(t => t.title),
          supportedLanguages = supportedLanguages,
          notes = notes,
          contexts = taxonomyForDraft.getOrElse(List.empty),
          users = users.distinct,
          previousVersionsNotes = draft.previousVersionsNotes.map(_.note),
          grepContexts = getGrepContexts(draft.grepCodes, grepBundle),
          traits = traits.toList.distinct,
          embedAttributes = embedAttributes,
          embedResourcesAndIds = embedResourcesAndIds,
        ))

    }

    def asLearningPathApiLicense(license: String): api.learningpath.License = {
      getLicense(license) match {
        case Some(l) => api.learningpath.License(l.license.toString, Option(l.description), l.url)
        case None    => api.learningpath.License(license, Some("Invalid license"), None)
      }
    }

    def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      val nonHtmlDescriptions = learningStep.description.map(desc =>
        domain.learningpath.Description(Jsoup.parseBodyFragment(desc.description).text(), desc.language))
      SearchableLearningStep(
        learningStep.`type`.toString,
        SearchableLanguageValues(learningStep.title.map(t => LanguageValue(t.language, t.title))),
        SearchableLanguageValues(nonHtmlDescriptions.map(d => LanguageValue(d.language, d.description)))
      )
    }

    /**
      * Attempts to extract language that hit from highlights in elasticsearch response.
      *
      * @param result Elasticsearch hit.
      * @return Language if found.
      */
    def getLanguageFromHit(result: SearchHit): Option[String] = {
      def keyToLanguage(keys: Iterable[String]): Option[String] = {
        val keySplits = keys.toList.flatMap(key => key.split('.'))
        val languagesInKeys = keySplits.filter(split => Iso639.get(split).isSuccess)

        languagesInKeys
          .sortBy(lang => {
            Language.languageAnalyzers.map(la => la.languageTag.toString).reverse.indexOf(lang)
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
      *
      * @param hitString Json string returned from elasticsearch for one article.
      * @param language  Language to extract from the hitString.
      * @return Article summary extracted from hitString in specified language.
      */
    def hitAsArticleSummary(hitString: String, language: String): ArticleSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

      val searchableArticle = read[SearchableArticle](hitString)

      val titles = searchableArticle.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val introductions =
        searchableArticle.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions =
        searchableArticle.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements =
        searchableArticle.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val metaImages =
        searchableArticle.metaImage.map(im => api.article.ArticleMetaImage(im.imageId, im.altText, im.language))

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage.toString))
      val visualElement = findByLanguageOrBestEffort(visualElements, language)
      val introduction = findByLanguageOrBestEffort(introductions, language)
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language)
      val metaImage = findByLanguageOrBestEffort(metaImages, language)

      val url = s"${SearchApiProperties.ExternalApiUrls("article-api")}/${searchableArticle.id}"

      ArticleSummary(
        searchableArticle.id,
        title,
        visualElement,
        introduction,
        metaDescription,
        metaImage,
        url,
        searchableArticle.license,
        searchableArticle.articleType,
        searchableArticle.supportedLanguages
      )
    }

    def hitAsDraftSummary(hitString: String, language: String): DraftSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

      val searchableDraft = read[SearchableDraft](hitString)

      val titles = searchableDraft.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val visualElements =
        searchableDraft.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val introductions =
        searchableDraft.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage.toString))
      val visualElement = findByLanguageOrBestEffort(visualElements, language)
      val introduction = findByLanguageOrBestEffort(introductions, language)

      val url = s"${SearchApiProperties.ExternalApiUrls("draft-api")}/${searchableDraft.id}"

      DraftSummary(
        id = searchableDraft.id,
        title = title,
        visualElement = visualElement,
        introduction = introduction,
        url = url,
        license = searchableDraft.license.getOrElse(""),
        articleType = searchableDraft.articleType,
        supportedLanguages = searchableDraft.supportedLanguages,
        notes = searchableDraft.notes
      )
    }

    def hitAsLearningPathSummary(hitString: String, language: String): LearningPathSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableLearningPath = read[SearchableLearningPath](hitString)

      val titles = searchableLearningPath.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val descriptions =
        searchableLearningPath.description.languageValues.map(lv => api.learningpath.Description(lv.value, lv.language))
      val introductionStep = searchableLearningPath.learningsteps.find(_.stepType == StepType.INTRODUCTION.toString)
      val introductions = asApiLearningPathIntroduction(introductionStep)
      val tags =
        searchableLearningPath.tags.languageValues.map(lv => api.learningpath.LearningPathTags(lv.value, lv.language))

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage.toString))
      val description = findByLanguageOrBestEffort(descriptions, language).getOrElse(
        api.learningpath.Description("", Language.UnknownLanguage.toString))
      val introduction = findByLanguageOrBestEffort(introductions, language).getOrElse(
        api.learningpath.Introduction("", Language.UnknownLanguage.toString))
      val tag = findByLanguageOrBestEffort(tags, language).getOrElse(
        api.learningpath.LearningPathTags(Seq.empty, Language.UnknownLanguage.toString))

      val url = s"${SearchApiProperties.ExternalApiUrls("learningpath-api")}/${searchableLearningPath.id}"

      LearningPathSummary(
        searchableLearningPath.id,
        title,
        description,
        introduction,
        url,
        searchableLearningPath.coverPhotoId,
        searchableLearningPath.duration,
        searchableLearningPath.status,
        searchableLearningPath.lastUpdated,
        tag,
        searchableLearningPath.copyright,
        searchableLearningPath.supportedLanguages,
        searchableLearningPath.isBasedOn
      )

    }

    def asApiLearningPathIntroduction(
        learningStep: Option[SearchableLearningStep]): List[api.learningpath.Introduction] = {
      learningStep.map(_.description) match {
        case Some(desc) => desc.languageValues.map(lv => api.learningpath.Introduction(lv.value, lv.language)).toList
        case None       => List.empty
      }
    }

    private def getHighlights(highlights: Map[String, Seq[String]]): List[HighlightedField] = {
      highlights.map {
        case (field, matches) =>
          HighlightedField(
            field = field,
            matches = matches
          )
      }.toList
    }

    private def getPathsFromContext(contexts: List[SearchableTaxonomyContext]): List[String] = {
      contexts.map(_.path)
    }

    def articleHitAsMultiSummary(hit: SearchHit, language: String): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableArticle = read[SearchableArticle](hit.sourceAsString)

      val contexts = searchableArticle.contexts.map(c => searchableContextToApiContext(c, language))

      val titles = searchableArticle.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val introductions =
        searchableArticle.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions =
        searchableArticle.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements =
        searchableArticle.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val metaImages = searchableArticle.metaImage.map(image => {
        val metaImageUrl = s"${SearchApiProperties.ExternalApiUrls("raw-image")}/${image.imageId}"
        api.MetaImage(metaImageUrl, image.altText, image.language)
      })

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage.toString))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
        api.MetaDescription("", Language.UnknownLanguage.toString))
      val metaImage = findByLanguageOrBestEffort(metaImages, language)

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)

      val url = s"${SearchApiProperties.ExternalApiUrls("article-api")}/${searchableArticle.id}"

      MultiSearchSummary(
        id = searchableArticle.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = metaImage,
        url = url,
        contexts = contexts,
        supportedLanguages = supportedLanguages,
        learningResourceType = searchableArticle.articleType,
        status = None,
        traits = searchableArticle.traits,
        score = hit.score,
        highlights = getHighlights(hit.highlight),
        paths = getPathsFromContext(searchableArticle.contexts),
        lastUpdated = searchableArticle.lastUpdated.toDate,
        license = Some(searchableArticle.license)
      )
    }

    def draftHitAsMultiSummary(hit: SearchHit, language: String): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableDraft = read[SearchableDraft](hit.sourceAsString)

      val contexts = searchableDraft.contexts.map(c => searchableContextToApiContext(c, language))

      val titles = searchableDraft.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val introductions =
        searchableDraft.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions =
        searchableDraft.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements =
        searchableDraft.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val metaImages = searchableDraft.metaImage.map(image => {
        val metaImageUrl = s"${SearchApiProperties.ExternalApiUrls("raw-image")}/${image.imageId}"
        api.MetaImage(metaImageUrl, image.altText, image.language)
      })

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage.toString))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
        api.MetaDescription("", Language.UnknownLanguage.toString))
      val metaImage = findByLanguageOrBestEffort(metaImages, language)

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)

      val url = s"${SearchApiProperties.ExternalApiUrls("draft-api")}/${searchableDraft.id}"

      MultiSearchSummary(
        id = searchableDraft.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = metaImage,
        url = url,
        contexts = contexts,
        supportedLanguages = supportedLanguages,
        learningResourceType = searchableDraft.articleType,
        status = Some(api.Status(searchableDraft.draftStatus.current, searchableDraft.draftStatus.other)),
        traits = searchableDraft.traits,
        score = hit.score,
        highlights = getHighlights(hit.highlight),
        paths = getPathsFromContext(searchableDraft.contexts),
        lastUpdated = searchableDraft.lastUpdated.toDate,
        license = searchableDraft.license
      )
    }

    def learningpathHitAsMultiSummary(hit: SearchHit, language: String): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      val searchableLearningPath = read[SearchableLearningPath](hit.sourceAsString)

      val contexts = searchableLearningPath.contexts.map(c => searchableContextToApiContext(c, language))

      val titles = searchableLearningPath.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val metaDescriptions =
        searchableLearningPath.description.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val tags =
        searchableLearningPath.tags.languageValues.map(lv => api.learningpath.LearningPathTags(lv.value, lv.language))

      val supportedLanguages = getSupportedLanguages(titles, metaDescriptions, tags)

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", Language.UnknownLanguage.toString))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
        api.MetaDescription("", Language.UnknownLanguage.toString))
      val url = s"${SearchApiProperties.ExternalApiUrls("learningpath-api")}/${searchableLearningPath.id}"
      val metaImage =
        searchableLearningPath.coverPhotoId.map(
          id =>
            api.MetaImage(
              url = s"${SearchApiProperties.ExternalApiUrls("raw-image")}/$id",
              alt = "",
              language = language
          ))

      MultiSearchSummary(
        id = searchableLearningPath.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = metaImage,
        url = url,
        contexts = contexts,
        supportedLanguages = supportedLanguages,
        learningResourceType = LearningResourceType.LearningPath.toString,
        status = Some(api.Status(searchableLearningPath.status, Seq.empty)),
        traits = List.empty,
        score = hit.score,
        highlights = getHighlights(hit.highlight),
        paths = getPathsFromContext(searchableLearningPath.contexts),
        lastUpdated = searchableLearningPath.lastUpdated.toDate,
        license = Some(searchableLearningPath.license)
      )
    }

    def searchableContextToApiContext(context: SearchableTaxonomyContext, language: String): ApiTaxonomyContext = {
      val subjectName = findByLanguageOrBestEffort(context.subject.languageValues, language).map(_.value).getOrElse("")
      val breadcrumbs = findByLanguageOrBestEffort(context.breadcrumbs.languageValues, language)
        .map(_.value)
        .getOrElse(Seq.empty)
        .toList

      val resourceTypes = context.resourceTypes.map(rt => {
        val name = findByLanguageOrBestEffort(rt.name.languageValues, language)
          .getOrElse(LanguageValue(Language.UnknownLanguage.toString, ""))
        TaxonomyResourceType(id = rt.id, name = name.value, language = name.language)
      })

      val relevance = findByLanguageOrBestEffort(context.relevance.languageValues, language).map(_.value).getOrElse("")

      ApiTaxonomyContext(
        id = context.id,
        subject = subjectName,
        subjectId = context.subjectId,
        relevance = relevance,
        path = context.path,
        breadcrumbs = breadcrumbs,
        filters = List.empty,
        learningResourceType = context.contextType,
        resourceTypes = resourceTypes,
        language = language
      )

    }

    private def compareId(contentUri: String, id: Long, `type`: String): Boolean = {
      contentUri == s"urn:${`type`}:$id"
    }

    private def getContextType(resourceId: String, contentUri: Option[String]): Try[LearningResourceType.Value] = {
      contentUri match {
        case Some(uri) if uri.contains("article") =>
          if (resourceId.contains(":topic:")) {
            Success(LearningResourceType.TopicArticle)
          } else {
            Success(LearningResourceType.Article)
          }
        case Some(uri) if uri.contains("learningpath") => Success(LearningResourceType.LearningPath)
        case _ =>
          val msg = s"Could not find type for resource $resourceId"
          logger.error(msg)
          Failure(ElasticIndexingException(msg))
      }
    }

    private def getBreadcrumbFromIds(ids: List[String], bundle: TaxonomyBundle): Seq[String] = {
      ids.map(id => {
        bundle.getObject(id).map(_.name).getOrElse("")
      })
    }

    /**
      * Returns every parent of resourceType.
      *
      * @param resourceType ResourceType to derive parents for.
      * @param allTypes All resourceTypes to derive parents from.
      * @return List of parents including resourceType.
      */
    def getResourceTypeParents(resourceType: ResourceType, allTypes: List[ResourceType]): List[ResourceType] = {
      def allTypesWithParents(allTypes: List[ResourceType],
                              parents: List[ResourceType]): List[(ResourceType, List[ResourceType])] = {
        allTypes.flatMap(resourceType => {
          val thisLevelWithParents = allTypes.map(resourceType => (resourceType, parents))

          val nextLevelWithParents = resourceType.subtypes match {
            case Some(subtypes) => allTypesWithParents(subtypes, parents :+ resourceType)
            case None           => List.empty
          }
          nextLevelWithParents ++ thisLevelWithParents
        })
      }
      allTypesWithParents(allTypes, List.empty).filter(x => x._1 == resourceType).flatMap(_._2).distinct
    }

    /**
      * Returns a flattened list of resourceType with its subtypes
      *
      * @param resourceType A resource with subtypes
      * @return Flattened list of resourceType with subtypes.
      */
    private def getTypeAndSubtypes(resourceType: ResourceType): List[ResourceType] = {
      def getTypeAndSubtypesWithParent(resourceType: ResourceType,
                                       parents: List[ResourceType] = List.empty): List[ResourceType] = {
        resourceType.subtypes match {
          case None => (parents :+ resourceType).distinct
          case Some(subtypes) =>
            subtypes.flatMap(x => getTypeAndSubtypesWithParent(x, parents :+ resourceType))
        }
      }
      getTypeAndSubtypesWithParent(resourceType, List.empty)
    }

    private def getResourceTaxonomyContexts(resource: Resource,
                                            filterVisibles: Boolean,
                                            bundle: TaxonomyBundle): Try[List[SearchableTaxonomyContext]] = {
      val topicsConnections = bundle.topicResourceConnections.filter(_.resourceId == resource.id)
      val resourceTypeConnections = bundle.resourceResourceTypeConnections.filter(_.resourceId == resource.id)
      val resourceTypesWithParents = getConnectedResourceTypesWithParents(resourceTypeConnections, bundle)

      getContextType(resource.id, resource.contentUri) match {
        case Success(contextType) =>
          val contexts = topicsConnections.map({ tc =>
            val relevanceId = tc.relevanceId.getOrElse("urn:relevance:core")
            val relevanceName = bundle.relevances
              .find(r => r.id == relevanceId)
              .map(_.name)
              .getOrElse("")
            val relevance = SearchableLanguageValues(Seq(LanguageValue(DefaultLanguage, relevanceName)))

            val topic = bundle.topics.find(topic => tc.topicid == topic.id)
            topic
              .map({
                t =>
                  getParentTopicsAndPaths(t, bundle, List(t.id)).flatMap({
                    case (topic, topicPath) =>
                      // Subjects needed to check visibility
                      val subjectConnections = bundle.subjectTopicConnections.filter(_.topicid == topic.id)
                      val subjects =
                        bundle.subjects.filter(subject => subjectConnections.map(_.subjectid).contains(subject.id))

                      val visibleSubjects = if (filterVisibles) {
                        subjects.filter(_.metadata.forall(_.visible))
                      } else {
                        subjects
                      }

                      visibleSubjects.flatMap(subject => {
                        val pathIds = (resource.id +: topicPath :+ subject.id).reverse
                        List(
                          getSearchableTaxonomyContext(resource.id,
                                                       pathIds,
                                                       subject,
                                                       relevanceId,
                                                       relevance,
                                                       contextType,
                                                       resourceTypesWithParents,
                                                       bundle))

                      })
                  })
              })
              .getOrElse(List.empty)
          })
          Success(contexts.flatten)
        case Failure(ex) => Failure(ex)
      }
    }

    private def getSearchableTaxonomyContext(taxonomyId: String,
                                             pathIds: List[String],
                                             subject: TaxSubject,
                                             relevanceId: String,
                                             relevance: SearchableLanguageValues,
                                             contextType: LearningResourceType.Value,
                                             resourceTypes: List[ResourceType],
                                             bundle: TaxonomyBundle) = {

      val path = "/" + pathIds.map(_.replace("urn:", "")).mkString("/")

      val searchableResourceTypes = resourceTypes.map(
        rt =>
          SearchableTaxonomyResourceType(
            id = rt.id,
            name = SearchableLanguageValues(Seq(LanguageValue(DefaultLanguage, rt.name))) // TODO: Get translations
        ))

      val subjectLanguageValues = SearchableLanguageValues(Seq(LanguageValue(DefaultLanguage, subject.name))) // TODO: Get translations
      val breadcrumbList = Seq(LanguageValue(DefaultLanguage, getBreadcrumbFromIds(pathIds.dropRight(1), bundle))) // TODO: Get translations
      val breadcrumbs = SearchableLanguageList(breadcrumbList)

      val parentTopics = getAllParentTopicIds(taxonomyId, bundle)

      SearchableTaxonomyContext(
        id = taxonomyId,
        subjectId = subject.id,
        subject = subjectLanguageValues,
        path = path,
        contextType = contextType.toString,
        breadcrumbs = breadcrumbs,
        relevanceId = Some(relevanceId),
        relevance = relevance,
        resourceTypes = searchableResourceTypes,
        parentTopicIds = parentTopics
      )
    }

    private def getAllParentTopicIds(id: String, bundle: TaxonomyBundle): List[String] = {
      val topicResourceConnections = bundle.topicResourceConnections.filter(_.resourceId == id)
      val topicSubtopicConnections = bundle.topicSubtopicConnections.filter(_.subtopicid == id)

      val directlyConnectedResourceTopics =
        bundle.topics.filter(t => topicResourceConnections.map(_.topicid).contains(t.id))
      val directlyConnectedTopicTopics =
        bundle.topics.filter(t => topicSubtopicConnections.map(_.topicid).contains(t.id))

      val allConnectedTopics = (directlyConnectedResourceTopics ++ directlyConnectedTopicTopics)
        .map(topic => getParentTopicsAndPaths(topic, bundle, List.empty))

      allConnectedTopics.flatMap(topic => topic.map(_._1)).map(_.id)
    }

    private def getConnectedResourceTypesWithParents(connections: List[ResourceTypeConnection],
                                                     bundle: TaxonomyBundle): List[ResourceType] = {
      val allResourceTypes = bundle.resourceTypes.flatMap(rt => getTypeAndSubtypes(rt))

      // Every explicitly specified resourceType
      val resourceTypes = allResourceTypes.filter(r => connections.map(_.resourceTypeId).contains(r.id))

      // Include parents of resourceTypes if they exist
      val subParents =
        resourceTypes.flatMap(rt => getResourceTypeParents(rt, bundle.resourceTypes)).filterNot(resourceTypes.contains)
      (resourceTypes ++ subParents).distinct
    }

    private def getTopicTaxonomyContexts(topic: Topic,
                                         filterVisibles: Boolean,
                                         bundle: TaxonomyBundle): Try[List[SearchableTaxonomyContext]] = {
      val parentTopicsConnections = bundle.topicSubtopicConnections.filter(_.subtopicid == topic.id)
      val parentTopicsAndPaths = getParentTopicsAndPaths(topic, bundle, List(topic.id))

      val relevanceIds = parentTopicsConnections.length match {
        case 0 =>
          bundle.subjectTopicConnections
            .filter(_.topicid == topic.id)
            .map(tc => tc.relevanceId.getOrElse("urn:relevance:core"))
        case _ => parentTopicsConnections.map(tc => tc.relevanceId.getOrElse("urn:relevance:core"))
      }

      getContextType(topic.id, topic.contentUri) match {
        case Success(contextType) =>
          val contexts = parentTopicsAndPaths.map({
            case (parentTopic, topicPath) =>
              val subjectConnections = bundle.subjectTopicConnections.filter(_.topicid == parentTopic.id)
              val subjects = bundle.subjects.filter(subject => subjectConnections.map(_.subjectid).contains(subject.id))

              val visibleSubjects = if (filterVisibles) {
                subjects.filter(subject => subject.metadata.exists(_.visible))
              } else {
                subjects
              }

              visibleSubjects.flatMap(subject => {
                val pathIds = (topicPath :+ subject.id).reverse
                val relevanceId = relevanceIds.headOption.getOrElse("urn.relevance.core")
                val relevanceName = bundle.relevances
                  .find(r => r.id == relevanceId)
                  .map(_.name)
                  .getOrElse("")
                val relevance = SearchableLanguageValues(Seq(LanguageValue(DefaultLanguage, relevanceName)))

                List(
                  getSearchableTaxonomyContext(topic.id,
                                               pathIds,
                                               subject,
                                               relevanceId,
                                               relevance,
                                               contextType,
                                               List.empty,
                                               bundle))
              })
          })
          Success(contexts.flatten)
        case Failure(ex) => Failure(ex)
      }

    }

    /**
      * Parses [[TaxonomyBundle]] to get taxonomy for a single resource/topic.
      *
      * @param id           of article/learningpath
      * @param taxonomyType Type of resource used in contentUri.
      *                     Example: "learningpath" in "urn:learningpath:123"
      * @param bundle       All taxonomy in an object.
      * @return Taxonomy that is to be indexed.
      */
    private def getTaxonomyContexts(id: Long,
                                    taxonomyType: String,
                                    bundle: TaxonomyBundle,
                                    filterVisibles: Boolean): Try[List[SearchableTaxonomyContext]] = {
      val (resources, topics) = getTaxonomyResourceAndTopicsForId(id, bundle, taxonomyType)
      val resourceContexts = getResourceContexts(bundle, filterVisibles, resources)
      val topicContexts = getTopicContexts(bundle, filterVisibles, topics)

      val all = resourceContexts ++ topicContexts
      val failed = all.collect {
        case Failure(e) =>
          logger.error(s"Getting taxonomy context for $id failed with: ", e)
          Failure(e)
      }

      if (failed.nonEmpty) {
        failed.head
      } else {
        val successful = all.collect { case Success(c) => c }
        val distinctContexts = successful.flatten.distinct
        Success(distinctContexts)
      }
    }

    private def getTopicContexts(bundle: TaxonomyBundle,
                                 filterVisibles: Boolean,
                                 topics: List[Topic]): List[Try[List[SearchableTaxonomyContext]]] =
      filterByVisibility(topics, filterVisibles)
        .map(topic => getTopicTaxonomyContexts(topic, filterVisibles, bundle))

    private def getResourceContexts(bundle: TaxonomyBundle,
                                    filterVisibles: Boolean,
                                    resources: List[Resource]): List[Try[List[SearchableTaxonomyContext]]] =
      filterByVisibility(resources, filterVisibles)
        .map(resource => getResourceTaxonomyContexts(resource, filterVisibles, bundle))

    private def filterByVisibility[T <: TaxonomyElement](
        elementsToFilter: List[T],
        filterVisibles: Boolean = true
    ): List[T] =
      if (filterVisibles) {
        elementsToFilter.filter((e: TaxonomyElement) => e.metadata.exists(_.visible))
      } else {
        elementsToFilter
      }

    private[service] def getGrepContexts(grepCodes: Seq[String],
                                         bundle: Option[GrepBundle]): List[SearchableGrepContext] = {
      bundle match {
        case None => List.empty
        case Some(grepBundle) =>
          val grepContext = grepBundle.kjerneelementer ++ grepBundle.kompetansemaal ++ grepBundle.tverrfagligeTemaer
          grepCodes
            .map(
              grepCode =>
                SearchableGrepContext(
                  grepCode,
                  grepContext
                    .find(grepElement => grepElement.kode == grepCode)
                    .flatMap(element =>
                      element.tittel.find(title => title.spraak == "default").map(title => title.verdi))
              ))
            .toList
      }
    }

    private def getTaxonomyResourceAndTopicsForId(id: Long,
                                                  bundle: TaxonomyBundle,
                                                  taxonomyType: String): (List[Resource], List[Topic]) = {
      val idMatchingTaxonomy = (elem: TaxonomyElement) => elem.contentUri.exists(compareId(_, id, taxonomyType))

      val resources = bundle.resources
        .filter(idMatchingTaxonomy)
        .distinct

      val topics = bundle.topics
        .filter(idMatchingTaxonomy)
        .distinct

      (resources, topics)
    }

    def toApiMultiTermsAggregation(agg: domain.TermAggregation): api.MultiSearchTermsAggregation =
      api.MultiSearchTermsAggregation(
        field = agg.field.mkString("."),
        sumOtherDocCount = agg.sumOtherDocCount,
        docCountErrorUpperBound = agg.docCountErrorUpperBound,
        values = agg.buckets.map(
          b =>
            api.TermValue(
              value = b.value,
              count = b.count
          ))
      )

    def toApiMultiSearchResult(searchResult: domain.SearchResult): MultiSearchResult =
      api.MultiSearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results,
        searchResult.suggestions,
        searchResult.aggregations.map(toApiMultiTermsAggregation)
      )

    def toApiGroupMultiSearchResult(group: String, searchResult: domain.SearchResult): GroupSearchResult =
      api.GroupSearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results,
        searchResult.suggestions,
        searchResult.aggregations.map(toApiMultiTermsAggregation),
        group
      )

  }

}
