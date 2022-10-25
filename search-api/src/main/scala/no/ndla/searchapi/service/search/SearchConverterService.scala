/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.LazyLogging
import no.ndla.common.model.domain.{ArticleContent, ArticleMetaImage, VisualElement}
import no.ndla.common.model.domain.draft.{Draft, RevisionStatus}
import no.ndla.language.Language.{UnknownLanguage, findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.language.model.Iso639
import no.ndla.mapping.ISO639
import no.ndla.mapping.License.getLicense
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api._
import no.ndla.searchapi.model.api.article.ArticleSummary
import no.ndla.searchapi.model.api.draft.DraftSummary
import no.ndla.searchapi.model.api.learningpath.LearningPathSummary
import no.ndla.search.{SearchLanguage, model}
import no.ndla.search.model.{LanguageValue, SearchableLanguageFormats, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.domain.article.{LearningResourceType, _}
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
import cats.implicits._

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

trait SearchConverterService {
  this: DraftApiClient with TaxonomyApiClient with ConverterService with Props =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def getParentTopicsAndPaths(
        topic: Topic,
        bundle: TaxonomyBundle,
        path: List[String]
    ): List[(Topic, List[String])] = {
      val parentConnections = bundle.topicSubtopicConnectionsBySubTopicId.getOrElse(topic.id, List.empty)
      val parents           = parentConnections.flatMap(pc => bundle.topicById.get(pc.topicid))

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
                if (
                  dataUrl.contains("youtu") || dataUrl.contains("vimeo") || dataUrl
                    .contains("filmiundervisning") || dataUrl.contains("imdb") || dataUrl
                    .contains("nrk") || dataUrl.contains("khanacademy")
                ) {
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
        }
      )
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
        }
      )
    }

    private def getAttributesToIndex(
        content: Seq[ArticleContent],
        visualElement: Seq[VisualElement]
    ): SearchableLanguageList = {
      val contentTuples          = content.map(c => c.language -> getAttributes(c.content))
      val visualElementTuples    = visualElement.map(v => v.language -> getAttributes(v.resource))
      val attrsGroupedByLanguage = (contentTuples ++ visualElementTuples).groupBy(_._1)

      val languageValues = attrsGroupedByLanguage.map { case (language, values) =>
        LanguageValue(language, values.flatMap(_._2))
      }

      SearchableLanguageList(languageValues.toSeq)
    }

    private def getEmbedResourcesAndIdsToIndex(
        content: Seq[ArticleContent],
        visualElement: Seq[VisualElement],
        metaImage: Seq[ArticleMetaImage]
    ): List[EmbedValues] = {
      val contentTuples       = content.flatMap(c => getEmbedValues(c.content, c.language))
      val visualElementTuples = visualElement.flatMap(v => getEmbedValues(v.resource, v.language))
      val metaImageTuples =
        metaImage.map(m => EmbedValues(id = List(m.imageId), resource = Some("image"), language = m.language))
      (contentTuples ++ visualElementTuples ++ metaImageTuples).toList

    }

    def asSearchableArticle(
        ai: Article,
        taxonomyBundle: TaxonomyBundle,
        grepBundle: Option[GrepBundle]
    ): Try[SearchableArticle] = {
      val taxonomyForArticle   = getTaxonomyContexts(ai.id.get, "article", taxonomyBundle, filterVisibles = true)
      val traits               = getArticleTraits(ai.content)
      val embedAttributes      = getAttributesToIndex(ai.content, ai.visualElement)
      val embedResourcesAndIds = getEmbedResourcesAndIdsToIndex(ai.content, ai.visualElement, ai.metaImage)

      val articleWithAgreement = converterService.withAgreementCopyright(ai)

      val defaultTitle = articleWithAgreement.title
        .sortBy(title => {
          ISO639.languagePriority.reverse.indexOf(title.language)
        })
        .lastOption

      val supportedLanguages = getSupportedLanguages(
        ai.title,
        ai.visualElement,
        ai.introduction,
        ai.metaDescription,
        ai.content,
        ai.tags
      ).toList

      Success(
        SearchableArticle(
          id = articleWithAgreement.id.get,
          title = SearchableLanguageValues(
            articleWithAgreement.title.map(title => LanguageValue(title.language, title.title))
          ),
          visualElement = model.SearchableLanguageValues(
            articleWithAgreement.visualElement.map(visual => LanguageValue(visual.language, visual.resource))
          ),
          introduction = model.SearchableLanguageValues(
            articleWithAgreement.introduction.map(intro => LanguageValue(intro.language, intro.introduction))
          ),
          metaDescription = model.SearchableLanguageValues(
            articleWithAgreement.metaDescription.map(meta => LanguageValue(meta.language, meta.content))
          ),
          content = model.SearchableLanguageValues(
            articleWithAgreement.content.map(article =>
              LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text())
            )
          ),
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
        )
      )

    }

    def asSearchableLearningPath(lp: LearningPath, taxonomyBundle: TaxonomyBundle): Try[SearchableLearningPath] = {
      val taxonomyForLearningPath =
        getTaxonomyContexts(lp.id.get, "learningpath", taxonomyBundle, filterVisibles = true)

      val supportedLanguages = getSupportedLanguages(lp.title, lp.description).toList
      val defaultTitle = lp.title.sortBy(title => ISO639.languagePriority.reverse.indexOf(title.language)).lastOption
      val license = api.learningpath.Copyright(
        asLearningPathApiLicense(lp.copyright.license),
        lp.copyright.contributors.map(c => api.learningpath.Author(c.`type`, c.name))
      )

      Success(
        SearchableLearningPath(
          id = lp.id.get,
          title = model.SearchableLanguageValues(lp.title.map(t => LanguageValue(t.language, t.title))),
          content = model.SearchableLanguageValues(
            lp.title.map(t => LanguageValue(t.language, "*"))
          ), // Make suggestion on content work
          description =
            model.SearchableLanguageValues(lp.description.map(d => LanguageValue(d.language, d.description))),
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
        )
      )
    }

    def asSearchableDraft(
        draft: Draft,
        taxonomyBundle: TaxonomyBundle,
        grepBundle: Option[GrepBundle]
    ): Try[SearchableDraft] = {
      val taxonomyForDraft     = getTaxonomyContexts(draft.id.get, "article", taxonomyBundle, filterVisibles = false)
      val traits               = getArticleTraits(draft.content)
      val embedAttributes      = getAttributesToIndex(draft.content, draft.visualElement)
      val embedResourcesAndIds = getEmbedResourcesAndIdsToIndex(draft.content, draft.visualElement, draft.metaImage)

      val defaultTitle = draft.title
        .sortBy(title => {
          ISO639.languagePriority.reverse.indexOf(title.language)
        })
        .lastOption

      val supportedLanguages = getSupportedLanguages(
        draft.title,
        draft.visualElement,
        draft.introduction,
        draft.metaDescription,
        draft.content,
        draft.tags
      ).toList

      val authors = (
        draft.copyright.map(_.creators).toList ++
          draft.copyright.map(_.processors).toList ++
          draft.copyright.map(_.rightsholders).toList
      ).flatten.map(_.name)

      val notes: List[String] = draft.notes.map(_.note).toList
      val users: List[String] =
        List(draft.updatedBy) ++ draft.notes.map(_.user) ++ draft.previousVersionsNotes.map(_.user)
      val nextRevision =
        draft.revisionMeta.filter(_.status == RevisionStatus.NeedsRevision).sortBy(_.revisionDate).headOption

      val responsible = draft.responsible.map(responsible =>
        Responsible(responsibleId = responsible.responsibleId, lastUpdated = responsible.lastUpdated)
      )

      Success(
        SearchableDraft(
          id = draft.id.get,
          draftStatus = search.Status(draft.status.current.toString, draft.status.other.map(_.toString).toSeq),
          title = model.SearchableLanguageValues(draft.title.map(title => LanguageValue(title.language, title.title))),
          content = model.SearchableLanguageValues(
            draft.content.map(article =>
              LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text())
            )
          ),
          visualElement = model.SearchableLanguageValues(
            draft.visualElement.map(visual => LanguageValue(visual.language, visual.resource))
          ),
          introduction = model.SearchableLanguageValues(
            draft.introduction.map(intro => LanguageValue(intro.language, intro.introduction))
          ),
          metaDescription = model.SearchableLanguageValues(
            draft.metaDescription.map(meta => LanguageValue(meta.language, meta.content))
          ),
          tags = SearchableLanguageList(draft.tags.map(tag => LanguageValue(tag.language, tag.tags))),
          lastUpdated = draft.updated,
          license = draft.copyright.flatMap(_.license),
          authors = authors,
          articleType = draft.articleType.entryName,
          metaImage = draft.metaImage.toList,
          defaultTitle = defaultTitle.map(t => t.title),
          supportedLanguages = supportedLanguages,
          notes = notes,
          contexts = taxonomyForDraft.getOrElse(List.empty),
          users = users.distinct,
          previousVersionsNotes = draft.previousVersionsNotes.map(_.note).toList,
          grepContexts = getGrepContexts(draft.grepCodes, grepBundle),
          traits = traits.toList.distinct,
          embedAttributes = embedAttributes,
          embedResourcesAndIds = embedResourcesAndIds,
          revisionMeta = draft.revisionMeta.toList,
          nextRevision = nextRevision,
          responsible = responsible
        )
      )

    }

    def asLearningPathApiLicense(license: String): api.learningpath.License = {
      getLicense(license) match {
        case Some(l) => api.learningpath.License(l.license.toString, Option(l.description), l.url)
        case None    => api.learningpath.License(license, Some("Invalid license"), None)
      }
    }

    def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      val nonHtmlDescriptions = learningStep.description.map(desc =>
        domain.learningpath.Description(Jsoup.parseBodyFragment(desc.description).text(), desc.language)
      )
      SearchableLearningStep(
        learningStep.`type`.toString,
        model.SearchableLanguageValues(learningStep.title.map(t => LanguageValue(t.language, t.title))),
        model.SearchableLanguageValues(nonHtmlDescriptions.map(d => LanguageValue(d.language, d.description)))
      )
    }

    /** Attempts to extract language that hit from highlights in elasticsearch response.
      *
      * @param result
      *   Elasticsearch hit.
      * @return
      *   Language if found.
      */
    def getLanguageFromHit(result: SearchHit): Option[String] = {
      def keyToLanguage(keys: Iterable[String]): Option[String] = {
        val keySplits       = keys.toList.flatMap(key => key.split('.'))
        val languagesInKeys = keySplits.filter(split => Iso639.get(split).isSuccess)

        languagesInKeys
          .sortBy(lang => {
            SearchLanguage.languageAnalyzers.map(la => la.languageTag.toString).reverse.indexOf(lang)
          })
          .lastOption
      }

      val highlightKeys: Option[Map[String, _]] = Option(result.highlight)
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
      *
      * @param hitString
      *   Json string returned from elasticsearch for one article.
      * @param language
      *   Language to extract from the hitString.
      * @return
      *   Article summary extracted from hitString in specified language.
      */
    def hitAsArticleSummary(hitString: String, language: String): ArticleSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormatsWithMillis

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
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", UnknownLanguage.toString))
      val visualElement   = findByLanguageOrBestEffort(visualElements, language)
      val introduction    = findByLanguageOrBestEffort(introductions, language)
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language)
      val metaImage       = findByLanguageOrBestEffort(metaImages, language)

      val url = s"${props.ExternalApiUrls("article-api")}/${searchableArticle.id}"

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
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormatsWithMillis

      val searchableDraft = read[SearchableDraft](hitString)

      val titles = searchableDraft.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val visualElements =
        searchableDraft.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val introductions =
        searchableDraft.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", UnknownLanguage.toString))
      val visualElement = findByLanguageOrBestEffort(visualElements, language)
      val introduction  = findByLanguageOrBestEffort(introductions, language)

      val url = s"${props.ExternalApiUrls("draft-api")}/${searchableDraft.id}"

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
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormatsWithMillis
      val searchableLearningPath    = read[SearchableLearningPath](hitString)

      val titles = searchableLearningPath.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val descriptions =
        searchableLearningPath.description.languageValues.map(lv => api.learningpath.Description(lv.value, lv.language))
      val introductionStep = searchableLearningPath.learningsteps.find(_.stepType == StepType.INTRODUCTION.toString)
      val introductions    = asApiLearningPathIntroduction(introductionStep)
      val tags =
        searchableLearningPath.tags.languageValues.map(lv => api.learningpath.LearningPathTags(lv.value, lv.language))

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", UnknownLanguage.toString))
      val description = findByLanguageOrBestEffort(descriptions, language).getOrElse(
        api.learningpath.Description("", UnknownLanguage.toString)
      )
      val introduction = findByLanguageOrBestEffort(introductions, language).getOrElse(
        api.learningpath.Introduction("", UnknownLanguage.toString)
      )
      val tag = findByLanguageOrBestEffort(tags, language).getOrElse(
        api.learningpath.LearningPathTags(Seq.empty, UnknownLanguage.toString)
      )

      val url = s"${props.ExternalApiUrls("learningpath-api")}/${searchableLearningPath.id}"

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
        learningStep: Option[SearchableLearningStep]
    ): List[api.learningpath.Introduction] = {
      learningStep.map(_.description) match {
        case Some(desc) => desc.languageValues.map(lv => api.learningpath.Introduction(lv.value, lv.language)).toList
        case None       => List.empty
      }
    }

    private def getHighlights(highlights: Map[String, Seq[String]]): List[HighlightedField] = {
      highlights.map { case (field, matches) =>
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
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormatsWithMillis
      val searchableArticle         = read[SearchableArticle](hit.sourceAsString)

      val contexts = searchableArticle.contexts.map(c => searchableContextToApiContext(c, language))

      val titles = searchableArticle.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val introductions =
        searchableArticle.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions =
        searchableArticle.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements =
        searchableArticle.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val metaImages = searchableArticle.metaImage.map(image => {
        val metaImageUrl = s"${props.ExternalApiUrls("raw-image")}/${image.imageId}"
        api.MetaImage(metaImageUrl, image.altText, image.language)
      })

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", UnknownLanguage.toString))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
        api.MetaDescription("", UnknownLanguage.toString)
      )
      val metaImage = findByLanguageOrBestEffort(metaImages, language)

      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)

      val url = s"${props.ExternalApiUrls("article-api")}/${searchableArticle.id}"

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
        lastUpdated = searchableArticle.lastUpdated,
        license = Some(searchableArticle.license),
        revisions = Seq.empty
      )
    }

    def draftHitAsMultiSummary(hit: SearchHit, language: String): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormatsWithMillis
      val searchableDraft           = read[SearchableDraft](hit.sourceAsString)

      val contexts = searchableDraft.contexts.map(c => searchableContextToApiContext(c, language))

      val titles = searchableDraft.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val introductions =
        searchableDraft.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions =
        searchableDraft.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements =
        searchableDraft.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val metaImages = searchableDraft.metaImage.map(image => {
        val metaImageUrl = s"${props.ExternalApiUrls("raw-image")}/${image.imageId}"
        api.MetaImage(metaImageUrl, image.altText, image.language)
      })

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", UnknownLanguage.toString))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
        api.MetaDescription("", UnknownLanguage.toString)
      )
      val metaImage          = findByLanguageOrBestEffort(metaImages, language)
      val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)
      val url                = s"${props.ExternalApiUrls("draft-api")}/${searchableDraft.id}"
      val revisions =
        searchableDraft.revisionMeta.map(m => api.RevisionMeta(m.revisionDate, m.note, m.status.entryName))

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
        lastUpdated = searchableDraft.lastUpdated,
        license = searchableDraft.license,
        revisions = revisions
      )
    }

    def learningpathHitAsMultiSummary(hit: SearchHit, language: String): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormatsWithMillis
      val searchableLearningPath    = read[SearchableLearningPath](hit.sourceAsString)

      val contexts = searchableLearningPath.contexts.map(c => searchableContextToApiContext(c, language))

      val titles = searchableLearningPath.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val metaDescriptions =
        searchableLearningPath.description.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val tags =
        searchableLearningPath.tags.languageValues.map(lv => api.learningpath.LearningPathTags(lv.value, lv.language))

      val supportedLanguages = getSupportedLanguages(titles, metaDescriptions, tags)

      val title =
        findByLanguageOrBestEffort(titles, language).getOrElse(api.Title("", UnknownLanguage.toString))
      val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
        api.MetaDescription("", UnknownLanguage.toString)
      )
      val url = s"${props.ExternalApiUrls("learningpath-api")}/${searchableLearningPath.id}"
      val metaImage =
        searchableLearningPath.coverPhotoId.map(id =>
          api.MetaImage(
            url = s"${props.ExternalApiUrls("raw-image")}/$id",
            alt = "",
            language = language
          )
        )

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
        lastUpdated = searchableLearningPath.lastUpdated,
        license = Some(searchableLearningPath.license),
        revisions = Seq.empty
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
          .getOrElse(LanguageValue(UnknownLanguage.toString, ""))
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

    private def getAllLanguagesAndDefault(breadcrumbs: Seq[TaxonomyElement]): Seq[String] =
      (breadcrumbs
        .flatMap(_.translations.map(_.language)) :+ props.DefaultLanguage).distinct

    private def maybeElementToName(maybeElement: Option[TaxonomyElement], language: String): String =
      maybeElement
        .map(_.getNameFromTranslationOrDefault(language))
        .getOrElse("")

    private def logBreadcrumbBuildingError(idsAndTaxonomy: Seq[(String, Option[TaxonomyElement])]): Unit =
      if (idsAndTaxonomy.exists(_._2.isEmpty)) {
        val idError = idsAndTaxonomy
          .map(bc => {
            if (bc._2.isEmpty) { s"${bc._1} (MISSING)" }
            else { s"${bc._1} (OK)" }
          })
          .mkString(" -> ")
        logger.warn(s"Something weird when getting taxonomy objects for building breadcrumbs, got: '$idError'")
      }

    private def getBreadcrumbFromIds(ids: Seq[String], bundle: TaxonomyBundle): SearchableLanguageList = {
      val idsAndBreadcrumbs = ids.map(id => id -> bundle.getObject(id))
      logBreadcrumbBuildingError(idsAndBreadcrumbs)

      val breadcrumbs  = idsAndBreadcrumbs.map(_._2)
      val allLanguages = getAllLanguagesAndDefault(breadcrumbs.flatten)

      val languageLists = allLanguages.map(language => {
        val crumbNames = breadcrumbs.map(e => maybeElementToName(e, language))
        LanguageValue(language, crumbNames)
      })

      SearchableLanguageList(languageLists)
    }

    private def getRelevanceNames(relevanceId: String, bundle: TaxonomyBundle): SearchableLanguageValues = {
      val relevanceName = bundle.relevancesById
        .get(relevanceId)
        .map(relevance => {
          getSearchableLanguageValues(relevance.name, relevance.translations)
        })

      relevanceName.getOrElse(SearchableLanguageValues(Seq(LanguageValue(props.DefaultLanguage, ""))))
    }

    private def getResourceTaxonomyContexts(
        resource: Resource,
        filterVisibles: Boolean,
        bundle: TaxonomyBundle
    ): Try[List[SearchableTaxonomyContext]] = {
      val topicsConnections = bundle.topicResourceConnectionsByResourceId.getOrElse(resource.id, List.empty)
      val resourceTypeConnections =
        bundle.resourceResourceTypeConnectionsByResourceId.getOrElse(resource.id, List.empty)
      val resourceTypesWithParents = getConnectedResourceTypesWithParents(resourceTypeConnections, bundle)

      getContextType(resource.id, resource.contentUri) match {
        case Success(contextType) =>
          val contexts = topicsConnections.map({ tc =>
            val relevanceId = tc.relevanceId.getOrElse("urn:relevance:core")
            val relevance   = getRelevanceNames(relevanceId, bundle)

            val topic = bundle.topicById.get(tc.topicid)
            topic
              .map({ t =>
                getParentTopicsAndPaths(t, bundle, List(t.id)).flatMap({ case (topic, topicPath) =>
                  val topicShouldBeExcluded = filterVisibles && (!topic.metadata.forall(_.visible))
                  if (topicShouldBeExcluded) {
                    List.empty
                  } else {
                    // Subjects needed to check visibility
                    val subjectConnections = bundle.subjectTopicConnectionsByTopicId.getOrElse(topic.id, List.empty)
                    val subjects           = subjectConnections.flatMap(sc => bundle.subjectsById.get(sc.subjectid))

                    val visibleSubjects = if (filterVisibles) {
                      subjects.filter(_.metadata.forall(_.visible))
                    } else {
                      subjects
                    }

                    visibleSubjects.map(subject => {
                      val pathIds = (resource.id +: topicPath :+ subject.id).reverse
                      getSearchableTaxonomyContext(
                        resource.id,
                        pathIds,
                        subject,
                        relevanceId,
                        relevance,
                        contextType,
                        resourceTypesWithParents,
                        bundle
                      )

                    })

                  }
                })
              })
              .getOrElse(List.empty)
          })
          Success(contexts.flatten)
        case Failure(ex) => Failure(ex)
      }
    }

    private[search] def getSearchableLanguageValues(
        name: String,
        translations: List[TaxonomyTranslation]
    ): SearchableLanguageValues = {
      val mainLv       = LanguageValue(props.DefaultLanguage, name)
      val translateLvs = translations.map(t => LanguageValue(t.language, t.name))

      // Keep `mainLv` at the back of the list so a translation is picked if one exists for the default language
      val lvsToUse = (translateLvs :+ mainLv).distinctBy(_.language)

      SearchableLanguageValues(lvsToUse)
    }

    private def getSearchableTaxonomyContext(
        taxonomyId: String,
        pathIds: List[String],
        subject: TaxSubject,
        relevanceId: String,
        relevance: SearchableLanguageValues,
        contextType: LearningResourceType.Value,
        resourceTypes: List[ResourceType],
        bundle: TaxonomyBundle
    ): SearchableTaxonomyContext = {

      val path = "/" + pathIds.map(_.replace("urn:", "")).mkString("/")

      val searchableResourceTypes = resourceTypes.map(rt => {
        SearchableTaxonomyResourceType(
          id = rt.id,
          name = getSearchableLanguageValues(rt.name, rt.translations)
        )
      })

      val subjectLanguageValues = getSearchableLanguageValues(subject.name, subject.translations)
      val breadcrumbs           = getBreadcrumbFromIds(pathIds.dropRight(1), bundle)

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
      val topicResourceConnections = bundle.topicResourceConnectionsByResourceId.getOrElse(id, List.empty)
      val topicSubtopicConnections = bundle.topicSubtopicConnectionsBySubTopicId.getOrElse(id, List.empty)

      val directlyConnectedResourceTopics = topicResourceConnections.flatMap(trc => bundle.topicById.get(trc.topicid))
      val directlyConnectedTopicTopics    = topicSubtopicConnections.flatMap(tsc => bundle.topicById.get(tsc.topicid))

      val allConnectedTopics = (directlyConnectedResourceTopics ++ directlyConnectedTopicTopics)
        .map(topic => getParentTopicsAndPaths(topic, bundle, List.empty))

      allConnectedTopics.flatMap(topic => topic.map(_._1)).map(_.id)
    }

    private def getConnectedResourceTypesWithParents(
        connections: List[ResourceTypeConnection],
        bundle: TaxonomyBundle
    ): List[ResourceType] = {

      // Every explicitly specified resourceType
      val connectedResourceTypes = connections.flatMap(c => bundle.allResourceTypesById.get(c.resourceTypeId))

      // Include parents of resourceTypes if they exist
      val subParents =
        connectedResourceTypes
          .flatMap(rt => bundle.resourceTypeParentsByResourceTypeId.getOrElse(rt.id, List.empty))
          .filterNot(connectedResourceTypes.contains)
      (connectedResourceTypes ++ subParents).distinct.sortWith((l, _) => l.subtypes.isDefined)
    }

    private def getTopicTaxonomyContexts(
        topic: Topic,
        filterVisibles: Boolean,
        bundle: TaxonomyBundle
    ): Try[List[SearchableTaxonomyContext]] = {
      val parentTopicsConnections = bundle.topicSubtopicConnectionsBySubTopicId.getOrElse(topic.id, List.empty)
      val parentTopicsAndPaths    = getParentTopicsAndPaths(topic, bundle, List(topic.id))

      val relevanceIds = parentTopicsConnections.length match {
        case 0 =>
          bundle.subjectTopicConnectionsByTopicId
            .getOrElse(topic.id, List.empty)
            .map(tc => tc.relevanceId.getOrElse("urn:relevance:core"))
        case _ => parentTopicsConnections.map(tc => tc.relevanceId.getOrElse("urn:relevance:core"))
      }

      getContextType(topic.id, topic.contentUri) match {
        case Success(contextType) =>
          val contexts = parentTopicsAndPaths.map({ case (parentTopic, topicPath) =>
            val topicShouldBeExcluded = filterVisibles && (!parentTopic.metadata.forall(_.visible))
            if (topicShouldBeExcluded) {
              List.empty
            } else {
              val subjectConnections = bundle.subjectTopicConnectionsByTopicId.getOrElse(parentTopic.id, List.empty)
              val subjects =
                subjectConnections.flatMap(sc => bundle.subjectsById.get(sc.subjectid))

              val visibleSubjects = if (filterVisibles) {
                subjects.filter(subject => subject.metadata.exists(_.visible))
              } else {
                subjects
              }

              visibleSubjects.map(subject => {
                val pathIds     = (topicPath :+ subject.id).reverse
                val relevanceId = relevanceIds.headOption.getOrElse("urn.relevance.core")
                val relevance   = getRelevanceNames(relevanceId, bundle)

                getSearchableTaxonomyContext(
                  topic.id,
                  pathIds,
                  subject,
                  relevanceId,
                  relevance,
                  contextType,
                  List.empty,
                  bundle
                )
              })
            }
          })
          Success(contexts.flatten)
        case Failure(ex) => Failure(ex)
      }

    }

    /** Parses [[TaxonomyBundle]] to get taxonomy for a single resource/topic.
      *
      * @param id
      *   of article/learningpath
      * @param taxonomyType
      *   Type of resource used in contentUri. Example: "learningpath" in "urn:learningpath:123"
      * @param bundle
      *   All taxonomy in an object.
      * @return
      *   Taxonomy that is to be indexed.
      */
    private def getTaxonomyContexts(
        id: Long,
        taxonomyType: String,
        bundle: TaxonomyBundle,
        filterVisibles: Boolean
    ): Try[List[SearchableTaxonomyContext]] = {
      val (resources, topics) = getTaxonomyResourceAndTopicsForId(id, bundle, taxonomyType)
      val resourceContexts    = getResourceContexts(bundle, filterVisibles, resources)
      val topicContexts       = getTopicContexts(bundle, filterVisibles, topics)

      val all = (resourceContexts ++ topicContexts).sequence.map(_.flatten)

      all match {
        case Failure(ex) =>
          logger.error(s"Getting taxonomy context for $id failed with: ", ex)
          Failure(ex)
        case Success(ctx) => Success(ctx.distinct)
      }
    }

    private def getTopicContexts(
        bundle: TaxonomyBundle,
        filterVisibles: Boolean,
        topics: List[Topic]
    ): List[Try[List[SearchableTaxonomyContext]]] =
      filterByVisibility(topics, filterVisibles)
        .map(topic => getTopicTaxonomyContexts(topic, filterVisibles, bundle))

    private def getResourceContexts(
        bundle: TaxonomyBundle,
        filterVisibles: Boolean,
        resources: List[Resource]
    ): List[Try[List[SearchableTaxonomyContext]]] =
      filterByVisibility(resources, filterVisibles)
        .map(resource => getResourceTaxonomyContexts(resource, filterVisibles, bundle))

    private def filterByVisibility[T <: TaxonomyElement](
        elementsToFilter: List[T],
        filterVisibles: Boolean
    ): List[T] =
      if (filterVisibles) {
        elementsToFilter.filter(e => e.metadata.exists(_.visible))
      } else {
        elementsToFilter
      }

    private[service] def getGrepContexts(
        grepCodes: Seq[String],
        bundle: Option[GrepBundle]
    ): List[SearchableGrepContext] = {
      bundle match {
        case None => List.empty
        case Some(grepBundle) =>
          grepCodes
            .map(grepCode =>
              SearchableGrepContext(
                grepCode,
                grepBundle.grepContextByCode
                  .get(grepCode)
                  .flatMap(element => element.tittel.find(title => title.spraak == "default").map(title => title.verdi))
              )
            )
            .toList
      }
    }

    private def getTaxonomyResourceAndTopicsForId(
        id: Long,
        bundle: TaxonomyBundle,
        taxonomyType: String
    ): (List[Resource], List[Topic]) = {
      val potentialContentUri = s"urn:$taxonomyType:$id"
      val resources           = bundle.resourcesByContentUri.getOrElse(potentialContentUri, List.empty)
      val topics              = bundle.topicsByContentUri.getOrElse(potentialContentUri, List.empty)

      (resources, topics)
    }

    def toApiMultiTermsAggregation(agg: domain.TermAggregation): api.MultiSearchTermsAggregation =
      api.MultiSearchTermsAggregation(
        field = agg.field.mkString("."),
        sumOtherDocCount = agg.sumOtherDocCount,
        docCountErrorUpperBound = agg.docCountErrorUpperBound,
        values = agg.buckets.map(b =>
          api.TermValue(
            value = b.value,
            count = b.count
          )
        )
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
