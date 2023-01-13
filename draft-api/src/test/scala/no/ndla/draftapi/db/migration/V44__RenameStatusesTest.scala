package no.ndla.draftapi.db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V44__RenameStatusesTest extends UnitSuite with TestEnvironment {

  test("That converting content-links works as expected") {
    val testHtml =
      """{"content":[{"content":"<section></section>","language":"nb"}],"status":{"current":"DRAFT","other":["PROPOSAL","USER_TEST","AWAITING_QUALITY_ASSURANCE","QUEUED_FOR_LANGUAGE","TRANSLATED","QUEUED_FOR_PUBLISHING","QUALITY_ASSURED","QUALITY_ASSURED_DELAYED","QUEUED_FOR_PUBLISHING_DELAYED","AWAITING_UNPUBLISHING","AWAITING_ARCHIVING"]}}"""
    val expectedResult =
      """{"content":[{"content":"<section></section>","language":"nb"}],"status":{"current":"PLANNED","other":["IN_PROGRESS","EXTERNAL_REVIEW","QUALITY_ASSURANCE","LANGUAGE","FOR_APPROVAL","END_CONTROL","END_CONTROL","END_CONTROL","END_CONTROL","PUBLISHED","PUBLISHED"]}}"""
    val migration = new V44__RenameStatuses
    val result    = migration.convertArticleUpdate(testHtml)
    result should be(expectedResult)
  }
}
