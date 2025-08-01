/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service

import cats.implicits.*
import io.lemonlabs.uri.{Path, Url}
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.draft.DraftStatus.PUBLISHED
import no.ndla.database.DBUtility
import no.ndla.draftapi.DraftUtil.shouldPartialPublish
import no.ndla.draftapi.Props
import no.ndla.draftapi.caching.MemoizeHelpers
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api.{ArticleRevisionHistoryDTO, NotFoundException}
import no.ndla.draftapi.model.domain.ImportId
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service.search.{
  ArticleSearchService,
  GrepCodesSearchService,
  SearchConverterService,
  TagSearchService
}
import no.ndla.validation.*
import org.jsoup.nodes.Element
import scalikejdbc.ReadOnlyAutoSession

import scala.jdk.CollectionConverters.*
import scala.math.max
import scala.util.{Failure, Success, Try}

trait ReadService {
  this: DraftRepository & ConverterService & ArticleSearchService & TagSearchService & GrepCodesSearchService &
    SearchConverterService & UserDataRepository & WriteService & Props & MemoizeHelpers & DBUtility =>
  val readService: ReadService

  import props.*

  class ReadService {

    def getInternalArticleIdByExternalId(externalId: Long): Option[api.ContentIdDTO] =
      draftRepository.getIdFromExternalId(externalId.toString)(ReadOnlyAutoSession).map(id => api.ContentIdDTO(id))

    def withId(id: Long, language: String, fallback: Boolean = false): Try[api.ArticleDTO] = {
      draftRepository.withId(id)(ReadOnlyAutoSession).map(addUrlsOnEmbedResources) match {
        case None          => Failure(NotFoundException(s"The article with id $id was not found"))
        case Some(article) => converterService.toApiArticle(article, language, fallback)
      }
    }

    def getArticleBySlug(slug: String, language: String, fallback: Boolean = false): Try[api.ArticleDTO] = {
      draftRepository.withSlug(slug)(ReadOnlyAutoSession).map(addUrlsOnEmbedResources) match {
        case None          => Failure(NotFoundException(s"The article with slug '$slug' was not found"))
        case Some(article) => converterService.toApiArticle(article, language, fallback)
      }
    }

    def getArticles(id: Long, language: String, fallback: Boolean): Seq[api.ArticleDTO] = {
      draftRepository
        .articlesWithId(id)
        .map(addUrlsOnEmbedResources)
        .map(article => converterService.toApiArticle(article, language, fallback))
        .collect { case Success(article) => article }
        .sortBy(_.revision)
        .reverse
    }

    private[service] def addUrlsOnEmbedResources(article: Draft): Draft = {
      val articleWithUrls = article.content.map(content => content.copy(content = addUrlOnResource(content.content)))
      val visualElementWithUrls =
        article.visualElement.map(visual => visual.copy(resource = addUrlOnResource(visual.resource)))

      article.copy(content = articleWithUrls, visualElement = visualElementWithUrls)
    }

    def getArticlesByPage(pageNo: Int, pageSize: Int, lang: String, fallback: Boolean = false): api.ArticleDumpDTO = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
      DBUtil.withSession { implicit session =>
        val results = draftRepository
          .getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize)
          .flatMap(article => converterService.toApiArticle(article, lang, fallback).toOption)
        api.ArticleDumpDTO(draftRepository.articleCount, pageNo, pageSize, lang, results)
      }
    }

    def getArticleDomainDump(pageNo: Int, pageSize: Int): api.ArticleDomainDumpDTO = {
      DBUtil.withSession(implicit session => {
        val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
        val results = draftRepository.getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize)

        api.ArticleDomainDumpDTO(draftRepository.articleCount, pageNo, pageSize, results)
      })
    }

    def getAllGrepCodes(input: String, pageSize: Int, page: Int): Try[api.GrepCodesSearchResultDTO] = {
      val result = grepCodesSearchService.matchingQuery(input, page, pageSize)
      result.map(converterService.toApiArticleGrepCodes)

    }

    def getAllTags(input: String, pageSize: Int, page: Int, language: String): Try[api.TagsSearchResultDTO] = {
      val result = tagSearchService.matchingQuery(
        query = input,
        searchLanguage = language,
        page = page,
        pageSize = pageSize
      )

      result.map(searchConverterService.tagSearchResultAsApiResult)
    }

    private[service] def addUrlOnResource(content: String): String = {
      val doc = HtmlTagRules.stringToJsoupDocument(content)

      val embedTags = doc.select(EmbedTagName).asScala.toList
      embedTags.foreach(addUrlOnEmbedTag)
      HtmlTagRules.jsoupDocumentToString(doc)
    }

    private def addUrlOnEmbedTag(embedTag: Element): Unit = {
      val typeAndPathOption = embedTag.attr(TagAttribute.DataResource.toString) match {
        case resourceType
            if resourceType == ResourceType.File.toString || resourceType == ResourceType.H5P.toString && embedTag
              .hasAttr(TagAttribute.DataPath.toString) =>
          val path = embedTag.attr(TagAttribute.DataPath.toString)
          Some((resourceType, path))

        case resourceType if embedTag.hasAttr(TagAttribute.DataResource_Id.toString) =>
          val id = embedTag.attr(TagAttribute.DataResource_Id.toString)
          Some((resourceType, id))
        case _ =>
          None
      }

      typeAndPathOption match {
        case Some((resourceType, path)) =>
          val baseUrl   = Url.parse(externalApiUrls(resourceType))
          val pathParts = Path.parse(path).parts

          embedTag.attr(
            s"${TagAttribute.DataUrl}",
            baseUrl.addPathParts(pathParts).toString
          ): Unit
        case _ =>
      }
    }

    def importIdOfArticle(externalId: String): Option[ImportId] = {
      draftRepository.importIdOfArticle(externalId)
    }

    def getUserData(userId: String): Try[api.UserDataDTO] = {
      userDataRepository.withUserId(userId) match {
        case None =>
          writeService.newUserData(userId) match {
            case Success(newUserData) => Success(newUserData)
            case Failure(exception)   => Failure(exception)
          }
        case Some(userData) => Success(converterService.toApiUserData(userData))
      }
    }

    def getArticlesByIds(
        articleIds: List[Long],
        language: String,
        fallback: Boolean,
        page: Long,
        pageSize: Long
    ): Try[Seq[api.ArticleDTO]] = {
      val offset = (page - 1) * pageSize
      for {
        ids <-
          if (articleIds.isEmpty) Failure(ValidationException("ids", "Query parameter 'ids' is missing"))
          else Success(articleIds)
        domainArticles <- draftRepository.withIds(ids, offset, pageSize)
        api            <- domainArticles.traverse(article =>
          converterService.toApiArticle(addUrlsOnEmbedResources(article), language, fallback)
        )
      } yield api
    }

    def getArticleRevisionHistory(
        articleId: Long,
        language: String,
        fallback: Boolean
    ): Try[ArticleRevisionHistoryDTO] = {
      val drafts = draftRepository
        .articlesWithId(articleId)
        .map(addUrlsOnEmbedResources)
        .sortBy(
          _.revision.getOrElse(
            return Failure(api.NotFoundException(s"Revision was missing for draft of article with id $articleId"))
          )
        )
        .reverse

      val canDeleteCurrentRevision = drafts match {
        case current :: previous :: _
            if current.status.current != PUBLISHED && shouldPartialPublish(Some(previous), current).isEmpty =>
          true
        case _ => false
      }

      val articles = drafts
        .map(article => converterService.toApiArticle(article, language, fallback))
        .collect { case Success(article) => article }

      Success(ArticleRevisionHistoryDTO(articles, canDeleteCurrentRevision))
    }
  }
}
