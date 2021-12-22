/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.validation

import no.ndla.draftapi.DraftApiProperties.H5PResizerScriptUrl
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.validation.{ValidationException, ValidationMessage}
import no.ndla.mapping.License.CC_BY_SA

import scala.util.Failure

class ContentValidatorTest extends UnitSuite with TestEnvironment {
  override val contentValidator = new ContentValidator()
  val validDocument = """<section><h1>heisann</h1><h2>heia</h2></section>"""
  val invalidDocument = """<section><invalid></invalid></section>"""

  test("validateArticle does not throw an exception on a valid document") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(ArticleContent(validDocument, "nb")))
    contentValidator.validateArticle(article).isSuccess should be(true)
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

  test("validateArticle should fail if the title is empty") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = Seq(ArticleTitle("", "nb")))
    val Failure(ex: ValidationException) = contentValidator.validateArticle(article)

    ex.errors.length should be(1)
    ex.errors.head.message should be("This field does not meet the minimum length requirement of 1 characters")
  }

  test("validateArticle should fail if the title is whitespace") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = Seq(ArticleTitle("  ", "nb")))
    val Failure(ex: ValidationException) = contentValidator.validateArticle(article)

    ex.errors.length should be(1)
    ex.errors.head.message should be("This field does not meet the minimum length requirement of 1 characters")
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
    val article = TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("123", "alttext", "en")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article where metaImageId is not a number") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(metaImage = Seq(ArticleMetaImage("not a number", "alttext", "en")))
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
      copyright = Some(Copyright(Some("beerware"), None, Seq(), List(), List(), None, None, None)))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with a valid license") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Some(Copyright(Some(CC_BY_SA.toString), None, Seq(), List(), List(), None, None, None)))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in copyright origin") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Some(Copyright(Some("by-sa"), Some("<h1>origin</h1>"), Seq(), List(), List(), None, None, None)))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("validateArticle does not throw an exception on an article with plain text in copyright origin") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Some(Copyright(Some(CC_BY_SA.toString), None, Seq(), List(), List(), None, None, None)))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle does not throw an exception on an article with plain text in authors field") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright = Some(
        Copyright(Some(CC_BY_SA.toString), None, Seq(Author("author", "John Doe")), List(), List(), None, None, None)))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("validateArticle throws an exception on an article with html in authors field") {
    val article = TestData.sampleArticleWithByNcSa.copy(
      copyright =
        Some(Copyright(Some("by-sa"), None, Seq(Author("author", "<h1>john</h1>")), List(), List(), None, None, None)))
    contentValidator.validateArticle(article).isFailure should be(true)
  }

  test("Validation should not fail with language=unknown if allowUnknownLanguage is set") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = Seq(ArticleTitle("tittele", "und")))
    contentValidator.validateArticle(article).isSuccess should be(true)
  }

  test("Validation should fail if agreement title contains html") {
    val agreement = TestData.sampleDomainAgreement.copy(title = "<h1>HEY TITLE</h1>")
    contentValidator.validateAgreement(agreement).isSuccess should be(false)
  }

  test("Validation should succeed if agreement title contains no html") {
    val agreement = TestData.sampleDomainAgreement.copy(title = "HEY TITLE")
    contentValidator.validateAgreement(agreement).isSuccess should be(true)
  }

  test("Validation should fail if agreement content contains html") {
    val agreement = TestData.sampleDomainAgreement.copy(content = "<h1>HEY CONTENT</h1>")
    contentValidator.validateAgreement(agreement).isSuccess should be(false)
  }

  test("Validation should succeed if agreement content contains no html") {
    val agreement = TestData.sampleDomainAgreement.copy(content = "HEY CONTENT")
    contentValidator.validateAgreement(agreement).isSuccess should be(true)
  }

  test("validation should fail if article does not contain a title") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = Seq.empty)
    val errors = contentValidator.validateArticle(article)
    errors.isFailure should be(true)
    errors.failed.get.asInstanceOf[ValidationException].errors.head.message should equal(
      "An article must contain at least one title. Perhaps you tried to delete the only title in the article?")
  }

  test("validation should fail if validFrom can not be parsed") {
    val agreementCopyright = TestData.newAgreement.copyright.copy(validFrom = Some("abc"), validTo = Some("def"))
    val errors = contentValidator.validateDates(agreementCopyright)

    errors.size should be(2)
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

  test("validation should fail if metaImageId is an empty string") {
    val Failure(res: ValidationException) =
      contentValidator.validateArticle(
        TestData.sampleArticleWithByNcSa.copy(
          metaImage = Seq(ArticleMetaImage("", "alt-text", "nb"))
        ))

    res.errors.length should be(1)
    res.errors.head.field should be("metaImageId")
    res.errors.head.message should be("Meta image ID must be a number")
  }

}
