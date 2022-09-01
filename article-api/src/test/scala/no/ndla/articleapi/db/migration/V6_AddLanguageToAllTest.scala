/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.articleapi.db.migration.{
  V6_Article,
  V6_ArticleContent,
  V6_ArticleIntroduction,
  V6_ArticleMetaDescription,
  V6_ArticleTag,
  V6_ArticleTitle,
  V6_Copyright,
  V6_VisualElement,
  V6__AddLanguageToAll
}

import java.time.LocalDateTime
import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V6_AddLanguageToAllTest extends UnitSuite with TestEnvironment {
  val migration = new V6__AddLanguageToAll

  test("migration should replace empty language fields with unknown") {
    val before = V6_Article(
      Some(1),
      Some(1),
      Seq(V6_ArticleTitle("A title", None)),
      Seq(V6_ArticleContent("Some content", None, Some(""))),
      V6_Copyright("", "", Seq()),
      Seq(V6_ArticleTag(Seq("abc"), Some("nb"))),
      Seq(),
      Seq(V6_VisualElement("abc", Some("en"))),
      Seq(V6_ArticleIntroduction("some", None)),
      Seq(V6_ArticleMetaDescription("some", Some(""))),
      None,
      LocalDateTime.now(),
      LocalDateTime.now(),
      "",
      ""
    )

    val after = migration.convertArticleUpdate(before)

    after.title.forall(_.language.contains("und")) should be(true)
    after.content.forall(_.language.contains("und")) should be(true)
    after.tags.forall(_.language.contains("nb")) should be(true)
    after.visualElement.forall(_.language.contains("en")) should be(true)
    after.introduction.forall(_.language.contains("und")) should be(true)
    after.metaDescription.forall(_.language.contains("und")) should be(true)
  }

}
