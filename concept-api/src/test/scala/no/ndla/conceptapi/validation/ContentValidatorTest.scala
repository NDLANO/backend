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
}
