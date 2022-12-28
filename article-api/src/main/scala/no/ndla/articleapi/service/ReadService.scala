/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.{Path, Url}
import no.ndla.articleapi.Props
import no.ndla.articleapi.caching.MemoizeHelpers
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.{ArticleSummaryV2, ErrorHelpers, NotFoundException}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.{ArticleSearchService, SearchConverterService}
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.{ArticleType, Availability}
import no.ndla.network.clients.FeideApiClient
import no.ndla.validation.HtmlTagRules.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.validation.{ResourceType, TagAttributes}
import org.jsoup.nodes.Element

import scala.jdk.CollectionConverters._
import scala.math.max
import scala.util.{Failure, Success, Try}

trait ReadService {
  this: ArticleRepository
    with FeideApiClient
    with ConverterService
    with ArticleSearchService
    with SearchConverterService
    with MemoizeHelpers
    with Props
    with ErrorHelpers =>
  val readService: ReadService

  class ReadService extends StrictLogging {
    import ErrorHelpers.ArticleGoneException

    def getInternalIdByExternalId(externalId: Long): Option[api.ArticleIdV2] =
      articleRepository.getIdFromExternalId(externalId.toString).map(api.ArticleIdV2)

    def withIdV2(
        id: Long,
        language: String,
        fallback: Boolean,
        revision: Option[Int],
        feideAccessToken: Option[String]
    ): Try[Cachable[api.ArticleV2]] = {
      val article = revision match {
        case Some(rev) => articleRepository.withIdAndRevision(id, rev)
        case None      => articleRepository.withId(id)
      }

      article.mapArticle(addUrlsOnEmbedResources) match {
        case None                               => Failure(NotFoundException(s"The article with id $id was not found"))
        case Some(ArticleRow(_, _, _, _, None)) => Failure(ArticleGoneException())
        case Some(ArticleRow(_, _, _, _, Some(article))) if article.availability == Availability.everyone =>
          Cachable.yes(converterService.toApiArticleV2(article, language, fallback))
        case Some(ArticleRow(_, _, _, _, Some(article))) =>
          feideApiClient
            .getFeideExtendedUser(feideAccessToken)
            .flatMap(feideUser =>
              article.availability match {
                case Availability.teacher if !feideUser.isTeacher =>
                  Failure(AccessDeniedException("User is missing required role(s) to perform this operation"))
                case _ =>
                  Cachable.no(converterService.toApiArticleV2(article, language, fallback))
              }
            )
      }
    }

    def getArticleBySlug(slug: String, language: String, fallback: Boolean = false): Try[Cachable[api.ArticleV2]] = {
      articleRepository.withSlug(slug) match {
        case None => Failure(NotFoundException(s"The article with slug '$slug' was not found"))
        case Some(ArticleRow(_, _, _, _, None)) => Failure(ArticleGoneException())
        case Some(ArticleRow(_, _, _, _, Some(article))) if article.availability == Availability.everyone =>
          Cachable.yes(converterService.toApiArticleV2(article, language, fallback))
        case Some(ArticleRow(_, _, _, _, Some(article))) =>
          Cachable.yes(converterService.toApiArticleV2(article, language, fallback))
      }
    }

    private[service] def addUrlsOnEmbedResources(article: Article): Article = {
      val articleWithUrls = article.content.map(content => content.copy(content = addUrlOnResource(content.content)))
      val visualElementWithUrls =
        article.visualElement.map(visual => visual.copy(resource = addUrlOnResource(visual.resource)))

      article.copy(content = articleWithUrls, visualElement = visualElementWithUrls)
    }

    def getAllTags(input: String, pageSize: Int, offset: Int, language: String): api.TagsSearchResult = {
      val (tags, tagsCount) = articleRepository.getTags(input, pageSize, (offset - 1) * pageSize, language)
      converterService.toApiArticleTags(tags, tagsCount, pageSize, offset, language)
    }

    def getArticlesByPage(pageNo: Int, pageSize: Int, lang: String, fallback: Boolean = false): api.ArticleDump = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
      val results = articleRepository
        .getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize)
        .flatMap(article => converterService.toApiArticleV2(article, lang, fallback).toOption)

      api.ArticleDump(articleRepository.articleCount, pageNo, pageSize, lang, results)
    }

    def getArticleDomainDump(pageNo: Int, pageSize: Int): api.ArticleDomainDump = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
      val results =
        articleRepository.getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize).map(addUrlsOnEmbedResources)

      api.ArticleDomainDump(articleRepository.articleCount, pageNo, pageSize, results)
    }

    private[service] def addUrlOnResource(content: String): String = {
      val doc = stringToJsoupDocument(content)

      val embedTags = doc.select(EmbedTagName).asScala.toList
      embedTags.foreach(addUrlOnEmbedTag)
      jsoupDocumentToString(doc)
    }

    private def addUrlOnEmbedTag(embedTag: Element): Unit = {
      val typeAndPathOption = embedTag.attr(TagAttributes.DataResource.toString) match {
        case resourceType
            if resourceType == ResourceType.File.toString
              || resourceType == ResourceType.H5P.toString
              && embedTag.hasAttr(TagAttributes.DataPath.toString) =>
          val path = embedTag.attr(TagAttributes.DataPath.toString)
          Some((resourceType, path))

        case resourceType if embedTag.hasAttr(TagAttributes.DataResource_Id.toString) =>
          val id = embedTag.attr(TagAttributes.DataResource_Id.toString)
          Some((resourceType, id))
        case _ =>
          None
      }

      typeAndPathOption match {
        case Some((resourceType, path)) =>
          val baseUrl   = Url.parse(props.externalApiUrls(resourceType))
          val pathParts = Path.parse(path).parts

          embedTag.attr(
            s"${TagAttributes.DataUrl}",
            baseUrl.addPathParts(pathParts).toString
          )
        case _ =>
      }
    }

    def getRevisions(articleId: Long): Try[Seq[Int]] = {
      articleRepository.getRevisions(articleId) match {
        case Nil       => Failure(NotFoundException(s"Could not find any revisions for article with id $articleId"))
        case revisions => Success(revisions)
      }
    }

    def getArticleIdsByExternalId(externalId: String): Option[api.ArticleIds] =
      articleRepository.getArticleIdsFromExternalId(externalId).map(converterService.toApiArticleIds)

    def search(
        query: Option[String],
        sort: Option[Sort],
        language: String,
        license: Option[String],
        page: Int,
        pageSize: Int,
        idList: List[Long],
        articleTypesFilter: Seq[String],
        fallback: Boolean,
        grepCodes: Seq[String],
        shouldScroll: Boolean,
        feideAccessToken: Option[String]
    ): Try[Cachable[SearchResult[ArticleSummaryV2]]] = {
      val availabilities = feideApiClient.getFeideExtendedUser(feideAccessToken) match {
        case Success(user) => user.availabilities
        case Failure(ex) =>
          logger.warn("Something went wrong when fetching feideuser, assuming non-user")
          Seq.empty
      }

      val settings = query match {
        case Some(q) =>
          SearchSettings(
            query = Some(q),
            withIdIn = idList,
            language = language,
            license = license,
            page = page,
            pageSize = if (idList.isEmpty) pageSize else idList.size,
            sort = sort.getOrElse(Sort.ByRelevanceDesc),
            if (articleTypesFilter.isEmpty) ArticleType.all else articleTypesFilter,
            fallback = fallback,
            grepCodes = grepCodes,
            shouldScroll = shouldScroll,
            availability = availabilities
          )

        case None =>
          SearchSettings(
            query = None,
            withIdIn = idList,
            language = language,
            license = license,
            page = page,
            pageSize = if (idList.isEmpty) pageSize else idList.size,
            sort = sort.getOrElse(Sort.ByIdAsc),
            if (articleTypesFilter.isEmpty) ArticleType.all else articleTypesFilter,
            fallback = fallback,
            grepCodes = grepCodes,
            shouldScroll = shouldScroll,
            availability = availabilities
          )
      }

      val result       = articleSearchService.matchingQuery(settings)
      val isRestricted = !settings.availability.distinct.forall(_ == Availability.everyone)
      if (isRestricted)
        Cachable.no(result)
      else
        Cachable.yes(result)
    }

    private def getAvailabilityFilter(feideAccessToken: Option[String]): Option[Availability.Value] = {
      feideApiClient.getFeideExtendedUser(feideAccessToken) match {
        case Success(user) if user.isTeacher => None
        case _                               => Some(Availability.everyone)
      }
    }

    private def applyAvailabilityFilter(feideAccessToken: Option[String], articles: Seq[Article]): Seq[Article] = {
      val availabilityFilter = getAvailabilityFilter(feideAccessToken)
      val filteredArticles = availabilityFilter
        .map(avaFilter => articles.filter(article => article.availability == avaFilter))
        .getOrElse(articles)
      filteredArticles
    }

    def getArticlesByIds(
        articleIds: List[Long],
        language: String,
        fallback: Boolean,
        page: Long,
        pageSize: Long,
        feideAccessToken: Option[String] = None
    ): Try[Seq[api.ArticleV2]] = {
      if (articleIds.isEmpty) Failure(ValidationException("ids", "Query parameter 'ids' is missing"))
      else {
        val offset         = (page - 1) * pageSize
        val domainArticles = articleRepository.withIds(articleIds, offset, pageSize).toArticles
        val isFeideNeeded  = domainArticles.exists(article => article.availability == Availability.teacher)
        val filtered = if (isFeideNeeded) applyAvailabilityFilter(feideAccessToken, domainArticles) else domainArticles
        filtered.traverse(article => converterService.toApiArticleV2(article, language, fallback))
      }
    }

  }

}
