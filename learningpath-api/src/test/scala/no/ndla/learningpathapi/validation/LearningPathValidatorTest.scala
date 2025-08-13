/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.common.errors.ValidationMessage
import no.ndla.common.model.domain.{Author, ContributorType, Tag, Title}
import no.ndla.common.model.domain.learningpath.{
  Description,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningpathCopyright
}
import no.ndla.learningpathapi.*
import no.ndla.mapping.License.PublicDomain
import org.mockito.Mockito.when
import no.ndla.common.model.domain.Priority

class LearningPathValidatorTest extends UnitSuite with TestEnvironment {

  var validator: LearningPathValidator = _

  override lazy val clock = new SystemClock

  override def beforeEach(): Unit = {
    validator = new LearningPathValidator
    resetMocks()

  }

  val trump: Author                    = Author(ContributorType.Writer, "Donald Drumpf")
  val license: String                  = PublicDomain.toString
  val copyright: LearningpathCopyright = LearningpathCopyright(license, List(trump))

  val ValidLearningPath: LearningPath = LearningPath(
    id = None,
    title = List(Title("Gyldig tittel", "nb")),
    description = List(Description("Gyldig beskrivelse", "nb")),
    coverPhotoId = Some(s"http://api.ndla.no/image-api/v2/images/1"),
    duration = Some(180),
    tags = List(Tag(Seq("Gyldig tag"), "nb")),
    revision = None,
    externalId = None,
    isBasedOn = None,
    status = LearningPathStatus.PRIVATE,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = clock.now(),
    lastUpdated = clock.now(),
    owner = "",
    copyright = copyright,
    isMyNDLAOwner = false,
    responsible = None,
    comments = Seq.empty,
    priority = Priority.Unspecified
  )

  private def validMock() = {
    when(languageValidator.validate("description.language", "nb", false))
      .thenReturn(None)
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "nb", false))
      .thenReturn(None)
  }

  test("That valid learningpath returns no errors") {
    validMock()
    validator.validateLearningPath(ValidLearningPath, allowUnknownLanguage = false) should equal(List())
  }

  test("That validate returns no error for no coverPhoto") {
    validMock()
    validator.validateLearningPath(ValidLearningPath.copy(coverPhotoId = None), allowUnknownLanguage = false) should be(
      List()
    )
  }

  test("That validateCoverPhoto returns an error when metaUrl is pointing to some another api on ndla") {
    validMock()
    val validationError =
      validator.validateCoverPhoto(s"http://api.ndla.no/h5p/1")
    validationError.size should be(1)
    validationError.head.field should equal("coverPhotoMetaUrl")
    validationError.head.message should equal("The url to the coverPhoto must point to an image in NDLA Image API.")
  }

  test("That validateCoverPhoto returns an error when metaUrl is pointing to empty string") {
    validMock()
    val validationError = validator.validateCoverPhoto("")
    validationError.size should be(1)
    validationError.head.field should equal("coverPhotoMetaUrl")
    validationError.head.message should equal("The url to the coverPhoto must point to an image in NDLA Image API.")
  }

  test("That validateCoverPhoto returns an error when metaUrl is pointing to another Domain") {
    validMock()
    val validationError =
      validator.validateCoverPhoto("http://api.vg.no/images/1")
    validationError.size should be(1)
    validationError.head.field should equal("coverPhotoMetaUrl")
    validationError.head.message should equal("The url to the coverPhoto must point to an image in NDLA Image API.")
  }

  test(
    "That validate does not return error message when no descriptions are defined and no descriptions are required"
  ) {
    validMock()
    new LearningPathValidator(descriptionRequired = false)
      .validateLearningPath(ValidLearningPath.copy(description = List()), allowUnknownLanguage = false) should equal(
      List()
    )
  }

  test("That validate returns error message when description contains html") {
    validMock()
    val validationErrors =
      validator.validateLearningPath(
        ValidLearningPath.copy(description = List(Description("<h1>Ugyldig</h1>", "nb"))),
        false
      )
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description.description")
  }
  test("That validate returns error when description has an illegal language") {
    when(languageValidator.validate("description.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("description.language", "Error")))
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "nb", false))
      .thenReturn(None)

    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(description = List(Description("Gyldig beskrivelse", "bergensk"))),
      false
    )
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description.language")
  }

  test("That validate returns error message when description contains html even if description is not required") {
    validMock()
    val validationErrors = new LearningPathValidator(descriptionRequired = false)
      .validateLearningPath(ValidLearningPath.copy(description = List(Description("<h1>Ugyldig</h1>", "nb"))), false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description.description")
  }

  test("That validate returns error when description has an illegal language even if description is not required") {
    when(languageValidator.validate("description.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("description.language", "Error")))
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "nb", false))
      .thenReturn(None)

    val validationErrors = new LearningPathValidator(descriptionRequired = false).validateLearningPath(
      ValidLearningPath.copy(description = List(Description("Gyldig beskrivelse", "bergensk"))),
      false
    )
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description.language")
  }

  test("That DescriptionValidator validates both description text and language") {
    when(languageValidator.validate("description.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("description.language", "Error")))
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "nb", false))
      .thenReturn(None)

    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(description = List(Description("<h1>Ugyldig</h1>", "bergensk"))),
      false
    )
    validationErrors.size should be(2)
    validationErrors.head.field should equal("description.description")
    validationErrors.last.field should equal("description.language")
  }

  test("That validate returns error for all invalid descriptions") {
    validMock()
    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(
        description = List(
          Description("Gyldig", "nb"),
          Description("<h1>Ugyldig</h1>", "nb"),
          Description("<h2>Også ugyldig</h2>", "nb")
        )
      ),
      false
    )

    validationErrors.size should be(2)
    validationErrors.head.field should equal("description.description")
    validationErrors.last.field should equal("description.description")
  }

  test("That validate returns error when duration less than 1") {
    validMock()
    val validationError =
      validator.validateLearningPath(ValidLearningPath.copy(duration = Some(0)), false)
    validationError.size should be(1)
    validationError.head.field should equal("duration")
  }

  test("That validate accepts a learningpath without duration") {
    validMock()
    validator.validateLearningPath(ValidLearningPath.copy(duration = None), false) should equal(List())
  }

  test("That validate returns error when tag contains html") {
    validMock()
    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(tags = List(Tag(Seq("<strong>ugyldig</strong>"), "nb"))),
      false
    )
    validationErrors.size should be(1)
    validationErrors.head.field should equal("tags.tags")
  }

  test("That validate returns error when tag language is invalid") {
    when(languageValidator.validate("description.language", "nb", false))
      .thenReturn(None)
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("tags.language", "Error")))

    val validationErrors =
      validator.validateLearningPath(
        ValidLearningPath.copy(tags = List(Tag(Seq("Gyldig"), "bergensk"))),
        false
      )
    validationErrors.size should be(1)
    validationErrors.head.field should equal("tags.language")
  }

  test("That returns error for both tag text and tag language") {
    when(languageValidator.validate("description.language", "nb", false))
      .thenReturn(None)
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("tags.language", "Error")))

    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(tags = List(Tag(Seq("<strong>ugyldig</strong>"), "bergensk"))),
      false
    )
    validationErrors.size should be(2)
    validationErrors.head.field should equal("tags.tags")
    validationErrors.last.field should equal("tags.language")
  }

  test("That validate returns error for all invalid tags") {
    validMock()
    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(
        tags = List(
          Tag(Seq("<strong>ugyldig</strong>", "<li>også ugyldig</li>"), "nb")
        )
      ),
      false
    )
    validationErrors.size should be(2)
    validationErrors.head.field should equal("tags.tags")
    validationErrors.last.field should equal("tags.tags")
  }

  test("That validate returns error when copyright.license is invalid") {
    validMock()
    val invalidLicense   = "dummy license"
    val invalidCopyright =
      ValidLearningPath.copyright.copy(license = invalidLicense)
    val validationErrors = validator.validateLearningPath(ValidLearningPath.copy(copyright = invalidCopyright), false)

    validationErrors.size should be(1)
  }

  test("That validate returns no errors when license is valid") {
    validMock()
    validator.validateLearningPath(ValidLearningPath, false).isEmpty should be(true)
  }

  test("That validate returns error when copyright.contributors contains html") {
    validMock()
    val invalidCopyright =
      ValidLearningPath.copyright.copy(contributors = List(Author(ContributorType.Writer, "<h1>Gandalf</h1>")))
    val validationErrors = validator.validateLearningPath(ValidLearningPath.copy(copyright = invalidCopyright), false)
    validationErrors.size should be(1)
  }

  test("That validate returns no errors when copyright.contributors contains no html") {
    validMock()
    validator.validateLearningPath(ValidLearningPath, false).isEmpty should be(true)
  }
}
