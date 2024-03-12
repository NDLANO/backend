/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.common.errors.ValidationMessage
import no.ndla.common.model.domain.Title
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito.when

class TitleValidatorTest extends UnitSuite with TestEnvironment {

  var validator: TitleValidator = _

  override def beforeEach(): Unit = {
    validator = new TitleValidator
    resetMocks()
  }

  val DefaultTitle: Title = Title("Some title", "nb")

  test("That TitleValidator.validate returns error message when no titles are defined") {
    val errorMessages = validator.validate(List(), false)
    errorMessages.size should be(1)
    errorMessages.head.field should equal("title")
    errorMessages.head.message should equal("At least one title is required.")
  }

  test("That TitleValidator validates title text") {
    when(languageValidator.validate("title.language", "nb", false))
      .thenReturn(None)
    val validationErrors = validator.validate(List(DefaultTitle.copy(title = "<h1>Illegal text</h1>")), false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("title.title")
  }

  test("That TitleValidator validates language") {
    when(languageValidator.validate("title.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("title.language", "Error")))
    val validationErrors =
      validator.validate(List(DefaultTitle.copy(language = "bergensk")), false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("title.language")
  }

  test("That TitleValidator validates both title text and language") {
    when(languageValidator.validate("title.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("title.language", "Error")))
    val validationErrors =
      validator.validate(List(DefaultTitle.copy(title = "<h1>Illegal text</h1>", language = "bergensk")), false)
    validationErrors.size should be(2)
    validationErrors.head.field should equal("title.title")
    validationErrors.last.field should equal("title.language")

  }

  test("That TitleValidator returns no errors for a valid title") {
    when(languageValidator.validate("title.language", "nb", false))
      .thenReturn(None)
    validator.validate(List(DefaultTitle), false) should equal(List())
  }

  test("That TitleValidator validates all titles") {
    when(languageValidator.validate("title.language", "nb", false))
      .thenReturn(None)
    val validationErrors =
      validator.validate(
        List(
          DefaultTitle.copy(title = "<h1>Invalid text</h1>"),
          DefaultTitle.copy(title = "<h1>Invalid text</h1>")
        ),
        false
      )
    validationErrors.size should be(2)
    validationErrors.head.field should equal("title.title")
    validationErrors.last.field should equal("title.title")
  }

  test("That TitleValidator does not return error message when no titles are defined and no titles are required") {
    when(languageValidator.validate("title.language", "nb", false))
      .thenReturn(None)
    new TitleValidator(titleRequired = false)
      .validate(List(), false) should equal(List())
  }

  test("That TitleValidator returns error message for an invalid title even if no titles are required") {
    when(languageValidator.validate("title.language", "nb", false))
      .thenReturn(None)
    val validationErrors = new TitleValidator(titleRequired = false)
      .validate(List(DefaultTitle.copy(title = "<h1>Invalid text</h1>")), false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("title.title")
  }
}
