/*
 * Part of NDLA concept-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V9__LanguageUnknownToUndTest extends UnitSuite with TestEnvironment {
  val migration = new V9__LanguageUnknownToUnd

  test("language unknown should be converted to und") {
    val old =
      """{"articleId":5,"tags":[{"tags":["tag"],"language":"unknown"}],"title":[{"title":"Title","language":"unknown"}],"status":{"other":["PUBLISHED"],"current":"DRAFT"},"content":[{"content":"Content","language":"unknown"}],"created":"2018-07-02T09:46:35Z","updated":"2020-11-09T09:48:50Z","metaImage":[{"imageId":"5522","altText":"Alttext works as well","language":"unknown"}],"updatedBy":["fsexOCfJFGOKuy1C2e71OsvQwq0NWKAK"],"subjectIds":[],"supportedLanguages":null,"visualElement":[{"visualElement":"<embed data-resource=\"image\" data-resource_id=\"5522\" data-alt=\"Alttext works as well\" data-size=\"full\" data-align=\"\" />","language":"unknown"}]}"""
    val expected =
      """{"articleId":5,"tags":[{"tags":["tag"],"language":"und"}],"title":[{"title":"Title","language":"und"}],"status":{"other":["PUBLISHED"],"current":"DRAFT"},"content":[{"content":"Content","language":"und"}],"created":"2018-07-02T09:46:35Z","updated":"2020-11-09T09:48:50Z","metaImage":[{"imageId":"5522","altText":"Alttext works as well","language":"und"}],"updatedBy":["fsexOCfJFGOKuy1C2e71OsvQwq0NWKAK"],"subjectIds":[],"supportedLanguages":null,"visualElement":[{"visualElement":"<embed data-resource=\"image\" data-resource_id=\"5522\" data-alt=\"Alttext works as well\" data-size=\"full\" data-align=\"\" />","language":"und"}]}"""

    migration.convertToNewConcept(old) should be(expected)
  }

}
