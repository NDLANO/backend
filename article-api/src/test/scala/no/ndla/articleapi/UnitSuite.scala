/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.articleapi.model.domain.ArticleRow
import no.ndla.common.model.domain.article.Article
import no.ndla.scalatestsuite.UnitTestSuite

import scala.util.Properties.setProp

trait UnitSuite extends UnitTestSuite {

  setProp("NDLA_ENVIRONMENT", "local")

  setProp("SEARCH_SERVER", "some-server")
  setProp("SEARCH_REGION", "some-region")
  setProp("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setProp("AUDIO_API_HOST", "localhost:30014")
  setProp("IMAGE_API_HOST", "localhost:30001")
  setProp("DRAFT_API_HOST", "localhost:30022")

  setProp("NDLA_BRIGHTCOVE_ACCOUNT_ID", "some-account-id")
  setProp("NDLA_BRIGHTCOVE_PLAYER_ID", "some-player-id")
  setProp("BRIGHTCOVE_API_CLIENT_ID", "some-client-id")
  setProp("BRIGHTCOVE_API_CLIENT_SECRET", "some-secret")
  setProp("SEARCH_INDEX_NAME", "article-integration-test-index")

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
