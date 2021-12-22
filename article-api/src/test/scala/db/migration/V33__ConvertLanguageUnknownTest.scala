/*
 * Part of NDLA article-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.articleapi.{TestEnvironment, UnitSuite}

class V33__ConvertLanguageUnknownTest extends UnitSuite with TestEnvironment {
  test("Article with language unknown be converted correctly") {
    val migration = new V33__ConvertLanguageUnknown

    val beforeArticle =
      """{"tags":[{"tags":["test"],"language":"unknown"}],"notes":[{"note":"Opprettet artikkel.","user":"ZQ3t4C9SiQkUkzXxxlk0IQyM","status":{"other":[],"current":"DRAFT"},"timestamp":"2020-06-03T06:43:13Z"}],"title":[{"title":"article test","language":"unknown"}],"status":{"other":[],"current":"DRAFT"},"content":[{"content":"<section><p>content</p></section>","language":"unknown"}],"created":"2020-06-03T06:43:13Z","updated":"2020-06-08T10:54:51Z","copyright":{"origin":"","license":"CC-BY-SA-4.0","creators":[],"processors":[],"rightsholders":[]},"grepCodes":["KM2257","KE108"],"metaImage":[{"imageId":"28976","altText":"h5p external","language":"unknown"}],"published":"2020-06-03T06:43:13Z","updatedBy":"ZQ3t4C9SiQkUkzXxxlk0IQyM","articleType":"standard","editorLabels":[],"introduction":[{"introduction":"intro","language":"unknown"}],"visualElement":[],"metaDescription":[{"content":"external h5p","language":"unknown"}],"requiredLibraries":[],"previousVersionsNotes":[],"availability":"everyone"}"""
    val expectedArticle =
      """{"tags":[{"tags":["test"],"language":"und"}],"notes":[{"note":"Opprettet artikkel.","user":"ZQ3t4C9SiQkUkzXxxlk0IQyM","status":{"other":[],"current":"DRAFT"},"timestamp":"2020-06-03T06:43:13Z"}],"title":[{"title":"article test","language":"und"}],"status":{"other":[],"current":"DRAFT"},"content":[{"content":"<section><p>content</p></section>","language":"und"}],"created":"2020-06-03T06:43:13Z","updated":"2020-06-08T10:54:51Z","copyright":{"origin":"","license":"CC-BY-SA-4.0","creators":[],"processors":[],"rightsholders":[]},"grepCodes":["KM2257","KE108"],"metaImage":[{"imageId":"28976","altText":"h5p external","language":"und"}],"published":"2020-06-03T06:43:13Z","updatedBy":"ZQ3t4C9SiQkUkzXxxlk0IQyM","articleType":"standard","editorLabels":[],"introduction":[{"introduction":"intro","language":"und"}],"visualElement":[],"metaDescription":[{"content":"external h5p","language":"und"}],"requiredLibraries":[],"previousVersionsNotes":[],"availability":"everyone"}"""

    migration.convertArticleUpdate(beforeArticle) should be(expectedArticle)
  }
}
