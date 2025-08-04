/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.validation

import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.*
import no.ndla.common.model.domain.draft.{Draft, DraftCopyright, RevisionMeta}
import no.ndla.common.model.domain.language.OptLanguageFields
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.mapping.License
import no.ndla.mapping.License.CC_BY_SA

import java.util.UUID
import scala.util.Failure

class ContentValidatorTest extends UnitSuite with TestEnvironment {
  override val contentValidator                   = new ContentValidator()
  override val converterService: ConverterService = new ConverterService
  val validDocument                               = """<section><h1>heisann</h1><h2>heia</h2></section>"""
  val invalidDocument                             = """<section><invalid></invalid></section>"""
  val validDisclaimer                             =
    """<p><strong>hallo!</strong><ndlaembed data-content-id="123" data-open-in="current-context" data-resource="content-link" data-content-type="article">test</ndlaembed></p>"""

  val articleToValidate: Draft =
    TestData.sampleArticleWithByNcSa.copy(responsible = Some(Responsible("hei", TestData.today)))

  test("validateArticle does not throw an exception on a valid document") {
    val article = articleToValidate.copy(content = Seq(ArticleContent(validDocument, "nb")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws a validation exception on an invalid document") {
    val article = articleToValidate.copy(content = Seq(ArticleContent(invalidDocument, "nb")))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception for MathMl tags") {
    val content = """<section><math xmlns="http://www.w3.org/1998/Math/MathML"></math></section>"""
    val article = articleToValidate.copy(content = Seq(ArticleContent(content, "nb")))

    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle should throw an error if introduction contains HTML tags") {
    val article = articleToValidate.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      introduction = Seq(Introduction("<p>introduction</p>", "nb"))
    )
    contentValidator.validateArticle(article).isFailure should be(false)
  }

  test("validateArticle should not throw an error if introduction contains plain text") {
    val article = articleToValidate.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      introduction = Seq(Introduction("introduction", "nb"))
    )
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle should throw an error if disclaimer contains illegal HTML tags") {
    val article = articleToValidate.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      disclaimer = OptLanguageFields.withValue("<p><hallo>hei</hallo></p>", "nb")
    )
    val Failure(error: ValidationException) = contentValidator.validateArticle(article)
    val expected                            = ValidationException(
      "disclaimer.nb",
      "The content contains illegal tags and/or attributes. Allowed HTML tags are: h3, msgroup, a, article, sub, sup, mtext, msrow, tbody, mtd, pre, thead, figcaption, mover, msup, semantics, ol, span, mroot, munder, h4, mscarries, dt, nav, mtr, ndlaembed, li, br, mrow, merror, mphantom, u, audio, ul, maligngroup, mfenced, annotation, div, strong, section, i, mspace, malignmark, mfrac, code, h2, td, aside, em, mstack, button, dl, th, tfoot, math, tr, b, blockquote, msline, col, annotation-xml, mstyle, caption, mpadded, mo, mlongdiv, msubsup, p, munderover, maction, menclose, h1, details, mmultiscripts, msqrt, mscarry, mstac, mi, mglyph, mlabeledtr, mtable, mprescripts, summary, mn, msub, ms, table, colgroup, dd"
    )
    error should be(expected)
  }

  test("validateArticle should not throw an error if disclaimer contains legal HTML tags") {
    val article = articleToValidate.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      disclaimer = OptLanguageFields.withValue(validDisclaimer, "nb")
    )
    contentValidator.validateArticle(article).isSuccess should be(true)

  }

  test("validateArticle should throw an error if metaDescription contains HTML tags") {
    val article = articleToValidate.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      metaDescription = Seq(Description(validDisclaimer, "nb"))
    )
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle should throw an error if metaDescription contains plain text") {
    val article = articleToValidate.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      metaDescription = Seq(Description(validDocument, "nb"))
    )
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle should not throw an error if metaDescription contains plain text") {
    val article = articleToValidate.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      metaDescription = Seq(Description("meta description", "nb"))
    )
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle should throw an error if title contains HTML tags") {
    val article = articleToValidate.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      title = Seq(Title(validDocument, "nb"))
    )
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle should not throw an error if title contains plain text") {
    val article = articleToValidate.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      title = Seq(Title("title", "nb"))
    )
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle should fail if the title exceeds 256 bytes") {
    val article                          = articleToValidate.copy(title = Seq(Title("A" * 257, "nb")))
    val Failure(ex: ValidationException) = contentValidator.validateArticle(article)

    ex.errors.length should be(1)
    ex.errors.head.message should be("This field exceeds the maximum permitted length of 256 characters")
  }

  test("validateArticle should fail if the title is empty") {
    val article                          = articleToValidate.copy(title = Seq(Title("", "nb")))
    val Failure(ex: ValidationException) = contentValidator.validateArticle(article)

    ex.errors.length should be(1)
    ex.errors.head.message should be("This field does not meet the minimum length requirement of 1 characters")
  }

  test("validateArticle should fail if the title is whitespace") {
    val article                          = articleToValidate.copy(title = Seq(Title("  ", "nb")))
    val Failure(ex: ValidationException) = contentValidator.validateArticle(article)

    ex.errors.length should be(1)
    ex.errors.head.message should be("This field does not meet the minimum length requirement of 1 characters")
  }

  test("Validation should fail if content contains other tags than section on root") {
    val article = articleToValidate.copy(content = Seq(ArticleContent("<h1>lolol</h1>", "nb")))
    val result  = contentValidator.validateArticle(article)
    result.isFailure should be(true)

    val validationMessage = result.failed.get.asInstanceOf[ValidationException].errors.head.message
    validationMessage.contains("An article must consist of one or more <section> blocks") should be(true)
  }

  test("validateArticle throws a validation exception on an invalid visual element") {
    val invalidVisualElement = TestData.visualElement.copy(resource = invalidDocument)
    val article              = articleToValidate.copy(visualElement = Seq(invalidVisualElement))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on a valid visual element") {
    val article = articleToValidate.copy(visualElement = Seq(TestData.visualElement))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle does not throw an exception on an article with plaintext tags") {
    val article = articleToValidate.copy(tags = Seq(Tag(Seq("vann", "snø", "sol"), "nb")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in tags") {
    val article =
      articleToValidate.copy(tags = Seq(Tag(Seq("<h1>vann</h1>", "snø", "sol"), "nb")))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article where metaImageId is a number") {
    val article = articleToValidate.copy(metaImage = Seq(ArticleMetaImage("123", "alttext", "en")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article where metaImageId is not a number") {
    val article =
      articleToValidate.copy(metaImage = Seq(ArticleMetaImage("not a number", "alttext", "en")))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle throws an exception on an article with an illegal required library") {
    val illegalRequiredLib = RequiredLibrary("text/javascript", "naughty", "http://scary.bad.source.net/notNice.js")
    val article            = articleToValidate.copy(requiredLibraries = Seq(illegalRequiredLib))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with a legal required library") {
    val illegalRequiredLib = RequiredLibrary("text/javascript", "h5p", props.H5PResizerScriptUrl)
    val article            = articleToValidate.copy(requiredLibraries = Seq(illegalRequiredLib))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with an invalid license") {
    val article = articleToValidate.copy(
      copyright = Some(DraftCopyright(Some("beerware"), None, Seq(), List(), List(), None, None, false))
    )
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with a valid license") {
    val article = articleToValidate.copy(
      copyright = Some(
        DraftCopyright(
          Some(CC_BY_SA.toString),
          None,
          Seq(Author(ContributorType.Originator, "navn")),
          List(),
          List(),
          None,
          None,
          false
        )
      )
    )
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in copyright origin") {
    val article = articleToValidate.copy(
      copyright = Some(
        DraftCopyright(
          Some(License.CC_BY_SA.toString),
          Some("<h1>origin</h1>"),
          Seq(),
          List(),
          List(),
          None,
          None,
          false
        )
      )
    )
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with plain text in copyright origin") {
    val article = articleToValidate.copy(
      copyright = Some(
        DraftCopyright(
          Some(CC_BY_SA.toString),
          Some("plain text"),
          Seq(),
          List(Author(ContributorType.Processor, "test")),
          List(),
          None,
          None,
          false
        )
      )
    )
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle does not throw an exception on an article with plain text in authors field") {
    val article = articleToValidate.copy(
      copyright = Some(
        DraftCopyright(
          Some(CC_BY_SA.toString),
          None,
          Seq(Author(ContributorType.Writer, "John Doe")),
          List(),
          List(),
          None,
          None,
          false
        )
      )
    )
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in authors field") {
    val article = articleToValidate.copy(
      copyright = Some(
        DraftCopyright(
          Some(License.CC_BY_SA.toString),
          None,
          Seq(Author(ContributorType.Writer, "<h1>john</h1>")),
          List(),
          List(),
          None,
          None,
          false
        )
      )
    )
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("Validation should not fail with language=unknown if allowUnknownLanguage is set") {
    val article = articleToValidate.copy(title = Seq(Title("tittele", "und")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validation should fail if article does not contain a title") {
    val article = articleToValidate.copy(title = Seq.empty)
    val errors  = contentValidator.validateArticle(article)
    errors.isFailure should be(true)
    errors.failed.get.asInstanceOf[ValidationException].errors.head.message should equal(
      "An article must contain at least one title. Perhaps you tried to delete the only title in the article?"
    )
  }

  test("validation should fail if metaImage altText contains html") {
    val article =
      articleToValidate.copy(metaImage = Seq(ArticleMetaImage("1234", "<b>Ikke krutte god<b>", "nb")))
    val Failure(res1: ValidationException) = contentValidator.validateArticle(article)
    res1.errors should be(
      Seq(ValidationMessage("metaImage.alt", "The content contains illegal html-characters. No HTML is allowed"))
    )

    val article2 = articleToValidate.copy(metaImage = Seq(ArticleMetaImage("1234", "Krutte god", "nb")))
    contentValidator.validateArticle(article2).isSuccess should be(true)
  }

  test("validation should fail if metaImageId is an empty string") {
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(
        articleToValidate.copy(
          metaImage = Seq(ArticleMetaImage("", "alt-text", "nb"))
        )
      )

    res.errors.length should be(1)
    res.errors.head.field should be("metaImageId")
    res.errors.head.message should be("Meta image ID must be a number")
  }

  test("validation should fail if revisionMeta does not have unplanned revisions") {
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(
        articleToValidate.copy(
          revisionMeta = Seq.empty
        )
      )

    res.errors.length should be(1)
    res.errors.head.field should be("revisionMeta")
    res.errors.head.message should be("An article must contain at least one planned revision date")
  }

  test("validation should fail if slug field is present but articleType is not frontpage-article") {
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(
        articleToValidate.copy(
          articleType = ArticleType.TopicArticle,
          slug = Some("pepe")
        )
      )

    res.errors.length should be(1)
    res.errors.head.field should be("articleType")
    res.errors.head.message should be(
      s"articleType needs to be of type ${ArticleType.FrontpageArticle.entryName} when slug is defined"
    )
  }

  test("validation should fail if articleType frontpage-article but sluig is None") {
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(
        articleToValidate.copy(
          articleType = ArticleType.FrontpageArticle,
          slug = None
        )
      )

    res.errors.length should be(1)
    res.errors.head.field should be("slug")
    res.errors.head.message should be(
      s"slug field must be defined when articleType is of type ${ArticleType.FrontpageArticle.entryName}"
    )
  }

  test("validation should fail if slug string is invalid") {
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(
        articleToValidate.copy(
          articleType = ArticleType.FrontpageArticle,
          slug = Some("ugyldig slug")
        )
      )

    res.errors.length should be(1)
    res.errors.head.field should be("slug")
    res.errors.head.message should be("The string contains invalid characters")
  }

  test("That we only validate the given language with validateArticleOnLanguage") {
    val article =
      TestData.sampleDomainArticle.copy(
        id = Some(5),
        content =
          Seq(ArticleContent("<section> Valid Content </section>", "nb"), ArticleContent("<div> content <div", "nn")),
        responsible = Some(Responsible("hei", TestData.today)),
        revisionMeta = RevisionMeta.default
      )

    contentValidator.validateArticleOnLanguage(article, Some("nb")).failIfFailure
    val Failure(error: ValidationException) = contentValidator.validateArticle(article)
    val Seq(err1, err2)                     = error.errors
    err1.message.contains("The content contains illegal tags and/or attributes.") should be(true)
    err2.message should be("An article must consist of one or more <section> blocks. Illegal tag(s) are div ")
  }

  test("That validation succeeds if only editor fields are updated") {
    val oldArticle =
      TestData.sampleDomainArticle.copy(
        id = Some(5),
        revision = Some(1),
        responsible = None,
        revisionMeta = RevisionMeta.default
      )
    val article =
      oldArticle.copy(
        id = Some(5),
        revision = Some(2),
        responsible = None,
        revisionMeta = RevisionMeta.default ++ RevisionMeta.default,
        notes = Seq(
          EditorNote("note1", "editor", oldArticle.status, TestData.today),
          EditorNote("note2", "editor", oldArticle.status, TestData.today)
        ),
        comments = Seq(
          Comment(UUID.randomUUID(), TestData.today, TestData.today, "Fin kommentar a gitt", true, false),
          Comment(UUID.randomUUID(), TestData.today, TestData.today, "Fin kommentar igjen a gitt", true, false)
        )
      )

    val result = contentValidator.validateArticleOnLanguage(Some(oldArticle), article, Some("nb"))
    result.get should be(article)
  }

  test("That validation fails if only editor fields are updated and editor validation fails") {
    val oldArticle =
      TestData.sampleDomainArticle.copy(
        id = Some(5),
        revision = Some(1),
        responsible = None,
        revisionMeta = Seq.empty
      )
    val article =
      oldArticle.copy(
        id = Some(5),
        revision = Some(2),
        responsible = None,
        revisionMeta = Seq.empty,
        notes = Seq(
          EditorNote("note1", "editor", oldArticle.status, TestData.today),
          EditorNote("note2", "editor", oldArticle.status, TestData.today)
        ),
        comments = Seq(
          Comment(UUID.randomUUID(), TestData.today, TestData.today, "Fin kommentar a gitt", true, false),
          Comment(UUID.randomUUID(), TestData.today, TestData.today, "Fin kommentar igjen a gitt", true, false)
        )
      )

    val result = contentValidator.validateArticleOnLanguage(Some(oldArticle), article, Some("nb"))
    result.isFailure should be(true)
    val Failure(validationError: ValidationException) = result
    validationError.errors.length should be(1)
    validationError.errors.head.message should be("An article must contain at least one planned revision date")
  }
}
