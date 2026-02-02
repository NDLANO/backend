/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import io.lemonlabs.uri.{Path, Url}
import no.ndla.articleapi.Props
import no.ndla.articleapi.integration.FrontpageApiClient
import no.ndla.articleapi.model.{NotFoundException, api}
import no.ndla.articleapi.controller.ArticleErrorHelpers
import no.ndla.articleapi.model.api.ArticleSummaryV2DTO
import no.ndla.articleapi.model.domain.*
import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.ArticleSearchService
import no.ndla.common.TryUtil.when
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.{AccessDeniedException, MissingIdException, ValidationException}
import no.ndla.common.implicits.*
import no.ndla.common.model.TagAttribute
import no.ndla.common.model.api.MenuDTO
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.{ArticleType, Availability}
import no.ndla.database.DBUtility
import no.ndla.language.Language
import no.ndla.network.model.{FeideUserWrapper, userOrAccessDenied}
import no.ndla.validation.HtmlTagRules.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.validation.ResourceType
import org.jsoup.nodes.Element
import scalikejdbc.DBSession

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.math.max
import scala.util.{Failure, Success, Try}

class ReadService(using
    articleRepository: ArticleRepository,
    converterService: ConverterService,
    articleSearchService: ArticleSearchService,
    dBUtility: DBUtility,
    props: Props,
    frontpageApiClient: FrontpageApiClient,
) extends StrictLogging {
  def getInternalIdByExternalId(externalId: String): Try[api.ArticleIdV2DTO] = dBUtility.readOnly { implicit session =>
    articleRepository.getIdFromExternalId(externalId).map(api.ArticleIdV2DTO.apply)
  }

  def withIdV2(
      id: Long,
      language: String,
      fallback: Boolean,
      revision: Option[Int],
      feide: Option[FeideUserWrapper],
  ): Try[Cachable[api.ArticleV2DTO]] = dBUtility.readOnly { implicit session =>
    val articleWithExternalIds = for {
      article <- revision match {
        case Some(rev) => articleRepository.withIdAndRevision(id, rev)
        case None      => articleRepository.withId(id)
      }
      externalIds <- articleRepository.getExternalIdsFromId(id)
    } yield (article, externalIds)

    articleWithExternalIds.flatMap { (article, externalIds) =>
      article.mapArticle(addUrlsOnEmbedResources) match {
        case None                                                                                         => Failure(NotFoundException(s"The article with id $id was not found"))
        case Some(ArticleRow(_, _, _, _, None))                                                           => Failure(ArticleErrorHelpers.ArticleGoneException())
        case Some(ArticleRow(_, _, _, _, Some(article))) if article.availability == Availability.everyone =>
          Cachable.yes(converterService.toApiArticleV2(article, language, externalIds, fallback))
        case Some(ArticleRow(_, _, _, _, Some(article))) =>
          val feideUser     = feide.flatMap(_.user)
          val userIsTeacher = feideUser.exists(_.isTeacher)
          article.availability match {
            case Availability.teacher if !userIsTeacher =>
              Failure(
                AccessDeniedException(
                  "User is missing required role(s) to perform this operation",
                  unauthorized = feideUser.isEmpty,
                )
              )
            case _ => Cachable.no(converterService.toApiArticleV2(article, language, externalIds, fallback))
          }
      }
    }
  }

  private def getDomainArticleBySlug(slug: String)(using DBSession): Try[Article] = articleRepository
    .withSlug(slug)
    .flatMap { article =>
      article.mapArticle(addUrlsOnEmbedResources) match {
        case None                                        => Failure(NotFoundException(s"The article with slug '$slug' was not found"))
        case Some(ArticleRow(_, _, _, _, None))          => Failure(ArticleErrorHelpers.ArticleGoneException())
        case Some(ArticleRow(_, _, _, _, Some(article))) => Success(article)
      }
    }

  def getArticleBySlug(slug: String, language: String, fallback: Boolean): Try[Cachable[api.ArticleV2DTO]] = dBUtility
    .readOnly { implicit session =>
      for {
        article         <- getDomainArticleBySlug(slug)
        articleId       <- article.id.toTry(MissingIdException("Article ID was missing"))
        externalIds     <- articleRepository.getExternalIdsFromId(articleId)
        cachableArticle <- article.availability match {
          case Availability.everyone =>
            Cachable.yes(converterService.toApiArticleV2(article, language, externalIds, fallback))
          case _ => Cachable.no(converterService.toApiArticleV2(article, language, externalIds, fallback))
        }
      } yield cachableArticle
    }

  private[service] def addUrlsOnEmbedResources(article: Article): Article = {
    val articleWithUrls       = article.content.map(content => content.copy(content = addUrlOnResource(content.content)))
    val visualElementWithUrls = article
      .visualElement
      .map(visual => visual.copy(resource = addUrlOnResource(visual.resource)))

    article.copy(content = articleWithUrls, visualElement = visualElementWithUrls)
  }

  def getAllTags(input: String, pageSize: Int, offset: Int, language: String): Try[api.TagsSearchResultDTO] = dBUtility
    .readOnly { implicit session =>
      articleRepository
        .getTags(input, pageSize, (offset - 1) * pageSize, language)
        .map { (tags, tagsCount) =>
          converterService.toApiArticleTags(tags, tagsCount, pageSize, offset, language)
        }
    }

  def getArticlesByPage(pageNo: Int, pageSize: Int, lang: String, fallback: Boolean): Try[api.ArticleDumpDTO] =
    dBUtility.readOnly { implicit session =>
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))

      for {
        articleCount <- articleRepository.articleCount
        articles     <- articleRepository.getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize)
        apiArticles  <- articles.traverse { article =>
          article
            .id
            .toTry(MissingIdException("Article ID was missing"))
            .flatMap { id =>
              articleRepository
                .getExternalIdsFromId(id)
                .flatMap(externalIds => converterService.toApiArticleV2(article, lang, externalIds, fallback))
            }
        }
      } yield api.ArticleDumpDTO(articleCount, pageNo, pageSize, lang, apiArticles)
    }

  def getArticleDomainDump(pageNo: Int, pageSize: Int): Try[api.ArticleDomainDumpDTO] =
    dBUtility.readOnly { implicit session =>
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
      for {
        articleCount   <- articleRepository.articleCount
        articles       <- articleRepository.getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize)
        articlesWithUrl = articles.map(addUrlsOnEmbedResources)
      } yield api.ArticleDomainDumpDTO(articleCount, pageNo, pageSize, articlesWithUrl)
    }

  private[service] def addUrlOnResource(content: String): String = {
    val doc = stringToJsoupDocument(content)

    val embedTags = doc.select(EmbedTagName).asScala.toList
    embedTags.foreach(addUrlOnEmbedTag)
    jsoupDocumentToString(doc)
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

  def getRevisions(articleId: Long): Try[Seq[Int]] = dBUtility.readOnly { implicit session =>
    articleRepository
      .getRevisions(articleId)
      .flatMap {
        case Nil       => Failure(NotFoundException(s"Could not find any revisions for article with id $articleId"))
        case revisions => Success(revisions)
      }
  }

  def getArticleIdsByExternalId(externalId: String): Try[Option[api.ArticleIdsDTO]] = dBUtility.readOnly {
    implicit session =>
      articleRepository
        .getArticleIdsFromExternalId(externalId)
        .map(maybeIds => maybeIds.map(converterService.toApiArticleIds))
  }

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
      feide: Option[FeideUserWrapper],
  ): Try[Cachable[SearchResult[ArticleSummaryV2DTO]]] = {
    val availabilities = feide.map(_.userOrAccessDenied) match {
      case Some(Success(user)) => user.availabilities
      case None                => Seq.empty
      case Some(Failure(_))    =>
        logger.info("User is not authenticated with Feide, assuming non-user")
        Seq.empty
    }

    val settings = query.emptySomeToNone match {
      case Some(q) => SearchSettings(
          query = Some(q),
          withIdIn = idList,
          language = language,
          license = license,
          page = page,
          pageSize =
            if (idList.isEmpty) pageSize
            else idList.size,
          sort = sort.getOrElse(Sort.ByRelevanceDesc),
          if (articleTypesFilter.isEmpty) ArticleType.all
          else articleTypesFilter,
          fallback = fallback,
          grepCodes = grepCodes,
          shouldScroll = shouldScroll,
          availability = availabilities,
        )

      case None => SearchSettings(
          query = None,
          withIdIn = idList,
          language = language,
          license = license,
          page = page,
          pageSize =
            if (idList.isEmpty) pageSize
            else idList.size,
          sort = sort.getOrElse(Sort.ByIdAsc),
          if (articleTypesFilter.isEmpty) ArticleType.all
          else articleTypesFilter,
          fallback = fallback,
          grepCodes = grepCodes,
          shouldScroll = shouldScroll,
          availability = availabilities,
        )
    }

    val result       = articleSearchService.matchingQuery(settings)
    val isRestricted = !settings.availability.distinct.forall(_ == Availability.everyone)
    if (isRestricted) Cachable.no(result)
    else Cachable.yes(result)
  }

  private def getAvailabilityFilter(feide: Option[FeideUserWrapper]): Option[Availability] =
    feide.userOrAccessDenied match {
      case Success(user) if user.isTeacher => None
      case _                               => Some(Availability.everyone)
    }

  private def applyAvailabilityFilter(feide: Option[FeideUserWrapper], articles: Seq[Article]): Seq[Article] = {
    val availabilityFilter = getAvailabilityFilter(feide)
    val filteredArticles   = availabilityFilter
      .map(avaFilter => articles.filter(article => article.availability == avaFilter))
      .getOrElse(articles)
    filteredArticles
  }

  def getArticlesByIds(
      articleIds: List[Long],
      language: String,
      fallback: Boolean,
      page: Int,
      pageSize: Int,
      feide: Option[FeideUserWrapper],
  ): Try[Seq[api.ArticleV2DTO]] = dBUtility.readOnly { implicit session =>
    val offset = (page - 1) * pageSize

    for {
      _            <- Failure.when(articleIds.isEmpty)(ValidationException("ids", "Query parameter 'ids' is missing"))
      articleRows  <- articleRepository.withIds(articleIds, offset, pageSize)
      articles      = articleRows.toArticles.map(addUrlsOnEmbedResources)
      isFeideNeeded = articles.exists(article => article.availability == Availability.teacher)
      filtered      =
        if (isFeideNeeded) applyAvailabilityFilter(feide, articles)
        else articles
      apiArticles <- filtered.traverse { article =>
        val triedExternalIds = article
          .id
          .toTry(MissingIdException("Article ID was missing"))
          .flatMap(id => articleRepository.getExternalIdsFromId(id))

        triedExternalIds.flatMap { externalIds =>
          converterService.toApiArticleV2(article, language, externalIds, fallback)
        }
      }
    } yield apiArticles
  }

  @tailrec
  private def findArticleMenu(article: api.ArticleV2DTO, menus: List[MenuDTO]): Try[MenuDTO] = {
    if (menus.isEmpty) Failure(NotFoundException(s"Could not find menu for article with id ${article.id}"))
    else menus.find(_.articleId == article.id) match {
      case Some(value) => Success(value)
      case None        =>
        val submenus = menus.flatMap(m =>
          m.menu
            .map { case x: MenuDTO =>
              x
            }
        )
        findArticleMenu(article, submenus)
    }
  }

  private def getArticlesForRSSFeed(menu: MenuDTO): Try[Cachable[List[api.ArticleV2DTO]]] = {
    val articleIds = menu
      .menu
      .map { case x: MenuDTO =>
        x.articleId
      }
    val articles =
      articleIds.traverse(id => withIdV2(id, Language.DefaultLanguage, fallback = true, revision = None, feide = None))
    articles.map(Cachable.merge)
  }

  private def toArticleItem(article: api.ArticleV2DTO): String = {
    s"""<item>
         |  <title>${article.title.title}</title>
         |  <description>${article.metaDescription.metaDescription}</description>
         |  <link>${toNdlaFrontendUrl(article.slug, article.id)}</link>
         |  <pubDate>${article.published.asString}</pubDate>
         |  ${article.metaImage.map(i => s"""<image>${i.url}</image>""").getOrElse("")}
         |</item>""".stripMargin.indent(4)
  }

  private def toNdlaFrontendUrl(slug: Option[String], id: Long) = slug
    .map(slug => s"${props.ndlaFrontendUrl}/about/$slug")
    .getOrElse(s"${props.ndlaFrontendUrl}/article/$id")

  private val allBlankLinesRegex                                                                  = """(?m)^\s*$[\r\n]*""".r
  private def toRSSXML(parentArticle: api.ArticleV2DTO, articles: List[api.ArticleV2DTO]): String = {
    val rss = s"""<?xml version="1.0" encoding="utf-8"?>
         |<rss version="2.0">
         |  <channel>
         |    <title>${parentArticle.title.title}</title>
         |    <link>${toNdlaFrontendUrl(parentArticle.slug, parentArticle.id)}</link>
         |    <description>${parentArticle.metaDescription.metaDescription}</description>
         |${articles.map(toArticleItem).mkString}
         |  </channel>
         |</rss>""".stripMargin

    allBlankLinesRegex.replaceAllIn(rss, "")
  }

  def getArticleFrontpageRSS(slug: String): Try[Cachable[String]] = {
    for {
      frontPage   <- frontpageApiClient.getFrontpage
      article     <- getArticleBySlug(slug, Language.DefaultLanguage, fallback = true)
      menu        <- findArticleMenu(article.value, frontPage.menu)
      rssArticles <- getArticlesForRSSFeed(menu)
    } yield rssArticles.map(arts => toRSSXML(article.value, arts))
  }
}
