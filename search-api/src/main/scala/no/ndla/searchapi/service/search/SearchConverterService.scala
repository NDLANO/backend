/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import cats.implicits._
import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.api.{Author, License}
import no.ndla.common.model.api.draft.Comment
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.draft.{Draft, RevisionStatus}
import no.ndla.common.model.domain.{ArticleContent, ArticleMetaImage, Priority, VisualElement}
import no.ndla.language.Language.{UnknownLanguage, findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.language.model.Iso639
import no.ndla.mapping.ISO639
import no.ndla.mapping.License.getLicense
import no.ndla.search.SearchConverter.getEmbedValues
import no.ndla.search.model.domain.EmbedValues
import no.ndla.search.model.{LanguageValue, SearchableLanguageFormats, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.search.{SearchLanguage, model}
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api._
import no.ndla.searchapi.model.domain.LearningResourceType
import no.ndla.searchapi.model.domain.learningpath.{LearningPath, LearningStep}
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
import scala.util.{Success, Try}

trait SearchConverterService {
  this: DraftApiClient with TaxonomyApiClient with ConverterService with Props =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends StrictLogging {

    private def parseHtml(html: String) = {
      val document = Jsoup.parseBodyFragment(html)
      document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
      document.body()
    }

    private def getArticleTraits(contents: Seq[ArticleContent]): Seq[String] = {
      contents.flatMap(content => {
        val traits = ListBuffer[String]()
        parseHtml(content.content)
          .select(EmbedTagName)
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
        .select(EmbedTagName)
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

    private def asSearchableTaxonomyContexts(
        taxonomyContexts: List[TaxonomyContext]
    ): List[SearchableTaxonomyContext] = {
      taxonomyContexts.map(context =>
        SearchableTaxonomyContext(
          publicId = context.publicId,
          rootId = context.rootId,
          root = context.root,
          path = context.path,
          breadcrumbs = context.breadcrumbs,
          contextType = context.contextType.getOrElse(""),
          relevanceId = context.relevanceId,
          relevance = context.relevance,
          resourceTypes = context.resourceTypes,
          parentIds = context.parentIds,
          isPrimary = context.isPrimary,
          isActive = context.isActive
        )
      )
    }

    def asSearchableArticle(
        ai: Article,
        taxonomyBundle: Option[TaxonomyBundle],
        grepBundle: Option[GrepBundle]
    ): Try[SearchableArticle] = {
      val articleId = ai.id.get
      val taxonomyContexts = taxonomyBundle match {
        case Some(bundle) =>
          Success(getTaxonomyContexts(articleId, "article", bundle, filterVisibles = true, filterContexts = true))
        case None =>
          taxonomyApiClient.getTaxonomyContext(
            s"urn:article:$articleId",
            filterVisibles = true,
            filterContexts = true,
            shouldUsePublishedTax = true
          )
      }

      val traits               = getArticleTraits(ai.content)
      val embedAttributes      = getAttributesToIndex(ai.content, ai.visualElement)
      val embedResourcesAndIds = getEmbedResourcesAndIdsToIndex(ai.content, ai.visualElement, ai.metaImage)

      val defaultTitle = ai.title
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
          id = ai.id.get,
          title = SearchableLanguageValues(
            ai.title.map(title => LanguageValue(title.language, title.title))
          ),
          visualElement = model.SearchableLanguageValues(
            ai.visualElement.map(visual => LanguageValue(visual.language, visual.resource))
          ),
          introduction = model.SearchableLanguageValues(
            ai.introduction.map(intro => LanguageValue(intro.language, intro.introduction))
          ),
          metaDescription = model.SearchableLanguageValues(
            ai.metaDescription.map(meta => LanguageValue(meta.language, meta.content))
          ),
          content = model.SearchableLanguageValues(
            ai.content.map(article => LanguageValue(article.language, Jsoup.parseBodyFragment(article.content).text()))
          ),
          tags = SearchableLanguageList(ai.tags.map(tag => LanguageValue(tag.language, tag.tags))),
          lastUpdated = ai.updated,
          license = ai.copyright.license,
          authors = (ai.copyright.creators.map(_.name) ++ ai.copyright.processors
            .map(_.name) ++ ai.copyright.rightsholders.map(_.name)).toList,
          articleType = ai.articleType.entryName,
          metaImage = ai.metaImage.toList,
          defaultTitle = defaultTitle.map(t => t.title),
          supportedLanguages = supportedLanguages,
          contexts = asSearchableTaxonomyContexts(taxonomyContexts.getOrElse(List.empty)),
          grepContexts = getGrepContexts(ai.grepCodes, grepBundle),
          traits = traits.toList.distinct,
          embedAttributes = embedAttributes,
          embedResourcesAndIds = embedResourcesAndIds,
          availability = ai.availability.toString
        )
      )

    }

    def asSearchableLearningPath(
        lp: LearningPath,
        taxonomyBundle: Option[TaxonomyBundle]
    ): Try[SearchableLearningPath] = {
      val taxonomyContexts = taxonomyBundle match {
        case Some(bundle) =>
          Success(getTaxonomyContexts(lp.id.get, "learningpath", bundle, filterVisibles = true, true))
        case None =>
          taxonomyApiClient.getTaxonomyContext(
            s"urn:learningpath:${lp.id.get}",
            filterVisibles = true,
            true,
            shouldUsePublishedTax = true
          )
      }

      val supportedLanguages = getSupportedLanguages(lp.title, lp.description).toList
      val defaultTitle = lp.title.sortBy(title => ISO639.languagePriority.reverse.indexOf(title.language)).lastOption
      val license = api.learningpath.Copyright(
        asLearningPathApiLicense(lp.copyright.license),
        lp.copyright.contributors.map(c => Author(c.`type`, c.name))
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
          contexts = asSearchableTaxonomyContexts(taxonomyContexts.getOrElse(List.empty))
        )
      )
    }

    def asSearchableDraft(
        draft: Draft,
        taxonomyBundle: Option[TaxonomyBundle],
        grepBundle: Option[GrepBundle]
    ): Try[SearchableDraft] = {
      val taxonomyContexts = {
        val draftId = draft.id.get
        taxonomyBundle match {
          case Some(bundle) => Success(getTaxonomyContexts(draftId, "article", bundle, filterVisibles = false, true))
          case None =>
            taxonomyApiClient.getTaxonomyContext(
              s"urn:article:$draftId",
              filterVisibles = false,
              true,
              shouldUsePublishedTax = false
            )
        }
      }

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
      val draftStatus = search.SearchableStatus(draft.status.current.toString, draft.status.other.map(_.toString).toSeq)

      Success(
        SearchableDraft(
          id = draft.id.get,
          draftStatus = draftStatus,
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
          defaultTitle = defaultTitle.map(t => t.title),
          supportedLanguages = supportedLanguages,
          notes = notes,
          contexts = asSearchableTaxonomyContexts(taxonomyContexts.getOrElse(List.empty)),
          users = users.distinct,
          previousVersionsNotes = draft.previousVersionsNotes.map(_.note).toList,
          grepContexts = getGrepContexts(draft.grepCodes, grepBundle),
          traits = traits.toList.distinct,
          embedAttributes = embedAttributes,
          embedResourcesAndIds = embedResourcesAndIds,
          revisionMeta = draft.revisionMeta.toList,
          nextRevision = nextRevision,
          responsible = draft.responsible,
          domainObject = draft,
          priority = draft.priority
        )
      )

    }

    private def asLearningPathApiLicense(license: String): License = {
      getLicense(license) match {
        case Some(l) => License(l.license.toString, Option(l.description), l.url)
        case None    => License(license, Some("Invalid license"), None)
      }
    }

    private def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      SearchableLearningStep(learningStep.`type`.toString)
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

    private def filterContexts(
        contexts: List[SearchableTaxonomyContext],
        language: String,
        filterInactive: Boolean
    ): List[ApiTaxonomyContext] = {
      val filtered = if (filterInactive) contexts.filter(c => c.isActive) else contexts
      filtered.sortBy(!_.isPrimary).map(c => searchableContextToApiContext(c, language))
    }

    def articleHitAsMultiSummary(hit: SearchHit, language: String, filterInactive: Boolean): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormatsWithMillis
      val searchableArticle         = read[SearchableArticle](hit.sourceAsString)

      val contexts = filterContexts(searchableArticle.contexts, language, filterInactive)
      val titles   = searchableArticle.title.languageValues.map(lv => api.Title(lv.value, lv.language))
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
        revisions = Seq.empty,
        responsible = None,
        comments = None,
        priority = Some(Priority.Unspecified)
      )
    }

    def draftHitAsMultiSummary(hit: SearchHit, language: String, filterInactive: Boolean): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormatsWithMillis
      val searchableDraft           = read[SearchableDraft](hit.sourceAsString)

      val contexts = filterContexts(searchableDraft.contexts, language, filterInactive)
      val titles   = searchableDraft.title.languageValues.map(lv => api.Title(lv.value, lv.language))
      val introductions =
        searchableDraft.introduction.languageValues.map(lv => api.article.ArticleIntroduction(lv.value, lv.language))
      val metaDescriptions =
        searchableDraft.metaDescription.languageValues.map(lv => api.MetaDescription(lv.value, lv.language))
      val visualElements =
        searchableDraft.visualElement.languageValues.map(lv => api.article.VisualElement(lv.value, lv.language))
      val metaImages = searchableDraft.domainObject.metaImage.map(image => {
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
      val responsible = searchableDraft.responsible.map(r => api.DraftResponsible(r.responsibleId, r.lastUpdated))
      val comments =
        searchableDraft.domainObject.comments.map(c =>
          Comment(c.id.toString, c.content, c.created, c.updated, c.isOpen)
        )

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
        revisions = revisions,
        responsible = responsible,
        comments = Some(comments),
        priority = Some(searchableDraft.priority)
      )
    }

    def learningpathHitAsMultiSummary(hit: SearchHit, language: String, filterInactive: Boolean): MultiSearchSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormatsWithMillis
      val searchableLearningPath    = read[SearchableLearningPath](hit.sourceAsString)

      val contexts = filterContexts(searchableLearningPath.contexts, language, filterInactive)
      val titles   = searchableLearningPath.title.languageValues.map(lv => api.Title(lv.value, lv.language))
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
        revisions = Seq.empty,
        responsible = None,
        comments = None,
        priority = Some(Priority.Unspecified)
      )
    }

    private def searchableContextToApiContext(
        context: SearchableTaxonomyContext,
        language: String
    ): ApiTaxonomyContext = {
      val subjectName = findByLanguageOrBestEffort(context.root.languageValues, language).map(_.value).getOrElse("")
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
        id = context.publicId,
        publicId = context.publicId,
        subject = subjectName,
        root = subjectName,
        subjectId = context.rootId,
        rootId = context.rootId,
        relevance = relevance,
        relevanceId = context.relevanceId,
        path = context.path,
        breadcrumbs = breadcrumbs,
        learningResourceType = context.contextType,
        contextType = context.contextType,
        resourceTypes = resourceTypes,
        language = language,
        isPrimaryConnection = context.isPrimary,
        isPrimary = context.isPrimary,
        isActive = context.isActive
      )

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

    /** Parses [[TaxonomyBundle]] to get taxonomy for a single node.
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
        filterVisibles: Boolean,
        filterContexts: Boolean
    ) = {
      val nodes       = bundle.nodeByContentUri.getOrElse(s"urn:$taxonomyType:$id", List.empty)
      val allContexts = nodes.flatMap(node => node.contexts)
      val visibles    = if (filterVisibles) allContexts.filter(c => c.isVisible) else allContexts
      if (filterContexts) visibles.filter(c => c.rootId.contains("subject")) else visibles
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

    private def toApiMultiTermsAggregation(agg: domain.TermAggregation): api.MultiSearchTermsAggregation =
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
