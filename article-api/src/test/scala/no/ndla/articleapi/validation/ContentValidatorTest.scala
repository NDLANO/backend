/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.ArticleApiProperties.H5PResizerScriptUrl
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.validation.{ValidationException, ValidationMessage}
import org.mockito.Mockito._

import scala.util.{Failure, Success}

class ContentValidatorTest extends UnitSuite with TestEnvironment {
  override val contentValidator = new ContentValidator()
  val validDocument = """<section><h1>heisann</h1><h2>heia</h2></section>"""
  val invalidDocument = """<section><invalid></invalid></section>"""

  test("validateArticle does not throw an exception on a valid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(validDocument, "nb")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validation should fail if article has no content") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq.empty)
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle throws a validation exception on an invalid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(invalidDocument, "nb")))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception for MathMl tags") {
    val content = """<section><math xmlns="http://www.w3.org/1998/Math/MathML"></math></section>"""
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(content, "nb")))

    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle should throw an error if introduction contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(validDocument, "nb")),
                                                        introduction = Seq(ArticleIntroduction(validDocument, "nb")))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle should not throw an error if introduction contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(validDocument, "nb")),
                                                        introduction = Seq(ArticleIntroduction("introduction", "nb")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle should throw an error if metaDescription contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(validDocument, "nb")),
                                                        metaDescription =
                                                          Seq(ArticleMetaDescription(validDocument, "nb")))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle should not throw an error if metaDescription contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(validDocument, "nb")),
                                                        metaDescription =
                                                          Seq(ArticleMetaDescription("meta description", "nb")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle should throw an error if title contains HTML tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(validDocument, "nb")),
                                                        title = Seq(ArticleTitle(validDocument, "nb")))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle should not throw an error if title contains plain text") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(validDocument, "nb")),
                                                        title = Seq(ArticleTitle("title", "nb")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle should fail if the title exceeds 256 bytes") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = Seq(ArticleTitle("A" * 257, "nb")))
    val Failure(ex: ValidationException) = contentValidator.validateArticle(article)

    ex.errors.length should be(1)
    ex.errors.head.message should be("This field exceeds the maximum permitted length of 256 characters")
  }

  test("Validation should fail if content contains other tags than section on root") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent("<h1>lolol</h1>", "nb")))
    val result = contentValidator.validateArticle(article)
    result.isFailure should be(true)

    val validationMessage = result.failed.get.asInstanceOf[ValidationException].errors.head.message
    validationMessage.contains("An article must consist of one or more <section> blocks") should be(true)
  }

  test("validateArticle throws a validation exception on an invalid visual element") {
    val invalidVisualElement = TestData.visualElement.copy(resource = invalidDocument)
    val article = TestData.sampleArticleWithByNcSa.copy(visualElement = Seq(invalidVisualElement))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on a valid visual element") {
    val article = TestData.sampleArticleWithByNcSa.copy(visualElement = Seq(TestData.visualElement))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle does not throw an exception on an article with plaintext tags") {
    val article = TestData.sampleArticleWithByNcSa.copy(tags = Seq(ArticleTag(Seq("vann", "snø", "sol"), "nb")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in tags") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(tags = Seq(ArticleTag(Seq("<h1>vann</h1>", "snø", "sol"), "nb")))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article where metaImageId is a number") {
    val article = TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("123", "alt", "nb")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article where metaImageId is not a number") {
    val article = TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("not a number", "alt", "nb")))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle throws an exception on an article with an illegal required library") {
    val illegalRequiredLib = RequiredLibrary("text/javascript", "naughty", "http://scary.bad.source.net/notNice.js")
    val article = TestData.sampleArticleWithByNcSa.copy(requiredLibraries = Seq(illegalRequiredLib))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with a legal required library") {
    val illegalRequiredLib = RequiredLibrary("text/javascript", "h5p", H5PResizerScriptUrl)
    val article = TestData.sampleArticleWithByNcSa.copy(requiredLibraries = Seq(illegalRequiredLib))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with an invalid license") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Copyright("beerware", "", Seq(), Seq(), Seq(), None, None, None))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with a valid license") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(
        copyright = Copyright("CC-BY-SA-4.0", "", Seq(), Seq(), Seq(), None, None, None))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in copyright origin") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Copyright("CC-BY-SA-4.0", "<h1>origin</h1>", Seq(), Seq(), Seq(), None, None, None))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with plain text in copyright origin") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(
        copyright = Copyright("CC-BY-SA-4.0", "", Seq(), Seq(), Seq(), None, None, None))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle does not throw an exception on an article with plain text in authors field") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Copyright("CC-BY-SA-4.0", "", Seq(Author("Writer", "John Doe")), Seq(), Seq(), None, None, None))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in authors field") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Copyright("CC-BY-SA", "", Seq(Author("Writer", "<h1>john</h1>")), Seq(), Seq(), None, None, None))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with correct author type") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Copyright("CC-BY-SA-4.0", "", Seq(Author("Writer", "John Doe")), Seq(), Seq(), None, None, None))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with invalid author type") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Copyright("CC-BY-SA-4.0", "", Seq(Author("invalid", "John Doe")), Seq(), Seq(), None, None, None))
    val result = contentValidator.validateArticle(article)
    result.isSuccess should be(false)
    result.failed.get.asInstanceOf[ValidationException].errors.length should be(1)
    result.failed.get.asInstanceOf[ValidationException].errors.head.message should be(
      "Author is of illegal type. Must be one of originator, photographer, artist, writer, scriptwriter, reader, translator, director, illustrator, cowriter, composer")
    result.failed.get.asInstanceOf[ValidationException].errors.head.field should be("copyright.creators.type")
  }

  test("validateArticle throws an exception on an article with an invalid article type") {
    val article = TestData.sampleArticleWithByNcSa.copy(articleType = "invalid")
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("Validation should not fail with language=unknown if allowUnknownLanguage is set") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = Seq(ArticleTitle("tittele", "und")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("Validation should succeed if agreement exists") {
    when(draftApiClient.agreementExists(10)).thenReturn(true)
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = TestData.sampleArticleWithByNcSa.copyright.copy(agreementId = Some(10)))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("Validation should fail if agreement doesnt exist") {
    when(draftApiClient.agreementExists(10)).thenReturn(false)
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = TestData.sampleArticleWithByNcSa.copyright.copy(agreementId = Some(10)))
    contentValidator.validateArticle(article).isSuccess should be(false)
  }

  test("validation should fail if metaImage altText contains html") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("1234", "<b>Ikke krutte god<b>", "nb")))
    val Failure(res1: ValidationException) = contentValidator.validateArticle(article)
    res1.errors should be(
      Seq(ValidationMessage("metaImage.alt", "The content contains illegal html-characters. No HTML is allowed")))

    val article2 = TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("1234", "Krutte god", "nb")))
    contentValidator.validateArticle(article2).isSuccess should be(true)
  }

  test("validation should fail if not imported and tags are < 3") {
    val Failure(res0: ValidationException) = contentValidator.validateArticle(
      TestData.sampleArticleWithByNcSa.copy(tags = Seq(ArticleTag(Seq("a", "b"), "nb"))))

    res0.errors should be(
      Seq(ValidationMessage("tags.nb", s"Invalid amount of tags. Articles needs 3 or more tags to be valid.")))

    val Failure(res1: ValidationException) =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(
          tags = Seq(ArticleTag(Seq("a", "b", "c"), "nb"), ArticleTag(Seq("a", "b"), "en"))))

    res1.errors should be(
      Seq(ValidationMessage("tags.en", s"Invalid amount of tags. Articles needs 3 or more tags to be valid.")))

    val Failure(res2: ValidationException) =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(
          tags = Seq(ArticleTag(Seq("a"), "en"), ArticleTag(Seq("a"), "nb"), ArticleTag(Seq("a", "b", "c"), "nn"))))
    res2.errors.sortBy(_.field) should be(
      Seq(
        ValidationMessage("tags.en", s"Invalid amount of tags. Articles needs 3 or more tags to be valid."),
        ValidationMessage("tags.nb", s"Invalid amount of tags. Articles needs 3 or more tags to be valid.")
      ))

    val res3 =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(
          tags = Seq(ArticleTag(Seq("a", "b", "c"), "nb"), ArticleTag(Seq("a", "b", "c"), "nn"))))
    res3.isSuccess should be(true)
  }

  test("imported articles should pass validation for amount of tags") {
    val res0 = contentValidator.validateArticle(
      TestData.sampleArticleWithByNcSa.copy(
        tags = Seq(ArticleTag(Seq("a"), "en"), ArticleTag(Seq("a"), "nb"), ArticleTag(Seq("a", "b", "c"), "nn"))),
      isImported = true
    )
    res0.isSuccess should be(true)

    val res1 =
      contentValidator.validateArticle(TestData.sampleArticleWithByNcSa.copy(tags = Seq(ArticleTag(Seq("a"), "en"))),
                                       isImported = true)
    res1.isSuccess should be(true)

    val Failure(res2: ValidationException) =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(tags = Seq(ArticleTag(Seq("<strong>a</strong>", "b", "c"), "nn"))),
        isImported = true)
    res2.errors should be(
      Seq(
        ValidationMessage("tags.nn", s"The content contains illegal html-characters. No HTML is allowed")
      ))
  }

  test("imported articles should pass validation for missing metaDescription") {
    val res0 = contentValidator.validateArticle(TestData.sampleArticleWithByNcSa.copy(metaDescription = Seq.empty),
                                                isImported = true)
    res0.isSuccess should be(true)
  }

  test("validation should fail if there are no tags for any languages") {
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(TestData.sampleArticleWithByNcSa.copy(tags = Seq()))
    res.errors.length should be(1)
    res.errors.head.field should equal("tags")
    res.errors.head.message should equal("The article must have at least one set of tags")
  }

  test("validation should fail if metaImageId is an empty string") {
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(TestData.sampleArticleWithByNcSa.copy(
                                         metaImage = Seq(ArticleMetaImage("", "alt-text", "nb"))
                                       ),
                                       false)

    res.errors.length should be(1)
    res.errors.head.field should be("metaImageId")
    res.errors.head.message should be("Meta image ID must be a number")
  }

  test("softvalidation is more lenient than strictvalidation") {
    val Failure(strictRes: ValidationException) = contentValidator.validateArticle(
      TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("", "alt-text", "nb"))),
      false)

    val softRes = contentValidator.softValidateArticle(
      TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("", "alt-text", "nb"))),
      false)

    strictRes.errors.length should be(1)
    strictRes.errors.head.field should be("metaImageId")
    strictRes.errors.head.message should be("Meta image ID must be a number")

    softRes.isSuccess should be(true)
  }
}
