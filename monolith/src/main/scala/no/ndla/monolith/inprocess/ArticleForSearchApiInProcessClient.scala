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

import scala.util.Try

/** In-process implementation of search-api's `ArticleApiClient` (a `SearchApiClient` for `Article`). Overrides the
  * read methods search-api invokes during indexing — `getSingle` and the protected `getChunk` — by talking directly to
  * article-api's read service. The trait's default `getChunks` / `getChunkSource` both funnel through `getChunk`, so
  * overriding it at the protected hook keeps both call paths in-process.
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

  override def getSingle(id: Long)(implicit d: Decoder[Article]): Try[Article] =
    articleApiCr.readService.getSingleDomainArticle(id)

  override protected def getChunk(page: Int, pageSize: Int)(implicit
      d: Decoder[Article]
  ): Try[DomainDumpResults[Article]] = articleApiCr
    .readService
    .getArticleDomainDump(page, pageSize)
    .map(dump => DomainDumpResults(dump.totalCount, dump.page, dump.pageSize, dump.results))
}
