/*
 * Part of NDLA concept-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.validation

import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.validation.{ValidationException, ValidationMessage}
import no.ndla.conceptapi.model.domain
import no.ndla.conceptapi.model.domain.{Author, Copyright}

import scala.util.{Failure, Success}

class ContentValidatorTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService
  override val contentValidator = new ContentValidator

  test("That title validation fails if no titles exist") {

    val conceptToValidate = TestData.domainConcept.copy(
      title = Seq()
    )

    val Failure(exception: ValidationException) = contentValidator.validateConcept(conceptToValidate)
    exception.errors should be(
      Seq(ValidationMessage("title", "The field does not have any entries, whereas at least one is required."))
    )
  }

  test("That title validation succeeds if titles exist") {
    val conceptToValidate = TestData.domainConcept.copy(
      title = Seq(domain.ConceptTitle("Amazing title", "nb"))
    )

    val result = contentValidator.validateConcept(conceptToValidate)
    result should be(Success(conceptToValidate))
  }
  test("Copyright validation succeeds if license is omitted and copyright holders are empty") {
    val concept =
      TestData.domainConcept.copy(copyright = Some(Copyright(None, None, Seq(), Seq(), Seq(), None, None, None)))
    val result = contentValidator.validateConcept(concept)
    result should be(Success(concept))
  }

  test("Copyright validation fails if license is included and copyright holders are empty") {
    val concept = TestData.domainConcept.copy(
      copyright = Some(Copyright(Some("CC-BY-4.0"), None, Seq(), Seq(), Seq(), None, None, None)))
    val Failure(exception: ValidationException) = contentValidator.validateConcept(concept)
    exception.errors should be(
      Seq(ValidationMessage("license.license", "At least one copyright holder is required when license is CC-BY-4.0")))
  }

  test("Copyright validation succeeds if license is included and copyright holders are not empty") {

    val concept = TestData.domainConcept.copy(
      copyright =
        Some(Copyright(Some("CC-BY-4.0"), None, Seq(Author("creator", "test")), Seq(), Seq(), None, None, None)))
    val result = contentValidator.validateConcept(concept)
    result should be(Success(concept))
  }
}
