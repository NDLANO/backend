/*
 * Part of NDLA concept-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.validation

import no.ndla.common.model.domain.{Author, Responsible, Title}
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.draft.Copyright
import no.ndla.conceptapi.model.domain.Concept
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}

import scala.util.{Failure, Success}

class ContentValidatorTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService
  override val contentValidator = new ContentValidator

  val baseConcept: Concept = TestData.domainConcept.copy(
    responsible = Some(Responsible("hei", TestData.today))
  )

  test("That title validation fails if no titles exist") {

    val conceptToValidate = baseConcept.copy(
      title = Seq()
    )

    val Failure(exception: ValidationException) = contentValidator.validateConcept(conceptToValidate)
    exception.errors should be(
      Seq(ValidationMessage("title", "The field does not have any entries, whereas at least one is required."))
    )
  }

  test("That title validation succeeds if titles exist") {
    val conceptToValidate = baseConcept.copy(
      title = Seq(Title("Amazing title", "nb"))
    )

    val result = contentValidator.validateConcept(conceptToValidate)
    result should be(Success(conceptToValidate))
  }
  test("Copyright validation succeeds if license is omitted and copyright holders are empty") {
    val concept =
      baseConcept.copy(copyright = Some(Copyright(None, None, Seq(), Seq(), Seq(), None, None, None)))
    val result = contentValidator.validateConcept(concept)
    result should be(Success(concept))
  }

  test("Copyright validation fails if license is included and copyright holders are empty") {
    val concept = baseConcept.copy(
      copyright = Some(Copyright(Some("CC-BY-4.0"), None, Seq(), Seq(), Seq(), None, None, None))
    )
    val Failure(exception: ValidationException) = contentValidator.validateConcept(concept)
    exception.errors should be(
      Seq(ValidationMessage("license.license", "At least one copyright holder is required when license is CC-BY-4.0"))
    )
  }

  test("Copyright validation succeeds if license is included and copyright holders are not empty") {

    val concept = baseConcept.copy(
      copyright =
        Some(Copyright(Some("CC-BY-4.0"), None, Seq(Author("creator", "test")), Seq(), Seq(), None, None, None))
    )
    val result = contentValidator.validateConcept(concept)
    result should be(Success(concept))
  }
}
