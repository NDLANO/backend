/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.articleapi.model.domain.ArticleRow
import no.ndla.common.configuration.HasBaseProps
import no.ndla.common.model.domain.article.Article
import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {
  this: HasBaseProps =>
  setPropEnv("NDLA_ENVIRONMENT", "local")

  setPropEnv("SEARCH_SERVER", "some-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setPropEnv("AUDIO_API_HOST", "localhost:30014")
  setPropEnv("IMAGE_API_HOST", "localhost:30001")
  setPropEnv("DRAFT_API_HOST", "localhost:30022")

  setPropEnv("BRIGHTCOVE_ACCOUNT_ID", "some-account-id")
  setPropEnv("BRIGHTCOVE_PLAYER_ID", "some-player-id")
  setPropEnv("BRIGHTCOVE_API_CLIENT_ID", "some-client-id")
  setPropEnv("BRIGHTCOVE_API_CLIENT_SECRET", "some-secret")
  setPropEnv("SEARCH_INDEX_NAME", "article-integration-test-index")

  def toArticleRow(article: Article): ArticleRow = {
    ArticleRow(
      rowId = article.id.get,
      revision = article.revision.get,
      articleId = article.id.get,
      slug = article.slug,
      article = Some(article)
    )
  }

}
