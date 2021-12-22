/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.util.Date

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
      new Date(),
      new Date(),
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
