/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V7__ConceptArticleIdsAsListTest extends UnitSuite with TestEnvironment {
  val migration = new V7__ConceptArticleIdsAsList

  test("articleId should be converted to articleIds") {
    val old =
      """{"articleId":5,"tags":[],"title":[{"title":"Title","language":"nb"},{"title":"Tittel","language":"nn"}],"status":{"other":["PUBLISHED"],"current":"DRAFT"},"content":[{"content":"Content","language":"nb"},{"content":"Contentnn","language":"nn"}],"created":"2018-07-02T09:46:35Z","updated":"2020-11-09T09:48:50Z","metaImage":[{"imageId":"5522","altText":"Alttext works as well","language":"nb"}],"updatedBy":["fsexOCfJFGOKuy1C2e71OsvQwq0NWKAK"],"subjectIds":[],"supportedLanguages":null,"visualElement":[{"visualElement":"<embed data-resource=\"image\" data-resource_id=\"5522\" data-alt=\"Alttext works as well\" data-size=\"full\" data-align=\"\" />","language":"nb"}]}"""
    val expected =
      """{"articleIds":[5],"tags":[],"title":[{"title":"Title","language":"nb"},{"title":"Tittel","language":"nn"}],"status":{"other":["PUBLISHED"],"current":"DRAFT"},"content":[{"content":"Content","language":"nb"},{"content":"Contentnn","language":"nn"}],"created":"2018-07-02T09:46:35Z","updated":"2020-11-09T09:48:50Z","metaImage":[{"imageId":"5522","altText":"Alttext works as well","language":"nb"}],"updatedBy":["fsexOCfJFGOKuy1C2e71OsvQwq0NWKAK"],"subjectIds":[],"supportedLanguages":null,"visualElement":[{"visualElement":"<embed data-resource=\"image\" data-resource_id=\"5522\" data-alt=\"Alttext works as well\" data-size=\"full\" data-align=\"\" />","language":"nb"}]}"""

    migration.convertToNewConcept(old) should be(expected)
  }

  test("articleId without value should be converted to articleIds with empty list") {
    val old =
      """{"tags":[],"title":[{"title":"Title","language":"nb"},{"title":"Tittel","language":"nn"}],"status":{"other":["PUBLISHED"],"current":"DRAFT"},"content":[{"content":"Content","language":"nb"},{"content":"Contentnn","language":"nn"}],"created":"2018-07-02T09:46:35Z","updated":"2020-11-09T09:48:50Z","metaImage":[{"imageId":"6622","altText":"Hva slags alttext","language":"nb"}],"visualElement":[{"visualElement":"<embed data-resource=\"image\" data-resource_id=\"5522\" data-alt=\"Alttext works as well\" data-size=\"full\" data-align=\"\" />","language":"nb"}],"updatedBy":["fsexOCfJFGOKuy1C2e71OsvQwq0NWKAK"],"subjectIds":[],"supportedLanguages":null}"""
    val expected =
      """{"articleIds":[],"tags":[],"title":[{"title":"Title","language":"nb"},{"title":"Tittel","language":"nn"}],"status":{"other":["PUBLISHED"],"current":"DRAFT"},"content":[{"content":"Content","language":"nb"},{"content":"Contentnn","language":"nn"}],"created":"2018-07-02T09:46:35Z","updated":"2020-11-09T09:48:50Z","metaImage":[{"imageId":"6622","altText":"Hva slags alttext","language":"nb"}],"visualElement":[{"visualElement":"<embed data-resource=\"image\" data-resource_id=\"5522\" data-alt=\"Alttext works as well\" data-size=\"full\" data-align=\"\" />","language":"nb"}],"updatedBy":["fsexOCfJFGOKuy1C2e71OsvQwq0NWKAK"],"subjectIds":[],"supportedLanguages":null}"""

    migration.convertToNewConcept(old) should be(expected)
  }
}
