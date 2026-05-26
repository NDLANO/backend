/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import io.circe.Decoder
import no.ndla.common.model.domain.article.Article
import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props
import no.ndla.searchapi.integration.ArticleApiClient
import no.ndla.searchapi.model.domain.DomainDumpResults

import scala.math.ceil
import scala.util.{Failure, Success, Try}

/** In-process implementation of search-api's `ArticleApiClient` (a `SearchApiClient` for `Article`). The only methods
  * search-api actually invokes on this client are the dump readers used for indexing: `getSingle`, `getChunks`. We
  * satisfy those by talking directly to article-api's read service / repository instead of going through HTTP.
  *
  * `articleApiCr` is by-name to avoid init-cycle hazards between the monolith CR and each per-app CR.
  */
class ArticleForSearchApiInProcessClient(articleApiCr: => no.ndla.articleapi.ComponentRegistry)(using
    ndlaClient: NdlaClient,
    props: Props,
) extends ArticleApiClient {

  override val baseUrl: String        = "in-process://article-api"
  override val searchPath: String     = "article-api/v2/articles"
  override val name: String           = "articles"
  override val dumpDomainPath: String = "intern/dump/article"

  override def getSingle(id: Long)(implicit d: Decoder[Article]): Try[Article] = {
    articleApiCr.readService.getSingleDomainArticle(id) match {
      case Failure(ex) =>
        logger.error(s"Could not fetch single $name (id: $id) in-process from article-api")
        Failure(ex)
      case ok => ok
    }
  }

  override def getChunks(implicit d: Decoder[Article]): Iterator[Try[Seq[Article]]] = {
    // Replicates SearchApiClient.getChunks but talks straight to article-api's ReadService rather than HTTP.
    inProcessChunk(0, 0) match {
      case Success(initial) =>
        val dbCount  = initial.totalCount
        val pageSize = props.IndexBulkSize
        val numPages = ceil(dbCount.toDouble / pageSize.toDouble).toInt
        Seq.range(1, numPages + 1).iterator.map(p => inProcessChunk(p, pageSize).map(_.results))
      case Failure(ex) =>
        logger.error(s"Could not fetch initial chunk in-process from article-api")
        Iterator(Failure(ex))
    }
  }

  private def inProcessChunk(page: Int, pageSize: Int): Try[DomainDumpResults[Article]] = {
    articleApiCr.readService.getArticleDomainDump(page, pageSize) match {
      case Success(dump) =>
        logger.info(s"Fetched chunk of ${dump.results.size} $name in-process from article-api")
        Success(DomainDumpResults(dump.totalCount, dump.page, dump.pageSize, dump.results))
      case Failure(ex) =>
        logger.error(s"Could not fetch chunk on page: '$page', with pageSize: '$pageSize' in-process from article-api")
        Failure(ex)
    }
  }
}
