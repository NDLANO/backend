/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import cats.implicits.*
import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common
import no.ndla.common.CirceUtil
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.MissingIdException
import no.ndla.common.implicits.*
import no.ndla.common.model.api.search.{
  ApiTaxonomyContextDTO,
  HighlightedFieldDTO,
  LanguageValue,
  LearningResourceType,
  MetaDescriptionDTO,
  MetaImageDTO,
  MultiSearchResultDTO,
  MultiSearchSummaryDTO,
  NodeHitDTO,
  RevisionMetaDTO,
  SearchTrait,
  SearchType,
  SearchableLanguageList,
  SearchableLanguageValues,
  StatusDTO,
  SubjectPageSummaryDTO,
  TaxonomyResourceTypeDTO,
  TitleWithHtmlDTO
}
import no.ndla.common.model.api.{AuthorDTO, CommentDTO, LicenseDTO, ResponsibleDTO}
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.concept.Concept
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.frontpage.{AboutSubject, SubjectPage}
import no.ndla.common.model.domain.learningpath.{LearningPath, LearningStep}
import no.ndla.common.model.domain.{
  ArticleContent,
  ArticleMetaImage,
  ArticleType,
  Tag,
  VisualElement,
  ResourceType as MyNDLAResourceType
}
import no.ndla.language.Language.{
  UnknownLanguage,
  findByLanguageOrBestEffort,
  getDefault,
  getSupportedLanguages,
  sortLanguagesByPriority
}
import no.ndla.language.model.{Iso639, LanguageField}
import no.ndla.mapping.License.getLicense
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.search.AggregationBuilder.toApiMultiTermsAggregation
import no.ndla.search.SearchConverter.getEmbedValues
import no.ndla.search.model.domain.EmbedValues
import no.ndla.search.model
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.*
import no.ndla.searchapi.model.api.*
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.model.grep.*
import no.ndla.searchapi.model.search.*
import no.ndla.searchapi.model.taxonomy.*
import no.ndla.searchapi.model.{api, domain, search}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}
import no.ndla.common.model.domain.getNextRevision

class SearchConverterService(using
    taxonomyApiClient: TaxonomyApiClient,
    props: Props,
    myndlaApiClient: MyNDLAApiClient
) extends StrictLogging {

  private def parseHtml(html: String): Element = {
    val document = Jsoup.parseBodyFragment(html)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.body()
  }

  private def getArticleTraits(contents: Seq[ArticleContent]): Seq[SearchTrait] = {
    contents.flatMap(content => {
      val traits = ListBuffer[SearchTrait]()
      parseHtml(content.content)
        .select(EmbedTagName)
        .forEach(embed => {
          val dataResource = embed.attr("data-resource")
          dataResource match {
            case "h5p"                 => traits += SearchTrait.H5p
            case "brightcove" | "nrk"  => traits += SearchTrait.Video
            case "external" | "iframe" =>
              val dataUrl = embed.attr("data-url")
              if (
                dataUrl.contains("youtu") || dataUrl.contains("vimeo") || dataUrl
                  .contains("filmiundervisning") || dataUrl.contains("imdb") || dataUrl
                  .contains("nrk") || dataUrl.contains("khanacademy")
              ) {
                traits += SearchTrait.Video
              }
            case "audio" =>
              val dataType = embed.attr("data-type")
              dataType match {
                case "podcast" => traits += SearchTrait.Podcast
                case _         => traits += SearchTrait.Audio
              }
            case _ => // Do nothing
          }
        })
      traits
    })
  }

  def nodeHitAsMultiSummary(hit: SearchHit, language: String): Try[NodeHitDTO] = permitTry {
    val searchableNode = CirceUtil.tryParseAs[SearchableNode](hit.sourceAsString).?
    val title          = searchableNode.title.getLanguageOrDefault(language).getOrElse("")
    val url            = searchableNode.url.map(urlPath => s"${props.ndlaFrontendUrl}$urlPath")

    val context  = searchableNode.context.map(c => searchableContextToApiContext(c, language))
    val contexts = filterContexts(searchableNode.contexts, language, true)

    Success(
      NodeHitDTO(
        id = searchableNode.nodeId,
        title = title,
        url = url,
        subjectPage = searchableNode.subjectPage.map(subjectPageToSummary(_, language)),
        context = context,
        contexts = contexts
      )
    )
  }

  private def subjectPageToSummary(subjectPage: SearchableSubjectPage, language: String): SubjectPageSummaryDTO = {
    val metaDescription =
      findByLanguageOrBestEffort(subjectPage.domainObject.metaDescription, language)
        .map(meta => MetaDescriptionDTO(meta.metaDescription, meta.language))
        .getOrElse(MetaDescriptionDTO("", UnknownLanguage.toString))

    SubjectPageSummaryDTO(
      id = subjectPage.id,
      name = subjectPage.name,
      metaDescription = metaDescription
    )
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
    val metaImageTuples     =
      metaImage.map(m => EmbedValues(id = List(m.imageId), resource = Some("image"), language = m.language))
    (contentTuples ++ visualElementTuples ++ metaImageTuples).toList

  }

  private def asSearchableTaxonomyContexts(
      taxonomyContexts: List[TaxonomyContext]
  ): List[SearchableTaxonomyContext] = {
    taxonomyContexts.map(context =>
      SearchableTaxonomyContext(
        domainObject = context,
        publicId = context.publicId,
        contextId = context.contextId,
        rootId = context.rootId,
        path = context.path,
        breadcrumbs = context.breadcrumbs,
        contextType = context.contextType.getOrElse(""),
        relevanceId = context.relevanceId,
        resourceTypeIds = context.resourceTypes.map(_.id),
        parentIds = context.parentIds,
        isPrimary = context.isPrimary,
        isActive = context.isActive,
        isVisible = context.isVisible,
        url = context.url
      )
    )
  }

  private def toPlaintext(text: String): String              = Jsoup.parseBodyFragment(text).text()
  private def toPlaintext(lv: LanguageField[String]): String = toPlaintext(lv.value)

  private def getTypeNames(learningResourceType: LearningResourceType): List[String] = {
    learningResourceType match {
      case LearningResourceType.Article =>
        List("article", "artikkel")
      case LearningResourceType.TopicArticle =>
        List("topic", "topic-article", "emne", "emneartikkel")
      case LearningResourceType.FrontpageArticle =>
        List(
          "frontpage",
          "frontpage article",
          "forside",
          "forsideartikkel",
          "om-ndla",
          "about-ndla",
          "om-ndla-artikkel"
        )
      case LearningResourceType.LearningPath =>
        List("learningpath", "læringssti", "sti", "læringsti")
      case LearningResourceType.Concept | LearningResourceType.Gloss =>
        List("concept", "forklaring", "konsept", "glose", "gloss")
    }
  }

  def asSearchableArticle(
      ai: Article,
      indexingBundle: IndexingBundle
  ): Try[SearchableArticle] = {
    val articleId        = ai.id.get
    val taxonomyContexts = indexingBundle.taxonomyBundle match {
      case Some(bundle) =>
        Success(getTaxonomyContexts(articleId, "article", bundle, filterVisibles = true, filterContexts = false))
      case None =>
        taxonomyApiClient.getTaxonomyContext(
          s"urn:article:$articleId",
          filterVisibles = true,
          filterContexts = false,
          shouldUsePublishedTax = true
        )
    }

    val traits               = getArticleTraits(ai.content)
    val embedAttributes      = getAttributesToIndex(ai.content, ai.visualElement)
    val embedResourcesAndIds = getEmbedResourcesAndIdsToIndex(ai.content, ai.visualElement, ai.metaImage)

    val defaultTitle       = getDefault(ai.title)
    val supportedLanguages = getSupportedLanguages(
      ai.title,
      ai.visualElement,
      ai.introduction,
      ai.metaDescription,
      ai.content,
      ai.tags
    ).toList
    val contexts             = asSearchableTaxonomyContexts(taxonomyContexts.getOrElse(List.empty))
    val learningResourceType = LearningResourceType.fromArticleType(ai.articleType)
    val typeNames            = getTypeNames(learningResourceType)

    Success(
      SearchableArticle(
        id = ai.id.get,
        title = SearchableLanguageValues(
          ai.title.map(title => LanguageValue(title.language, toPlaintext(title.title)))
        ),
        content = common.model.api.search.SearchableLanguageValues(
          ai.content.map(article => LanguageValue(article.language, toPlaintext(article.content)))
        ),
        introduction = common.model.api.search.SearchableLanguageValues(
          ai.introduction.map(intro => LanguageValue(intro.language, toPlaintext(intro.introduction)))
        ),
        metaDescription = common.model.api.search.SearchableLanguageValues(
          ai.metaDescription.map(meta => LanguageValue(meta.language, meta.content))
        ),
        tags = SearchableLanguageList(ai.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = ai.updated,
        license = ai.copyright.license,
        status = "PUBLISHED",
        authors = (ai.copyright.creators.map(_.name) ++ ai.copyright.processors
          .map(_.name) ++ ai.copyright.rightsholders.map(_.name)).toList,
        articleType = ai.articleType.entryName,
        metaImage = ai.metaImage.toList,
        defaultTitle = defaultTitle.map(t => t.title),
        supportedLanguages = supportedLanguages,
        context = contexts.find(_.isPrimary),
        contexts = contexts,
        contextids =
          indexingBundle.taxonomyBundle.map(getTaxonomyContexids(articleId, "article", _)).getOrElse(List.empty),
        grepContexts = getGrepContexts(ai.grepCodes, indexingBundle.grepBundle),
        traits = traits.toList.distinct,
        embedAttributes = embedAttributes,
        embedResourcesAndIds = embedResourcesAndIds,
        availability = ai.availability.toString,
        learningResourceType = learningResourceType,
        domainObject = ai,
        typeName = typeNames
      )
    )

  }

  def asSearchableGrep(grepElement: GrepElement): Try[SearchableGrepElement] = {
    val laererplan = grepElement match {
      case lp: BelongsToLaerePlan => Some(lp.`tilhoerer-laereplan`.kode)
      case _                      => None
    }
    val kompetansemaalSett = grepElement match {
      case km: GrepKompetansemaal => Some(km.`tilhoerer-kompetansemaalsett`.kode)
      case _                      => None
    }
    val defaultTitle = grepElement.getTitle.find(_.spraak == "default")
    val titles       = GrepTitle.convertTitles(grepElement.getTitle)
    val title        = SearchableLanguageValues.fromFields(titles)

    val erstattesAv = grepElement match {
      case g: GrepLaererplan => g.`erstattes-av`.map(_.kode)
      case _                 => List.empty
    }

    val gjenbrukAv = grepElement match {
      case g: GrepKompetansemaal => g.`gjenbruk-av`.map(_.kode)
      case _                     => None
    }

    Success(
      SearchableGrepElement(
        code = grepElement.kode,
        title = title,
        defaultTitle = defaultTitle.map(_.verdi),
        belongsTo = List(laererplan, kompetansemaalSett).flatten,
        gjenbrukAv = gjenbrukAv,
        erstattesAv = erstattesAv,
        domainObject = grepElement
      )
    )
  }

  def asSearchableLearningPath(lp: LearningPath, indexingBundle: IndexingBundle): Try[SearchableLearningPath] =
    permitTry {
      val taxonomyContexts = indexingBundle.taxonomyBundle match {
        case Some(bundle) =>
          Success(
            getTaxonomyContexts(lp.id.get, "learningpath", bundle, filterVisibles = true, filterContexts = false)
          )
        case None =>
          taxonomyApiClient.getTaxonomyContext(
            s"urn:learningpath:${lp.id.get}",
            filterVisibles = true,
            filterContexts = false,
            shouldUsePublishedTax = true
          )
      }

      val favorited =
        getFavoritedCountFor(indexingBundle, lp.id.get.toString, List(MyNDLAResourceType.Learningpath)).?

      val supportedLanguages = getSupportedLanguages(lp.title, lp.description).toList
      val defaultTitle       = getDefault(lp.title)
      val license            = api.learningpath.CopyrightDTO(
        asLearningPathApiLicense(lp.copyright.license),
        lp.copyright.contributors.map(c => AuthorDTO(c.`type`, c.name))
      )
      val users    = List(lp.owner)
      val contexts = asSearchableTaxonomyContexts(taxonomyContexts.getOrElse(List.empty))

      val parentTopicName = SearchableLanguageValues(
        taxonomyContexts
          .getOrElse(List.empty)
          .headOption
          .map(context => {
            context.breadcrumbs
              .map(breadcrumbsLanguageValue =>
                breadcrumbsLanguageValue.value.lastOption
                  .map(LanguageValue(breadcrumbsLanguageValue.language, _))
              )
              .flatten
          })
          .getOrElse(Seq.empty)
      )
      val draftStatus = search.SearchableStatus(lp.status.entryName, Seq(lp.status.entryName))

      val primaryContext =
        taxonomyContexts.getOrElse(List.empty).find(tc => tc.isPrimary && tc.rootId.startsWith("urn:subject:"))
      val primaryRoot              = primaryContext.map(_.root).getOrElse(SearchableLanguageValues.empty)
      val sortableResourceTypeName = primaryContext
        .flatMap(context => {
          val typeNames = context.resourceTypes.map(resourceType => resourceType.name)
          Option.when(typeNames.nonEmpty) {
            SearchableLanguageValues.combine(typeNames)
          }
        })
        .getOrElse(
          SearchableLanguageValues.from(
            "nb" -> "Læringssti",
            "nn" -> "Læringssti",
            "en" -> "Learning path"
          )
        )

      Success(
        SearchableLearningPath(
          domainObject = lp,
          id = lp.id.get,
          title =
            common.model.api.search.SearchableLanguageValues(lp.title.map(t => LanguageValue(t.language, t.title))),
          content = common.model.api.search.SearchableLanguageValues(
            lp.title.map(t => LanguageValue(t.language, "*"))
          ),
          description = common.model.api.search
            .SearchableLanguageValues(lp.description.map(d => LanguageValue(d.language, d.description))),
          introduction = common.model.api.search.SearchableLanguageValues(
            lp.introduction.map(i => LanguageValue(i.language, i.introduction))
          ),
          coverPhotoId = lp.coverPhotoId,
          duration = lp.duration,
          status = lp.status.toString,
          draftStatus = draftStatus,
          owner = lp.owner,
          users = users,
          verificationStatus = lp.verificationStatus.toString,
          lastUpdated = lp.lastUpdated,
          defaultTitle = defaultTitle.map(_.title),
          tags = SearchableLanguageList(lp.tags.map(tag => LanguageValue(tag.language, tag.tags))),
          learningsteps = lp.learningsteps.getOrElse(Seq.empty).map(asSearchableLearningStep).toList,
          license = lp.copyright.license,
          copyright = license,
          isBasedOn = lp.isBasedOn,
          supportedLanguages = supportedLanguages,
          authors = lp.copyright.contributors.map(_.name).toList,
          context = contexts.find(_.isPrimary),
          contexts = contexts,
          contextids = indexingBundle.taxonomyBundle
            .map(getTaxonomyContexids(lp.id.get, "learningpath", _))
            .getOrElse(List.empty),
          favorited = favorited,
          learningResourceType = LearningResourceType.LearningPath,
          typeName = getTypeNames(LearningResourceType.LearningPath),
          priority = lp.priority,
          defaultParentTopicName = parentTopicName.defaultValue,
          parentTopicName = parentTopicName,
          defaultRoot = primaryRoot.defaultValue,
          primaryRoot = primaryRoot,
          resourceTypeName = sortableResourceTypeName,
          defaultResourceTypeName = sortableResourceTypeName.defaultValue,
          revisionMeta = lp.revisionMeta.toList,
          nextRevision = lp.revisionMeta.getNextRevision,
          grepCodes = lp.grepCodes.toList,
          responsible = lp.responsible
        )
      )
    }

  private def getFavoritedCountFor(
      indexingBundle: IndexingBundle,
      id: String,
      resourceTypes: List[MyNDLAResourceType]
  ): Try[Long] = {
    indexingBundle.myndlaBundle match {
      case Some(value) => Success(value.getFavorites(id, resourceTypes))
      case None        =>
        myndlaApiClient
          .getStatsFor(id, resourceTypes)
          .map(_.map(_.favourites).sum)
    }
  }

  def asSearchableConcept(c: Concept, indexingBundle: IndexingBundle): Try[SearchableConcept] = permitTry {
    val title = common.model.api.search
      .SearchableLanguageValues(c.title.map(t => LanguageValue(t.language, toPlaintext(t.title))))
    val content   = SearchableLanguageValues.fromFieldsMap(c.content)(toPlaintext)
    val tags      = SearchableLanguageList.fromFields(c.tags)
    val favorited = getFavoritedCountFor(indexingBundle, c.id.get.toString, List(MyNDLAResourceType.Concept)).?

    val authors = (
      c.copyright.map(_.creators).toList ++
        c.copyright.map(_.processors).toList ++
        c.copyright.map(_.rightsholders).toList
    ).flatten.map(_.name)

    val users: Seq[String] = c.updatedBy ++ c.editorNotes.map(_.user)

    val status               = StatusDTO(c.status.current.toString, c.status.other.map(_.toString).toSeq)
    val learningResourceType = LearningResourceType.fromConceptType(c.conceptType)

    Success(
      SearchableConcept(
        id = c.id.get,
        conceptType = c.conceptType.entryName,
        title = title,
        content = content,
        defaultTitle = title.defaultValue,
        tags = tags,
        lastUpdated = c.updated,
        draftStatus = status,
        users = users.toList,
        updatedBy = c.updatedBy,
        license = c.copyright.flatMap(_.license),
        authors = authors,
        created = c.created,
        source = c.copyright.flatMap(_.origin),
        responsible = c.responsible,
        gloss = c.glossData.map(_.gloss),
        domainObject = c,
        favorited = favorited,
        learningResourceType = learningResourceType,
        typeName = getTypeNames(learningResourceType)
      )
    )
  }

  def asSearchableDraft(draft: Draft, indexingBundle: IndexingBundle): Try[SearchableDraft] = permitTry {
    val taxonomyContexts = {
      val draftId = draft.id.get
      indexingBundle.taxonomyBundle match {
        case Some(bundle) =>
          Success(getTaxonomyContexts(draftId, "article", bundle, filterVisibles = false, filterContexts = false))
        case None =>
          taxonomyApiClient.getTaxonomyContext(
            s"urn:article:$draftId",
            filterVisibles = false,
            filterContexts = false,
            shouldUsePublishedTax = false
          )
      }
    }.getOrElse(List.empty)

    val traits               = getArticleTraits(draft.content)
    val embedAttributes      = getAttributesToIndex(draft.content, draft.visualElement)
    val embedResourcesAndIds = getEmbedResourcesAndIdsToIndex(draft.content, draft.visualElement, draft.metaImage)

    val defaultTitle = getDefault(draft.title)

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
    val nextRevision = draft.revisionMeta.getNextRevision
    val draftStatus  = search.SearchableStatus(draft.status.current.toString, draft.status.other.map(_.toString).toSeq)

    val parentTopicName = SearchableLanguageValues(
      taxonomyContexts.headOption
        .map(context => {
          context.breadcrumbs
            .map(breadcrumbsLanguageValue =>
              breadcrumbsLanguageValue.value.lastOption
                .map(LanguageValue(breadcrumbsLanguageValue.language, _))
            )
            .flatten
        })
        .getOrElse(Seq.empty)
    )

    val primaryContext           = taxonomyContexts.find(tc => tc.isPrimary && tc.rootId.startsWith("urn:subject:"))
    val primaryRoot              = primaryContext.map(_.root).getOrElse(SearchableLanguageValues.empty)
    val sortableResourceTypeName = getSortableResourceTypeName(draft, taxonomyContexts)

    val favorited = (indexingBundle.myndlaBundle match {
      case Some(value) =>
        Success(
          value.getFavorites(
            draft.id.get.toString,
            List(MyNDLAResourceType.Article, MyNDLAResourceType.Multidisciplinary, MyNDLAResourceType.Topic)
          )
        )
      case None =>
        myndlaApiClient
          .getStatsFor(
            draft.id.get.toString,
            List(MyNDLAResourceType.Article, MyNDLAResourceType.Multidisciplinary, MyNDLAResourceType.Topic)
          )
          .map(_.map(_.favourites).sum)
    }).?

    val title                = SearchableLanguageValues.fromFieldsMap(draft.title)(toPlaintext)
    val content              = SearchableLanguageValues.fromFieldsMap(draft.content)(toPlaintext)
    val introduction         = SearchableLanguageValues.fromFieldsMap(draft.introduction)(toPlaintext)
    val metaDescription      = SearchableLanguageValues.fromFields(draft.metaDescription)
    val contexts             = asSearchableTaxonomyContexts(taxonomyContexts)
    val learningResourceType = LearningResourceType.fromArticleType(draft.articleType)
    val typeNames            = getTypeNames(learningResourceType)

    Success(
      SearchableDraft(
        id = draft.id.get,
        title = title,
        content = content,
        introduction = introduction,
        metaDescription = metaDescription,
        tags = SearchableLanguageList(draft.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        lastUpdated = draft.updated,
        license = draft.copyright.flatMap(_.license),
        authors = authors,
        articleType = draft.articleType.entryName,
        defaultTitle = defaultTitle.map(t => t.title),
        supportedLanguages = supportedLanguages,
        notes = notes,
        context = contexts.find(_.isPrimary),
        contexts = contexts,
        contextids =
          indexingBundle.taxonomyBundle.map(getTaxonomyContexids(draft.id.get, "article", _)).getOrElse(List.empty),
        draftStatus = draftStatus,
        status = draft.status.current.toString,
        users = users.distinct,
        previousVersionsNotes = draft.previousVersionsNotes.map(_.note).toList,
        grepContexts = getGrepContexts(draft.grepCodes, indexingBundle.grepBundle),
        traits = traits.toList.distinct,
        embedAttributes = embedAttributes,
        embedResourcesAndIds = embedResourcesAndIds,
        revisionMeta = draft.revisionMeta.toList,
        nextRevision = nextRevision,
        responsible = draft.responsible,
        domainObject = draft,
        priority = draft.priority,
        defaultParentTopicName = parentTopicName.defaultValue,
        parentTopicName = parentTopicName,
        defaultRoot = primaryRoot.defaultValue,
        primaryRoot = primaryRoot,
        resourceTypeName = sortableResourceTypeName,
        defaultResourceTypeName = sortableResourceTypeName.defaultValue,
        published = draft.published,
        favorited = favorited,
        learningResourceType = learningResourceType,
        typeName = typeNames
      )
    )
  }

  private def getSortableResourceTypeName(
      draft: Draft,
      taxonomyContexts: List[TaxonomyContext]
  ): SearchableLanguageValues = {
    draft.articleType match {
      case ArticleType.Standard =>
        taxonomyContexts.headOption
          .flatMap(context => {
            val typeNames = context.resourceTypes.map(resourceType => resourceType.name)
            Option.when(typeNames.nonEmpty) {
              SearchableLanguageValues.combine(typeNames)
            }
          })
          .getOrElse(
            SearchableLanguageValues.from(
              "nb" -> "Læringsressurs",
              "nn" -> "Læringsressurs",
              "en" -> "Subject matter"
            )
          )
      case ArticleType.TopicArticle =>
        SearchableLanguageValues.from(
          "nb" -> "Emne",
          "nn" -> "Emne",
          "en" -> "Topic"
        )
      case ArticleType.FrontpageArticle =>
        SearchableLanguageValues.from(
          "nb" -> "Om-NDLA-artikkel",
          "nn" -> "Om-NDLA-artikkel",
          "en" -> "About-NDLA article"
        )
    }
  }

  private def asLearningPathApiLicense(license: String): LicenseDTO = {
    getLicense(license) match {
      case Some(l) => LicenseDTO(l.license.toString, Option(l.description), l.url)
      case None    => LicenseDTO(license, Some("Invalid license"), None)
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

      sortLanguagesByPriority(languagesInKeys).headOption
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

  private def getHighlights(highlights: Map[String, Seq[String]]): List[HighlightedFieldDTO] = {
    highlights.map { case (field, matches) =>
      HighlightedFieldDTO(
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
  ): List[ApiTaxonomyContextDTO] = {
    val filtered = contexts.filter { c =>
      // Filter inactive if required, and also don't show programme contexts
      (!filterInactive || c.isActive) && !c.rootId.startsWith("urn:programme:")
    }
    filtered.sortBy(!_.isPrimary).map(c => searchableContextToApiContext(c, language))
  }

  def articleHitAsMultiSummary(
      hit: SearchHit,
      language: String,
      filterInactive: Boolean
  ): Try[MultiSearchSummaryDTO] = permitTry {
    val searchableArticle = CirceUtil.tryParseAs[SearchableArticle](hit.sourceAsString).?

    val context  = searchableArticle.context.map(c => searchableContextToApiContext(c, language))
    val contexts = filterContexts(searchableArticle.contexts, language, filterInactive)
    val titles   = searchableArticle.domainObject.title.map(title =>
      TitleWithHtmlDTO(Jsoup.parseBodyFragment(title.title).body().text(), title.title, title.language)
    )
    val introductions = searchableArticle.domainObject.introduction.map(intro =>
      api.article
        .ArticleIntroductionDTO(
          Jsoup.parseBodyFragment(intro.introduction).body().text(),
          intro.introduction,
          intro.language
        )
    )
    val metaDescriptions =
      searchableArticle.metaDescription.languageValues.map(lv => MetaDescriptionDTO(lv.value, lv.language))
    val visualElements =
      searchableArticle.domainObject.visualElement.map(lv => api.article.VisualElementDTO(lv.value, lv.language))
    val metaImages = searchableArticle.metaImage.map(image => {
      val metaImageUrl = s"${props.ExternalApiUrls("raw-image")}/${image.imageId}"
      MetaImageDTO(metaImageUrl, image.altText, image.language)
    })

    val title =
      findByLanguageOrBestEffort(titles, language).getOrElse(
        common.model.api.search.TitleWithHtmlDTO("", "", UnknownLanguage.toString)
      )
    val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
      common.model.api.search.MetaDescriptionDTO("", UnknownLanguage.toString)
    )
    val metaImage = findByLanguageOrBestEffort(metaImages, language)

    val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)

    val url = s"${props.ExternalApiUrls("article-api")}/${searchableArticle.id}"

    Success(
      MultiSearchSummaryDTO(
        id = searchableArticle.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = metaImage,
        url = url,
        context = context,
        contexts = contexts,
        supportedLanguages = supportedLanguages,
        learningResourceType = searchableArticle.learningResourceType,
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
        priority = None,
        resourceTypeName = None,
        parentTopicName = None,
        primaryRootName = None,
        published = None,
        favorited = None,
        resultType = SearchType.Articles
      )
    )
  }

  def draftHitAsMultiSummary(
      hit: SearchHit,
      language: String,
      filterInactive: Boolean
  ): Try[MultiSearchSummaryDTO] = permitTry {
    val searchableDraft = CirceUtil.tryParseAs[SearchableDraft](hit.sourceAsString).?

    val context  = searchableDraft.context.map(c => searchableContextToApiContext(c, language))
    val contexts = filterContexts(searchableDraft.contexts, language, filterInactive)
    val titles   = searchableDraft.domainObject.title.map(title =>
      common.model.api.search
        .TitleWithHtmlDTO(Jsoup.parseBodyFragment(title.title).body().text(), title.title, title.language)
    )
    val introductions = searchableDraft.domainObject.introduction.map(intro =>
      api.article
        .ArticleIntroductionDTO(
          Jsoup.parseBodyFragment(intro.introduction).body().text(),
          intro.introduction,
          intro.language
        )
    )
    val metaDescriptions =
      searchableDraft.metaDescription.languageValues.map(lv =>
        common.model.api.search.MetaDescriptionDTO(lv.value, lv.language)
      )
    val visualElements =
      searchableDraft.domainObject.visualElement.map(lv => api.article.VisualElementDTO(lv.value, lv.language))
    val metaImages = searchableDraft.domainObject.metaImage.map(image => {
      val metaImageUrl = s"${props.ExternalApiUrls("raw-image")}/${image.imageId}"
      common.model.api.search.MetaImageDTO(metaImageUrl, image.altText, image.language)
    })

    val title =
      findByLanguageOrBestEffort(titles, language).getOrElse(
        common.model.api.search.TitleWithHtmlDTO("", "", UnknownLanguage.toString)
      )
    val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
      common.model.api.search.MetaDescriptionDTO("", UnknownLanguage.toString)
    )
    val metaImage          = findByLanguageOrBestEffort(metaImages, language)
    val supportedLanguages = getSupportedLanguages(titles, visualElements, introductions, metaDescriptions)
    val url                = s"${props.ExternalApiUrls("draft-api")}/${searchableDraft.id}"
    val revisions          =
      searchableDraft.revisionMeta.map(m => RevisionMetaDTO(m.revisionDate, m.note, m.status.entryName))
    val responsible = searchableDraft.responsible.map(r => ResponsibleDTO(r.responsibleId, r.lastUpdated))
    val comments    =
      searchableDraft.domainObject.comments.map(c =>
        CommentDTO(c.id.toString, c.content, c.created, c.updated, c.isOpen, c.solved)
      )

    val resourceTypeName = searchableDraft.resourceTypeName.getLanguageOrDefault(language)
    val parentTopicName  = searchableDraft.parentTopicName.getLanguageOrDefault(language)
    val primaryRootName  = searchableDraft.primaryRoot.getLanguageOrDefault(language)

    Success(
      MultiSearchSummaryDTO(
        id = searchableDraft.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = metaImage,
        url = url,
        context = context,
        contexts = contexts,
        supportedLanguages = supportedLanguages,
        learningResourceType = searchableDraft.learningResourceType,
        status = Some(
          common.model.api.search.StatusDTO(searchableDraft.draftStatus.current, searchableDraft.draftStatus.other)
        ),
        traits = searchableDraft.traits,
        score = hit.score,
        highlights = getHighlights(hit.highlight),
        paths = getPathsFromContext(searchableDraft.contexts),
        lastUpdated = searchableDraft.lastUpdated,
        license = searchableDraft.license,
        revisions = revisions,
        responsible = responsible,
        comments = Some(comments),
        priority = Some(searchableDraft.priority),
        resourceTypeName = resourceTypeName,
        parentTopicName = parentTopicName,
        primaryRootName = primaryRootName,
        published = Some(searchableDraft.published),
        favorited = Some(searchableDraft.favorited),
        resultType = SearchType.Drafts
      )
    )
  }

  def learningpathHitAsMultiSummary(
      hit: SearchHit,
      language: String,
      filterInactive: Boolean
  ): Try[MultiSearchSummaryDTO] = permitTry {
    val searchableLearningPath = CirceUtil.tryParseAs[SearchableLearningPath](hit.sourceAsString).?

    val context  = searchableLearningPath.context.map(c => searchableContextToApiContext(c, language))
    val contexts = filterContexts(searchableLearningPath.contexts, language, filterInactive)
    val titles   =
      searchableLearningPath.title.languageValues.map(lv =>
        common.model.api.search.TitleWithHtmlDTO(lv.value, lv.value, lv.language)
      )
    val metaDescriptions =
      searchableLearningPath.description.languageValues.map(lv =>
        common.model.api.search.MetaDescriptionDTO(lv.value, lv.language)
      )
    val tags =
      searchableLearningPath.tags.languageValues.map(lv => api.learningpath.LearningPathTagsDTO(lv.value, lv.language))

    val supportedLanguages = getSupportedLanguages(titles, metaDescriptions, tags)

    val title =
      findByLanguageOrBestEffort(titles, language).getOrElse(
        common.model.api.search.TitleWithHtmlDTO("", "", UnknownLanguage.toString)
      )
    val metaDescription = findByLanguageOrBestEffort(metaDescriptions, language).getOrElse(
      common.model.api.search.MetaDescriptionDTO("", UnknownLanguage.toString)
    )
    val comments =
      searchableLearningPath.domainObject.comments.map(c =>
        CommentDTO(c.id.toString, c.content, c.created, c.updated, c.isOpen, c.solved)
      )
    val url       = s"${props.ExternalApiUrls("learningpath-api")}/${searchableLearningPath.id}"
    val metaImage =
      searchableLearningPath.coverPhotoId.map(id =>
        common.model.api.search.MetaImageDTO(
          url = s"${props.ExternalApiUrls("raw-image")}/$id",
          alt = "",
          language = language
        )
      )
    val responsible = searchableLearningPath.responsible.map(r => ResponsibleDTO(r.responsibleId, r.lastUpdated))

    val resourceTypeName = searchableLearningPath.resourceTypeName.getLanguageOrDefault(language)
    val parentTopicName  = searchableLearningPath.parentTopicName.getLanguageOrDefault(language)
    val primaryRootName  = searchableLearningPath.primaryRoot.getLanguageOrDefault(language)

    Success(
      MultiSearchSummaryDTO(
        id = searchableLearningPath.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = metaImage,
        url = url,
        context = context,
        contexts = contexts,
        supportedLanguages = supportedLanguages,
        learningResourceType = LearningResourceType.LearningPath,
        status = Some(common.model.api.search.StatusDTO(searchableLearningPath.status, Seq.empty)),
        traits = List.empty,
        score = hit.score,
        highlights = getHighlights(hit.highlight),
        paths = getPathsFromContext(searchableLearningPath.contexts),
        lastUpdated = searchableLearningPath.lastUpdated,
        license = Some(searchableLearningPath.license),
        revisions = Seq.empty,
        responsible = responsible,
        comments = Some(comments),
        priority = Some(searchableLearningPath.priority),
        resourceTypeName = resourceTypeName,
        parentTopicName = parentTopicName,
        primaryRootName = primaryRootName,
        published = None,
        favorited = Some(searchableLearningPath.favorited),
        resultType = SearchType.LearningPaths
      )
    )
  }

  def conceptHitAsMultiSummary(hit: SearchHit, language: String): Try[MultiSearchSummaryDTO] = permitTry {
    val searchableConcept = CirceUtil.tryParseAs[SearchableConcept](hit.sourceAsString).?

    val titles =
      searchableConcept.title.languageValues.map(lv =>
        common.model.api.search.TitleWithHtmlDTO(lv.value, lv.value, lv.language)
      )

    val content = searchableConcept.content.languageValues.map(lv =>
      common.model.api.search.MetaDescriptionDTO(lv.value, lv.language)
    )
    val tags = searchableConcept.tags.languageValues.map(lv => Tag(lv.value, lv.language))

    val supportedLanguages = getSupportedLanguages(titles, content, tags)

    val title =
      findByLanguageOrBestEffort(titles, language).getOrElse(
        common.model.api.search.TitleWithHtmlDTO("", "", UnknownLanguage.toString)
      )
    val url = s"${props.ExternalApiUrls("concept-api")}/${searchableConcept.id}"

    val responsible =
      searchableConcept.responsible.map(r => common.model.api.ResponsibleDTO(r.responsibleId, r.lastUpdated))
    val metaDescription = findByLanguageOrBestEffort(content, language).getOrElse(
      common.model.api.search.MetaDescriptionDTO("", UnknownLanguage.toString)
    )

    Success(
      MultiSearchSummaryDTO(
        id = searchableConcept.id,
        title = title,
        metaDescription = metaDescription,
        metaImage = None,
        url = url,
        context = None,
        contexts = List.empty,
        supportedLanguages = supportedLanguages,
        learningResourceType = searchableConcept.learningResourceType,
        status = Some(searchableConcept.draftStatus),
        traits = List.empty,
        score = hit.score,
        highlights = getHighlights(hit.highlight),
        paths = List.empty,
        lastUpdated = searchableConcept.lastUpdated,
        license = searchableConcept.license,
        revisions = Seq.empty,
        responsible = responsible,
        comments = None,
        priority = None,
        resourceTypeName = None,
        parentTopicName = None,
        primaryRootName = None,
        published = None,
        favorited = Some(searchableConcept.favorited),
        resultType = SearchType.Concepts
      )
    )
  }

  private def searchableContextToApiContext(
      context: SearchableTaxonomyContext,
      language: String
  ): ApiTaxonomyContextDTO = {
    val subjectName =
      findByLanguageOrBestEffort(context.domainObject.root.languageValues, language).map(_.value).getOrElse("")
    val breadcrumbs = findByLanguageOrBestEffort(context.breadcrumbs.languageValues, language)
      .map(_.value)
      .getOrElse(Seq.empty)
      .toList

    val resourceTypes = context.domainObject.resourceTypes.map(rt => {
      val name = findByLanguageOrBestEffort(rt.name.languageValues, language)
        .getOrElse(LanguageValue(UnknownLanguage.toString, ""))
      TaxonomyResourceTypeDTO(id = rt.id, name = name.value, language = name.language)
    })

    val relevance =
      findByLanguageOrBestEffort(context.domainObject.relevance.languageValues, language).map(_.value).getOrElse("")

    ApiTaxonomyContextDTO(
      publicId = context.publicId,
      root = subjectName,
      rootId = context.rootId,
      relevance = relevance,
      relevanceId = context.relevanceId,
      path = context.path,
      breadcrumbs = breadcrumbs,
      contextId = context.contextId,
      contextType = context.contextType,
      resourceTypes = resourceTypes,
      language = language,
      isPrimary = context.isPrimary,
      isActive = context.isActive,
      url = context.url
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

  /** Parses [[TaxonomyBundle]] to get all contextids for a single node.
    *
    * @param id
    *   of article/learningpath
    * @param taxonomyType
    *   Type of resource used in contentUri. Example: "learningpath" in "urn:learningpath:123"
    * @param bundle
    *   All taxonomy in an object.
    * @return
    *   List of strings to be indexed.
    */
  private def getTaxonomyContexids(id: Long, taxonomyType: String, bundle: TaxonomyBundle) = {
    val nodes = bundle.nodeByContentUri.getOrElse(s"urn:$taxonomyType:$id", List.empty)
    nodes.flatMap(node => node.contextids)
  }

  private[service] def getGrepContexts(
      grepCodes: Seq[String],
      bundle: Option[GrepBundle]
  ): List[SearchableGrepContext] = {
    bundle match {
      case None             => List.empty
      case Some(grepBundle) =>
        grepCodes.flatMap { grepCode =>
          grepBundle.grepContextByCode
            .get(grepCode) match {
            case Some(element: GrepKompetansemaalSett) =>
              val subContexts = getGrepContexts(element.kompetansemaal.map(_.kode), bundle)
              subContexts :+ SearchableGrepContext(
                code = grepCode,
                title = element.getTitleValue("default"),
                status = element.status.entryName
              )
            case Some(element) =>
              List(
                SearchableGrepContext(
                  code = grepCode,
                  title = element.getTitleValue("default"),
                  status = element.status.entryName
                )
              )
            case None => List(SearchableGrepContext(code = grepCode, title = None, status = ""))
          }
        }.toList
    }
  }

  def toApiMultiSearchResult(searchResult: domain.SearchResult): MultiSearchResultDTO = {
    common.model.api.search.MultiSearchResultDTO(
      searchResult.totalCount,
      searchResult.page,
      searchResult.pageSize,
      searchResult.language,
      searchResult.results,
      searchResult.suggestions,
      searchResult.aggregations.map(toApiMultiTermsAggregation)
    )
  }

  def toApiGroupMultiSearchResult(group: String, searchResult: domain.SearchResult): GroupSearchResultDTO =
    api.GroupSearchResultDTO(
      searchResult.totalCount,
      searchResult.page,
      searchResult.pageSize,
      searchResult.language,
      searchResult.results,
      searchResult.suggestions,
      searchResult.aggregations.map(toApiMultiTermsAggregation),
      group
    )

  private def asFrontPage(frontpage: Option[SubjectPage]): Try[Option[SearchableSubjectPage]] = {
    frontpage match {
      case None     => Success(None)
      case Some(fp) =>
        fp.id match {
          case None =>
            Failure(MissingIdException("Missing id for fetched frontpage. This is weird and probably a bug."))
          case Some(id) =>
            val aboutTitles       = SearchableLanguageValues.fromFieldsMap[AboutSubject](fp.about)(_.title)
            val aboutDescriptions = SearchableLanguageValues.fromFieldsMap[AboutSubject](fp.about)(_.description)
            val metaDescriptions  = SearchableLanguageValues.fromFields(fp.metaDescription)
            Success(
              Some(
                SearchableSubjectPage(
                  id = id,
                  name = fp.name,
                  aboutTitle = aboutTitles,
                  aboutDescription = aboutDescriptions,
                  metaDescription = metaDescriptions,
                  domainObject = fp
                )
              )
            )
        }
    }
  }

  def asSearchableNode(
      node: Node,
      frontpage: Option[SubjectPage],
      indexingBundle: IndexingBundle
  ): Try[SearchableNode] = {
    asFrontPage(frontpage).map { frontpage =>
      val context      = node.context.map(ctx => asSearchableTaxonomyContexts(List(ctx)).head)
      val contexts     = asSearchableTaxonomyContexts(node.contexts)
      val grepContexts =
        node.metadata.map(meta => getGrepContexts(meta.grepCodes, indexingBundle.grepBundle)).getOrElse(List.empty)

      val typeNames = node.nodeType match {
        case NodeType.NODE      => List("node")
        case NodeType.SUBJECT   => List("fag", "subject")
        case NodeType.TOPIC     => List("emne", "topic")
        case NodeType.RESOURCE  => List("ressurs", "resource")
        case NodeType.PROGRAMME => List("programfag", "program", "programme")
      }

      SearchableNode(
        nodeId = node.id,
        title = getSearchableLanguageValues(node.name, node.translations),
        contentUri = node.contentUri,
        url = node.url,
        nodeType = node.nodeType,
        subjectPage = frontpage,
        context = context.orElse(contexts.find(_.isPrimary)),
        contexts = contexts,
        grepContexts = grepContexts,
        typeName = typeNames
      )
    }
  }
}
