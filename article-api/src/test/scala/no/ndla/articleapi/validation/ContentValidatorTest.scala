/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.{
  ArticleContent,
  ArticleMetaImage,
  Author,
  ContributorType,
  Description,
  Introduction,
  RequiredLibrary,
  Tag,
  Title
}
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.domain.language.OptLanguageFields
import no.ndla.mapping.License.{CC_BY_SA, NA}

import scala.util.Failure

class ContentValidatorTest extends UnitSuite with TestEnvironment {
  override val contentValidator = new ContentValidator
  val validDocument             = """<section><h1>heisann</h1><h2>heia</h2></section>"""
  val validIntroduction         = """<p>heisann <span lang="en">heia</span></p><p>hopp</p>"""
  val invalidDocument           = """<section><invalid></invalid></section>"""
  val validDisclaimer           =
    """<p><strong>hallo!</strong><ndlaembed data-content-id="123" data-open-in="current-context" data-resource="content-link" data-content-type="article">test</ndlaembed></p>"""

  test("validateArticle does not throw an exception on a valid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(validDocument, "nb")))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validation should fail if article has no content") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq.empty)
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle throws a validation exception on an invalid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(invalidDocument, "nb")))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception for MathMl tags") {
    val content = """<section><math xmlns="http://www.w3.org/1998/Math/MathML"></math></section>"""
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(content, "nb")))

    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle should throw an error if introduction contains illegal HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      introduction = Seq(Introduction(validDocument, "nb"))
    )
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle should not throw an error if introduction contains legal HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      introduction = Seq(Introduction(validIntroduction, "nb"))
    )
    contentValidator.validateArticle(article, false).isFailure should be(false)
  }

  test("validateArticle should not throw an error if introduction contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      introduction = Seq(Introduction("introduction", "nb"))
    )
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle should throw an error if disclaimer contains illegal HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      disclaimer = OptLanguageFields.withValue("<p><hallo>hei</hallo></p>", "nb")
    )

    val Failure(error: ValidationException) = contentValidator.validateArticle(article, false)
    error should be(
      ValidationException(
        "disclaimer.nb",
        "The content contains illegal tags and/or attributes. Allowed HTML tags are: h3, msgroup, a, article, sub, sup, mtext, msrow, tbody, mtd, pre, thead, figcaption, mover, msup, semantics, ol, span, mroot, munder, h4, mscarries, dt, nav, mtr, ndlaembed, li, br, mrow, merror, mphantom, u, audio, ul, maligngroup, mfenced, annotation, div, strong, section, i, mspace, malignmark, mfrac, code, h2, td, aside, em, mstack, button, dl, th, tfoot, math, tr, b, blockquote, msline, col, annotation-xml, mstyle, caption, mpadded, mo, mlongdiv, msubsup, p, munderover, maction, menclose, h1, details, mmultiscripts, msqrt, mscarry, mstac, mi, mglyph, mlabeledtr, mtable, mprescripts, summary, mn, msub, ms, table, colgroup, dd"
      ).copy(message = "Article with id 1 failed article validation")
    )
  }

  test("validateArticle should not throw an error if disclaimer contains legal HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      disclaimer = OptLanguageFields.withValue(validDisclaimer, "nb")
    )
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle should not throw an error if disclaimer contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      disclaimer = OptLanguageFields.withValue("disclaimer", "nb")
    )
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle should throw an error if metaDescription contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      metaDescription = Seq(Description(validDocument, "nb"))
    )
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle should not throw an error if metaDescription contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      metaDescription = Seq(Description("meta description", "nb"))
    )
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle should throw an error if title contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      title = Seq(Title(validDocument, "nb"))
    )
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle should not throw an error if title contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(ArticleContent(validDocument, "nb")),
      title = Seq(Title("title", "nb"))
    )
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle should fail if the title exceeds 256 bytes") {
    val article                          = TestData.sampleArticleWithByNcSa.copy(title = Seq(Title("A" * 257, "nb")))
    val Failure(ex: ValidationException) = contentValidator.validateArticle(article, false)

    ex.errors.length should be(1)
    ex.errors.head.message should be("This field exceeds the maximum permitted length of 256 characters")
  }

  test("Validation should fail if content contains other tags than section on root") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent("<h1>lolol</h1>", "nb")))
    val result  = contentValidator.validateArticle(article, false)
    result.isFailure should be(true)

    val validationMessage = result.failed.get.asInstanceOf[ValidationException].errors.head.message
    validationMessage.contains("An article must consist of one or more <section> blocks") should be(true)
  }

  test("validateArticle throws a validation exception on an invalid visual element") {
    val invalidVisualElement = TestData.visualElement.copy(resource = invalidDocument)
    val article              = TestData.sampleArticleWithByNcSa.copy(visualElement = Seq(invalidVisualElement))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on a valid visual element") {
    val article = TestData.sampleArticleWithByNcSa.copy(visualElement = Seq(TestData.visualElement))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle does not throw an exception on an article with plaintext tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(tags = Seq(Tag(Seq("vann", "snø", "sol"), "nb")))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in tags") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(tags = Seq(Tag(Seq("<h1>vann</h1>", "snø", "sol"), "nb")))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article where metaImageId is a number") {
    val article = TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("123", "alt", "nb")))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article where metaImageId is not a number") {
    val article = TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("not a number", "alt", "nb")))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle throws an exception on an article with an illegal required library") {
    val illegalRequiredLib = RequiredLibrary("text/javascript", "naughty", "http://scary.bad.source.net/notNice.js")
    val article            = TestData.sampleArticleWithByNcSa.copy(requiredLibraries = Seq(illegalRequiredLib))
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with a legal required library") {
    val illegalRequiredLib = RequiredLibrary("text/javascript", "h5p", props.H5PResizerScriptUrl)
    val article            = TestData.sampleArticleWithByNcSa.copy(requiredLibraries = Seq(illegalRequiredLib))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with an invalid license") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright =
        Copyright("beerware", None, Seq(Author(ContributorType.Writer, "John doe")), Seq(), Seq(), None, None, false)
    )
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with a valid license") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(
        copyright =
          Copyright("CC-BY-SA-4.0", None, Seq(Author(ContributorType.Writer, "test")), Seq(), Seq(), None, None, false)
      )
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in copyright origin") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Copyright(
        "CC-BY-SA-4.0",
        Some("<h1>origin</h1>"),
        Seq(Author(ContributorType.Writer, "John Doe")),
        Seq(),
        Seq(),
        None,
        None,
        false
      )
    )
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with plain text in copyright origin") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(
        copyright = Copyright(
          "CC-BY-SA-4.0",
          None,
          Seq(Author(ContributorType.Writer, "John doe")),
          Seq(),
          Seq(),
          None,
          None,
          false
        )
      )
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle does not throw an exception on an article with plain text in authors field") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Copyright(
        "CC-BY-SA-4.0",
        None,
        Seq(Author(ContributorType.Writer, "John Doe")),
        Seq(),
        Seq(),
        None,
        None,
        false
      )
    )
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in authors field") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Copyright(
        "CC-BY-SA",
        None,
        Seq(Author(ContributorType.Writer, "<h1>john</h1>")),
        Seq(),
        Seq(),
        None,
        None,
        false
      )
    )
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with correct author type") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Copyright(
        "CC-BY-SA-4.0",
        None,
        Seq(Author(ContributorType.Writer, "John Doe")),
        Seq(),
        Seq(),
        None,
        None,
        false
      )
    )
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with empty author name") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright =
        Copyright("CC-BY-SA-4.0", None, Seq(Author(ContributorType.Writer, "")), Seq(), Seq(), None, None, false)
    )
    val result = contentValidator.validateArticle(article, false)
    result.isSuccess should be(false)
    result.failed.get.asInstanceOf[ValidationException].errors.length should be(1)
    result.failed.get.asInstanceOf[ValidationException].errors.head.message should be(
      "This field is shorter than the minimum permitted length of 1 characters"
    )
    result.failed.get.asInstanceOf[ValidationException].errors.head.field should be("copyright.creators.name")
  }

  test("Validation should not fail with language=unknown if allowUnknownLanguage is set") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = Seq(Title("tittele", "und")))
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("validation should fail if metaImage altText contains html") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("1234", "<b>Ikke krutte god<b>", "nb")))
    val Failure(res1: ValidationException) = contentValidator.validateArticle(article, false)
    res1.errors should be(
      Seq(ValidationMessage("metaImage.alt", "The content contains illegal html-characters. No HTML is allowed"))
    )

    val article2 = TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("1234", "Krutte god", "nb")))
    contentValidator.validateArticle(article2, false).isSuccess should be(true)
  }

  test("validation should fail if not imported and tags are < 3") {
    val Failure(res0: ValidationException) = contentValidator.validateArticle(
      TestData.sampleArticleWithByNcSa.copy(tags = Seq(Tag(Seq("a", "b"), "nb"))),
      false
    )

    res0.errors should be(
      Seq(ValidationMessage("tags.nb", s"Invalid amount of tags. Articles needs 3 or more tags to be valid."))
    )

    val Failure(res1: ValidationException) =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(
          tags = Seq(Tag(Seq("a", "b", "c"), "nb"), Tag(Seq("a", "b"), "en"))
        ),
        false
      )

    res1.errors should be(
      Seq(ValidationMessage("tags.en", s"Invalid amount of tags. Articles needs 3 or more tags to be valid."))
    )

    val Failure(res2: ValidationException) =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(
          tags = Seq(Tag(Seq("a"), "en"), Tag(Seq("a"), "nb"), Tag(Seq("a", "b", "c"), "nn"))
        ),
        false
      )
    res2.errors.sortBy(_.field) should be(
      Seq(
        ValidationMessage("tags.en", s"Invalid amount of tags. Articles needs 3 or more tags to be valid."),
        ValidationMessage("tags.nb", s"Invalid amount of tags. Articles needs 3 or more tags to be valid.")
      )
    )

    val res3 =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(
          tags = Seq(Tag(Seq("a", "b", "c"), "nb"), Tag(Seq("a", "b", "c"), "nn"))
        ),
        false
      )
    res3.isSuccess should be(true)
  }

  test("imported articles should pass validation for amount of tags") {
    val res0 = contentValidator.validateArticle(
      TestData.sampleArticleWithByNcSa.copy(
        tags = Seq(Tag(Seq("a"), "en"), Tag(Seq("a"), "nb"), Tag(Seq("a", "b", "c"), "nn"))
      ),
      isImported = true
    )
    res0.isSuccess should be(true)

    val res1 =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(tags = Seq(Tag(Seq("a"), "en"))),
        isImported = true
      )
    res1.isSuccess should be(true)

    val Failure(res2: ValidationException) =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(tags = Seq(Tag(Seq("<strong>a</strong>", "b", "c"), "nn"))),
        isImported = true
      )
    res2.errors should be(
      Seq(
        ValidationMessage("tags.nn", s"The content contains illegal html-characters. No HTML is allowed")
      )
    )
  }

  test("imported articles should pass validation for missing metaDescription") {
    val res0 = contentValidator.validateArticle(
      TestData.sampleArticleWithByNcSa.copy(metaDescription = Seq.empty),
      isImported = true
    )
    res0.isSuccess should be(true)
  }

  test("validation should fail if there are no tags for any languages") {
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(TestData.sampleArticleWithByNcSa.copy(tags = Seq()), false)
    res.errors.length should be(1)
    res.errors.head.field should equal("tags")
    res.errors.head.message should equal("The article must have at least one set of tags")
  }

  test("validation should fail if metaImageId is an empty string") {
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(
          metaImage = Seq(ArticleMetaImage("", "alt-text", "nb"))
        ),
        false
      )

    res.errors.length should be(1)
    res.errors.head.field should be("metaImageId")
    res.errors.head.message should be("Meta image ID must be a number")
  }
  test("validation should fail if license is chosen and no copyright holders are provided") {
    val copyright                         = Copyright(CC_BY_SA.toString, None, Seq(), Seq(), Seq(), None, None, false)
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(TestData.sampleArticleWithByNcSa.copy(copyright = copyright), false)
    res.errors.length should be(1)
    res.errors.head.field should be("license.license")
    res.errors.head.message should be("At least one copyright holder is required when license is CC-BY-SA-4.0")
  }

  test("an article with no copyright holders can pass validation if license is N/A") {
    val copyright = Copyright(NA.toString, None, Seq(), Seq(), Seq(), None, None, false)
    val article   = TestData.sampleArticleWithCopyrighted.copy(copyright = copyright)
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("an article with one or more copyright holder can pass validation, regardless of license") {
    val copyright =
      Copyright(CC_BY_SA.toString, None, Seq(Author(ContributorType.Reader, "test")), Seq(), Seq(), None, None, false)
    val article = TestData.sampleArticleWithByNcSa.copy(copyright = copyright)
    contentValidator.validateArticle(article, false).isSuccess should be(true)
  }

  test("softvalidation is more lenient than strictvalidation") {
    val Failure(strictRes: ValidationException) = contentValidator.validateArticle(
      TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("", "alt-text", "nb"))),
      false
    )

    val softRes = contentValidator.softValidateArticle(
      TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("", "alt-text", "nb"))),
      false
    )

    strictRes.errors.length should be(1)
    strictRes.errors.head.field should be("metaImageId")
    strictRes.errors.head.message should be("Meta image ID must be a number")

    softRes.isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with a missing revisionDate") {
    val article = TestData.sampleArticleWithByNcSa.copy(revisionDate = None)
    contentValidator.validateArticle(article, false).isFailure should be(true)
  }
}
