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
import no.ndla.common.implicits.toTry
import no.ndla.common.model.TagAttribute
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.draft.DraftStatus.PUBLISHED
import no.ndla.database.DBUtility
import no.ndla.draftapi.DraftUtil.shouldPartialPublish
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api.{ArticleRevisionHistoryDTO, NotFoundException}
import no.ndla.draftapi.model.domain.ImportId
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service.search.{GrepCodesSearchService, SearchConverterService, TagSearchService}
import no.ndla.validation.*
import org.jsoup.nodes.Element

import scala.jdk.CollectionConverters.*
import scala.math.max
import scala.util.{Failure, Success, Try, boundary}

class ReadService(using
    draftRepository: DraftRepository,
    converterService: ConverterService,
    tagSearchService: => TagSearchService,
    grepCodesSearchService: => GrepCodesSearchService,
    searchConverterService: => SearchConverterService,
    userDataRepository: UserDataRepository,
    writeService: => WriteService,
    props: Props,
    dbUtility: DBUtility,
) {
  def getInternalArticleIdByExternalId(externalId: Long): Try[api.ContentIdDTO] = dbUtility.readOnly {
    implicit session =>
      draftRepository
        .getIdFromExternalId(externalId.toString)
        .flatMap(_.toTry(NotFoundException(s"No article with id $externalId")))
        .map(api.ContentIdDTO.apply)
  }

  def withId(id: Long, language: String, fallback: Boolean = false): Try[api.ArticleDTO] = dbUtility.readOnly {
    implicit session =>
      draftRepository
        .withId(id)
        .flatMap {
          case None          => Failure(NotFoundException(s"The article with id $id was not found"))
          case Some(article) => converterService.toApiArticle(addUrlsOnEmbedResources(article), language, fallback)
        }
  }

  def getArticleBySlug(slug: String, language: String, fallback: Boolean = false): Try[api.ArticleDTO] = dbUtility
    .readOnly { implicit session =>
      draftRepository
        .withSlug(slug)
        .flatMap {
          case None          => Failure(NotFoundException(s"The article with slug '$slug' was not found"))
          case Some(article) => converterService.toApiArticle(addUrlsOnEmbedResources(article), language, fallback)
        }
    }

  def getArticles(id: Long, language: String, fallback: Boolean): Try[Seq[api.ArticleDTO]] = dbUtility.readOnly {
    implicit session =>
      for {
        articles         <- draftRepository.articlesWithId(id)
        articlesWithUrls  = articles.map(addUrlsOnEmbedResources)
        apiArticles      <- articlesWithUrls.traverse(article => converterService.toApiArticle(article, language, fallback))
        sortedApiArticles = apiArticles.sortBy(_.revision)(using Ordering.Int.reverse)
      } yield sortedApiArticles
  }

  private[service] def addUrlsOnEmbedResources(article: Draft): Draft = {
    val articleWithUrls       = article.content.map(content => content.copy(content = addUrlOnResource(content.content)))
    val visualElementWithUrls = article
      .visualElement
      .map(visual => visual.copy(resource = addUrlOnResource(visual.resource)))

    article.copy(content = articleWithUrls, visualElement = visualElementWithUrls)
  }

  def getArticlesByPage(
      pageNo: Int,
      pageSize: Int,
      lang: String,
      fallback: Boolean = false,
  ): Try[api.ArticleDumpDTO] = {
    val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
    dbUtility.readOnly { implicit session =>
      for {
        articles <- draftRepository.getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize)
        results   = articles.flatMap(article => converterService.toApiArticle(article, lang, fallback).toOption)
        count    <- draftRepository.articleCount
      } yield api.ArticleDumpDTO(count, pageNo, pageSize, lang, results)
    }
  }

  def getArticleDomainDump(pageNo: Int, pageSize: Int): Try[api.ArticleDomainDumpDTO] = {
    dbUtility.readOnly { implicit session =>
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
      for {
        results <- draftRepository.getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize)
        count   <- draftRepository.articleCount
      } yield api.ArticleDomainDumpDTO(count, pageNo, pageSize, results)
    }
  }

  def getAllGrepCodes(input: String, pageSize: Int, page: Int): Try[api.GrepCodesSearchResultDTO] = {
    val result = grepCodesSearchService.matchingQuery(input, page, pageSize)
    result.map(converterService.toApiArticleGrepCodes)

  }

  def getAllTags(input: String, pageSize: Int, page: Int, language: String): Try[api.TagsSearchResultDTO] = {
    val result =
      tagSearchService.matchingQuery(query = input, searchLanguage = language, page = page, pageSize = pageSize)

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
          if resourceType == EmbedType.File.toString || resourceType == EmbedType.H5P.toString && embedTag.hasAttr(
            TagAttribute.DataPath.toString
          ) =>
        val path = embedTag.attr(TagAttribute.DataPath.toString)
        Some((resourceType, path))

      case resourceType if embedTag.hasAttr(TagAttribute.DataResource_Id.toString) =>
        val id = embedTag.attr(TagAttribute.DataResource_Id.toString)
        Some((resourceType, id))
      case _ => None
    }

    typeAndPathOption match {
      case Some((resourceType, path)) =>
        val baseUrl   = Url.parse(props.externalApiUrls(resourceType))
        val pathParts = Path.parse(path).parts

        embedTag.attr(s"${TagAttribute.DataUrl}", baseUrl.addPathParts(pathParts).toString): Unit
      case _ =>
    }
  }

  def importIdOfArticle(externalId: String): Try[ImportId] = dbUtility.readOnly { implicit session =>
    draftRepository
      .importIdOfArticle(externalId)
      .flatMap(_.toTry(NotFoundException(s"Could not find draft with external id: '$externalId'")))
  }

  def getUserData(userId: String): Try[api.UserDataDTO] = dbUtility.rollbackOnFailure { implicit session =>
    userDataRepository
      .withUserId(userId)
      .flatMap {
        case None           => writeService.newUserData(userId)
        case Some(userData) => Success(converterService.toApiUserData(userData))
      }
  }

  def getArticlesByIds(
      articleIds: List[Long],
      language: String,
      fallback: Boolean,
      page: Long,
      pageSize: Long,
  ): Try[Seq[api.ArticleDTO]] = dbUtility.readOnly { implicit session =>
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

  def getArticleRevisionHistory(articleId: Long, language: String, fallback: Boolean): Try[ArticleRevisionHistoryDTO] =
    boundary {
      val drafts = dbUtility.readOnly(implicit session => draftRepository.articlesWithId(articleId)) match {
        case Failure(ex)   => boundary.break(Failure(ex))
        case Success(list) => list
            .map(addUrlsOnEmbedResources)
            .sortBy(
              _.revision
                .getOrElse {
                  boundary.break(
                    Failure(api.NotFoundException(s"Revision was missing for draft of article with id $articleId"))
                  )
                }
            )
            .reverse
      }

      val canDeleteCurrentRevision = drafts match {
        case current :: previous :: _
            if current.status.current != PUBLISHED && shouldPartialPublish(Some(previous), current).isEmpty => true
        case _ => false
      }

      val articles = drafts
        .map(article => converterService.toApiArticle(article, language, fallback))
        .collect { case Success(article) =>
          article
        }

      Success(ArticleRevisionHistoryDTO(articles, canDeleteCurrentRevision))
    }
}
