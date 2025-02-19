/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.db.migration

import no.ndla.frontpageapi.{TestEnvironment, UnitSuite}

class V10__subjectpage_visual_element_ids_Test extends UnitSuite with TestEnvironment {
  val migration = new V10__subjectpage_visual_element_ids

  test("that visual element id-urls are converted to ids") {
    val before =
      """{"name":"English 1","about":[{"title":"English 1","language":"en","description":"International English has been replaced by English 1. There has been an extensive update and renewal of resources to ensure compliance with the curriculum.","visualElement":{"id":"https://api.staging.ndla.no/image-api/raw/id/56295","alt":"Sketches of famous buildings around the world. Illustration.","type":"image"}},{"title":"Norsk 1","language":"nb","description":"Norskolini","visualElement":{"id":"https://api.staging.ndla.no/image-api/raw/id/56296","alt":"Altorama","type":"image"}}],"leadsTo":[],"buildsOn":[],"bannerImage":{"mobileImageId":51968,"desktopImageId":51968},"connectedTo":[],"editorsChoices":[],"metaDescription":[{"language":"en","metaDescription":"International English has been replaced by English 1. There has been an extensive update and renewal of resources to ensure compliance with the curriculum."}]}"""

    val after =
      """{"name":"English 1","about":[{"title":"English 1","language":"en","description":"International English has been replaced by English 1. There has been an extensive update and renewal of resources to ensure compliance with the curriculum.","visualElement":{"id":"56295","alt":"Sketches of famous buildings around the world. Illustration.","type":"image"}},{"title":"Norsk 1","language":"nb","description":"Norskolini","visualElement":{"id":"56296","alt":"Altorama","type":"image"}}],"leadsTo":[],"buildsOn":[],"bannerImage":{"mobileImageId":51968,"desktopImageId":51968},"connectedTo":[],"editorsChoices":[],"metaDescription":[{"language":"en","metaDescription":"International English has been replaced by English 1. There has been an extensive update and renewal of resources to ensure compliance with the curriculum."}]}"""

    migration.convertSubjectpage(V10__DBSubjectPage(1, before)).document should be(after)
  }

  test("that visual element id are untouched") {
    val before =
      """{"name":"English 1","about":[{"title":"English 1","language":"en","description":"International English has been replaced by English 1. There has been an extensive update and renewal of resources to ensure compliance with the curriculum.","visualElement":{"id":"56295","alt":"Sketches of famous buildings around the world. Illustration.","type":"image"}}],"leadsTo":[],"buildsOn":[],"bannerImage":{"mobileImageId":51968,"desktopImageId":51968},"connectedTo":[],"editorsChoices":[],"metaDescription":[{"language":"en","metaDescription":"International English has been replaced by English 1. There has been an extensive update and renewal of resources to ensure compliance with the curriculum."}]}"""

    val after =
      """{"name":"English 1","about":[{"title":"English 1","language":"en","description":"International English has been replaced by English 1. There has been an extensive update and renewal of resources to ensure compliance with the curriculum.","visualElement":{"id":"56295","alt":"Sketches of famous buildings around the world. Illustration.","type":"image"}}],"leadsTo":[],"buildsOn":[],"bannerImage":{"mobileImageId":51968,"desktopImageId":51968},"connectedTo":[],"editorsChoices":[],"metaDescription":[{"language":"en","metaDescription":"International English has been replaced by English 1. There has been an extensive update and renewal of resources to ensure compliance with the curriculum."}]}"""

    migration.convertSubjectpage(V10__DBSubjectPage(1, before)).document should be(after)
  }
}
