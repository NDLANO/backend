/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.common.errors.ValidationMessage
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.learningpath.{
  Description,
  EmbedType,
  EmbedUrl,
  Introduction,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningStep,
  LearningpathCopyright,
  StepStatus,
  StepType
}
import no.ndla.common.model.domain.{Author, ContributorType, Priority, RevisionMeta, Tag, Title}
import no.ndla.learningpathapi.*
import no.ndla.mapping.License.PublicDomain
import org.mockito.Mockito.when

class LearningStepValidatorTest extends UnitSuite with TestEnvironment {

  var validator: LearningStepValidator = scala.compiletime.uninitialized

  val license: String = PublicDomain.toString
  val today: NDLADate = clock.now()

  val ValidLearningStep: LearningStep = LearningStep(
    id = None,
    revision = None,
    externalId = None,
    learningPathId = None,
    seqNo = 0,
    title = List(Title("Gyldig tittel", "nb")),
    introduction = List(Introduction("<p>Gyldig introduksjon</p>", "nb")),
    description = List(Description("<strong>Gyldig description</strong>", "nb")),
    embedUrl = List(EmbedUrl("https://www.ndla.no/123", "nb", EmbedType.OEmbed)),
    articleId = None,
    `type` = StepType.TEXT,
    copyright = Some(LearningpathCopyright(license, Seq.empty)),
    showTitle = true,
    status = StepStatus.ACTIVE,
    created = today,
    lastUpdated = today
  )

  val trump: Author                    = Author(ContributorType.Writer, "Donald Drumpf")
  val copyright: LearningpathCopyright = LearningpathCopyright(license, List(trump))

  val ValidLearningPath: LearningPath = LearningPath(
    id = None,
    title = List(Title("Gyldig tittel", "nb")),
    description = List(Description("Gyldig beskrivelse", "nb")),
    introduction = List(Introduction("<section><p>Gyldig introduksjon</p></section>", "nb")),
    coverPhotoId = Some(s"http://api.ndla.no/image-api/v2/images/1"),
    duration = Some(180),
    tags = List(Tag(Seq("Gyldig tag"), "nb")),
    revision = None,
    externalId = None,
    isBasedOn = None,
    status = LearningPathStatus.PRIVATE,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = today,
    lastUpdated = today,
    owner = "",
    copyright = copyright,
    isMyNDLAOwner = false,
    responsible = None,
    comments = Seq.empty,
    priority = Priority.Unspecified,
    revisionMeta = RevisionMeta.default
  )

  override def beforeEach(): Unit = {
    validator = new LearningStepValidator
    resetMocks()
  }

  test("That a valid learningstep does not give an error") {
    validator.validateLearningStep(ValidLearningStep, ValidLearningPath, false) should equal(List())
  }

  test("That validate returns error message when description contains illegal html") {
    val validationErrors =
      validator.validateLearningStep(
        ValidLearningStep.copy(description = List(Description("<h1>Ugyldig</h1>", "nb"))),
        ValidLearningPath,
        false
      )
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description")
  }

  test("That validate returns error when description has an illegal language") {
    when(languageValidator.validate("language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("language", "Error")))
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    val validationErrors = validator.validateLearningStep(
      ValidLearningStep.copy(description = List(Description("<strong>Gyldig beskrivelse</strong>", "bergensk"))),
      ValidLearningPath,
      false
    )
    validationErrors.size should be(1)
    validationErrors.head.field should equal("language")
  }

  test("That DescriptionValidator validates both description text and language") {
    when(languageValidator.validate("language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("language", "Error")))
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    val validationErrors = validator.validateLearningStep(
      ValidLearningStep.copy(description = List(Description("<h1>Ugyldig</h1>", "bergensk"))),
      ValidLearningPath,
      false
    )
    validationErrors.size should be(2)
    validationErrors.head.field should equal("description")
    validationErrors.last.field should equal("language")
  }

  test("That validate returns error for all invalid descriptions") {
    val validationErrors = validator.validateLearningStep(
      ValidLearningStep.copy(
        description = List(
          Description("<strong>Gyldig</strong>", "nb"),
          Description("<h2>Også gyldig</h2>", "nb"),
          Description("<h1>Ugyldig</h1>", "nb"),
          Description("<h4>Også ugyldig</h4>", "nb")
        )
      ),
      ValidLearningPath,
      false
    )

    validationErrors.size should be(2)
    validationErrors.head.field should equal("description")
    validationErrors.last.field should equal("description")
  }

  test("That validate returns error when embedUrl contains html") {
    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(embedUrl = List(EmbedUrl("<strong>ikke gyldig</strong>", "nb", EmbedType.OEmbed))),
      ValidLearningPath,
      false
    )
    validationMessages.size should be(1)
    validationMessages.head.field should equal("embedUrl.url")
    validationMessages.head.message.contains("contains illegal html") should be(true)
  }

  test("That validate returns error when embedUrl.language is invalid") {
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
    when(languageValidator.validate("language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("language", "Error")))
    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(embedUrl = List(EmbedUrl("https://www.ndla.no/123", "bergensk", EmbedType.OEmbed))),
      ValidLearningPath,
      false
    )
    validationMessages.size should be(1)
    validationMessages.head.field should equal("language")
  }

  test("That validate returns error for both embedUrl.url and embedUrl.language") {
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
    when(languageValidator.validate("language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("language", "Error")))

    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(embedUrl = List(EmbedUrl("<h1>Ugyldig</h1>", "bergensk", EmbedType.OEmbed))),
      ValidLearningPath,
      false
    )
    validationMessages.size should be(2)
    validationMessages.head.field should equal("embedUrl.url")
    validationMessages.last.field should equal("language")
  }

  test("That all embedUrls are validated") {
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
    when(languageValidator.validate("language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("language", "Error")))
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)

    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(
        embedUrl = List(
          EmbedUrl("<h1>Ugyldig</h1>", "nb", EmbedType.OEmbed),
          EmbedUrl("https://www.ndla.no/123", "bergensk", EmbedType.OEmbed)
        )
      ),
      ValidLearningPath,
      false
    )
    validationMessages.size should be(2)
    validationMessages.head.field should equal("embedUrl.url")
    validationMessages.last.field should equal("language")
  }

  test("Embedurls containing only paths should be legal") {
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())

    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(
        embedUrl =
          List(EmbedUrl("/subjects/subject:9/topic:1:179373/topic:1:170165/resource:1:16145", "nb", EmbedType.OEmbed))
      ),
      ValidLearningPath,
      false
    )
    validationMessages.size should be(0)
  }

  test("That html-code in license returns an error") {
    val license            = "<strong>ugyldig</strong>"
    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(copyright = Some(LearningpathCopyright(license, Seq.empty))),
      ValidLearningPath,
      false
    )
    validationMessages.size should be(1)
    validationMessages.head.field should equal("license")
  }

  test("That None-license doesn't give an error") {
    validator.validateLearningStep(ValidLearningStep.copy(copyright = None), ValidLearningPath, false) should equal(
      List()
    )
  }

  test("That error is returned when no descriptions, embedUrls or articleId are defined") {
    val validationErrors =
      validator.validateLearningStep(
        ValidLearningStep.copy(description = List(), embedUrl = Seq(), articleId = None),
        ValidLearningPath,
        false
      )
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description|embedUrl|articleId")
    validationErrors.head.message should equal(
      "A learningstep is required to have either a description, an embedUrl, or an articleId."
    )
  }

  test("That no error is returned when a description is present, but no embedUrls") {
    validator.validateLearningStep(ValidLearningStep.copy(embedUrl = Seq()), ValidLearningPath, false) should equal(
      Seq()
    )
  }

  test("That no error is returned when an embedUrl is present, but no descriptions") {
    validator.validateLearningStep(ValidLearningStep.copy(description = List()), ValidLearningPath, false) should equal(
      List()
    )
  }

  test("That error is returned if step in My NDLA path is created/updated with multiple languages") {
    val newStep      = ValidLearningStep.copy(title = ValidLearningStep.title :+ Title("Tittel", "nn"))
    val learningPath = ValidLearningPath.copy(isMyNDLAOwner = true)
    validator.validateLearningStep(newStep, learningPath, false) should be(
      Seq(
        ValidationMessage(
          "supportedLanguages",
          "A learning step created in MyNDLA must have exactly one supported language."
        )
      )
    )
  }
}
