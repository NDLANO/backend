/*
 * Part of NDLA concept-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.db.migration

import no.ndla.conceptapi.{TestEnvironment, UnitSuite}

class V13__RenameStatusesTest extends UnitSuite with TestEnvironment {
  val migration = new V13__RenameStatuses

  val old =
    """{"articleId":5,"tags":[{"tags":["tag"],"language":"nb"}],"title":[{"title":"Title","language":"nb"}],"status":{"current":"DRAFT","other":["QUALITY_ASSURED","QUEUED_FOR_LANGUAGE","TRANSLATED"]},"content":[{"content":"Content","language":"nb"}],"created":"2018-07-02T09:46:35Z","updated":"2020-11-09T09:48:50Z","metaImage":[{"imageId":"5522","altText":"Alttext works as well","language":"nb"}],"updatedBy":["fsexOCfJFGOKuy1C2e71OsvQwq0NWKAK"],"subjectIds":[],"supportedLanguages":null,"visualElement":[]}"""
  val expected =
    """{"articleId":5,"tags":[{"tags":["tag"],"language":"nb"}],"title":[{"title":"Title","language":"nb"}],"status":{"current":"IN_PROGRESS","other":["END_CONTROL","LANGUAGE","FOR_APPROVAL"]},"content":[{"content":"Content","language":"nb"}],"created":"2018-07-02T09:46:35Z","updated":"2020-11-09T09:48:50Z","metaImage":[{"imageId":"5522","altText":"Alttext works as well","language":"nb"}],"updatedBy":["fsexOCfJFGOKuy1C2e71OsvQwq0NWKAK"],"subjectIds":[],"supportedLanguages":null,"visualElement":[]}"""

  migration.convertToNewConcept(old) should be(expected)

}
