/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.domain

import no.ndla.frontpageapi.{TestData, TestEnvironment, UnitSuite}

import scala.util.Success

class SubjectPageTest extends UnitSuite with TestEnvironment {
  test("decodeJson should use correct id") {
    val Success(subject) = SubjectPage.decodeJson(TestData.domainSubjectJson, 10)
    subject.id should be(Some(10))
  }

}
