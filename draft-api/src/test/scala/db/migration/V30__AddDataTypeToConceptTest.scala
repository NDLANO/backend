/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V30__AddDataTypeToConceptTest extends UnitSuite with TestEnvironment {
  val migration = new V30__AddDataTypeToConcept

  test("migration should a data-type to concept") {
    val learningResourceBefore =
      """{"tags":[{"tags":["test"],"language":"nb"}],"notes":[{"note":"Opprettet artikkel.","user":"ZQ3t4C9SiQkUkzXxxlk0IQyM","status":{"other":[],"current":"DRAFT"},"timestamp":"2020-06-03T06:43:13Z"}],"title":[{"title":"concept test","language":"nb"}],"status":{"other":[],"current":"DRAFT"},"content":[{"content":"<section><p>image:</p><embed data-resource=\"image\" data-resource_id=\"12669\" data-size=\"fullbredde\" data-align=\"\" data-alt=\"Samiske språkområder i Norge. Illustrasjon. \" data-caption=\"\"><p>concept</p><br><br><embed data-resource=\"concept\" data-link-text=\"Forklaring\" data-content-id=\"123\"><p>External</p><p>Rexternal huehue</p></section>","language":"nb"}],"created":"2020-06-03T06:43:13Z","updated":"2020-06-08T10:54:51Z","copyright":{"origin":"","license":"CC-BY-SA-4.0","creators":[],"processors":[],"rightsholders":[]},"grepCodes":["KM2257","KE108"],"metaImage":[{"altText":"h5p external","imageId":"28976","language":"nb"}],"published":"2020-06-03T06:43:13Z","updatedBy":"ZQ3t4C9SiQkUkzXxxlk0IQyM","articleType":"standard","editorLabels":[],"introduction":[{"language":"nb","introduction":"external h5p"}],"visualElement":[],"metaDescription":[{"content":"external h5p","language":"nb"}],"requiredLibraries":[],"previousVersionsNotes":[]}"""
    val learningResourceAfter =
      """{"tags":[{"tags":["test"],"language":"nb"}],"notes":[{"note":"Opprettet artikkel.","user":"ZQ3t4C9SiQkUkzXxxlk0IQyM","status":{"other":[],"current":"DRAFT"},"timestamp":"2020-06-03T06:43:13Z"}],"title":[{"title":"concept test","language":"nb"}],"status":{"other":[],"current":"DRAFT"},"content":[{"content":"<section><p>image:</p><embed data-resource=\"image\" data-resource_id=\"12669\" data-size=\"fullbredde\" data-align=\"\" data-alt=\"Samiske språkområder i Norge. Illustrasjon. \" data-caption=\"\"><p>concept</p><br><br><embed data-resource=\"concept\" data-link-text=\"Forklaring\" data-content-id=\"123\" data-type=\"inline\"><p>External</p><p>Rexternal huehue</p></section>","language":"nb"}],"created":"2020-06-03T06:43:13Z","updated":"2020-06-08T10:54:51Z","copyright":{"origin":"","license":"CC-BY-SA-4.0","creators":[],"processors":[],"rightsholders":[]},"grepCodes":["KM2257","KE108"],"metaImage":[{"altText":"h5p external","imageId":"28976","language":"nb"}],"published":"2020-06-03T06:43:13Z","updatedBy":"ZQ3t4C9SiQkUkzXxxlk0IQyM","articleType":"standard","editorLabels":[],"introduction":[{"language":"nb","introduction":"external h5p"}],"visualElement":[],"metaDescription":[{"content":"external h5p","language":"nb"}],"requiredLibraries":[],"previousVersionsNotes":[]}"""

    migration.convertArticleUpdate(learningResourceBefore) should equal(learningResourceAfter)
  }
}
