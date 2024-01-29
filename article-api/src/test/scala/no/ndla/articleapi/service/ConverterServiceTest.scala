/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model
import no.ndla.common.model.{NDLADate, RelatedContentLink, api => commonApi}
import no.ndla.common.model.api.{License, UpdateWith}
import no.ndla.common.model.domain.{Author, Availability, Description, Introduction, RequiredLibrary, Tag, Title}
import no.ndla.common.model.domain.article.Copyright

import scala.util.Success

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val service         = new ConverterService
  val contentTitle    = Title("", "und")
  val author          = Author("forfatter", "Henrik")
  val tag             = Tag(List("asdf"), "nb")
  val requiredLibrary = RequiredLibrary("", "", "")
  val nodeId          = "1234"
  val sampleAlt       = "Fotografi"

  test("toApiLicense defaults to unknown if the license was not found") {
    service.toApiLicense("invalid") should equal(License("unknown", None, None))
  }

  test("toApiLicense converts a short license string to a license object with description and url") {
    service.toApiLicense("CC-BY-4.0") should equal(
      model.api.License(
        "CC-BY-4.0",
        Some("Creative Commons Attribution 4.0 International"),
        Some("https://creativecommons.org/licenses/by/4.0/")
      )
    )
  }

  test("toApiArticleTitle returns both title and htmlTitle") {
    val title = Title("Title with <span data-language=\"uk\">ukrainian</span> word", "en")
    service.toApiArticleTitle(title) should equal(
      api.ArticleTitle("Title with ukrainian word", "Title with <span data-language=\"uk\">ukrainian</span> word", "en")
    )
  }

  test("toApiArticleIntroduction returns both introduction and htmlIntroduction") {
    val introduction = Introduction("Introduction with <em>emphasis</em>", "en")
    service.toApiArticleIntroduction(introduction) should equal(
      api.ArticleIntroduction("Introduction with emphasis", "Introduction with <em>emphasis</em>", "en")
    )
  }

  test("toApiArticleV2 converts a domain.Article to an api.ArticleV2") {
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    service.toApiArticleV2(TestData.sampleDomainArticle, "nb", false) should equal(Success(TestData.apiArticleV2))
  }

  test("that toApiArticleV2 returns sorted supportedLanguages") {
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    val result = service.toApiArticleV2(
      TestData.sampleDomainArticle.copy(title = TestData.sampleDomainArticle.title :+ Title("hehe", "und")),
      "nb",
      false
    )
    result.get.supportedLanguages should be(Seq("nb", "und"))
  }

  test("toApiArticleV2 returns None when language is not supported") {
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    service.toApiArticleV2(TestData.sampleDomainArticle, "someRandomLanguage", false).isFailure should be(true)
    service.toApiArticleV2(TestData.sampleDomainArticle, "", false).isFailure should be(true)
  }

  test("toApiArticleV2 should always an article if language neutral") {
    val domainArticle = TestData.sampleDomainArticleWithLanguage("und")
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    service.toApiArticleV2(domainArticle, "someRandomLanguage", false).isSuccess should be(true)
  }

  test(
    "toApiArticleV2 should return Failure if article does not exist on the language asked for and is not language neutral"
  ) {
    val domainArticle = TestData.sampleDomainArticleWithLanguage("en")
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    service.toApiArticleV2(domainArticle, "someRandomLanguage", false).isFailure should be(true)
  }

  test("that toApiArticleV2 returns none if article does not exist on language, and fallback is not specified") {
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    val result = service.toApiArticleV2(TestData.sampleDomainArticle, "en", false)
    result.isFailure should be(true)
  }

  test(
    "That toApiArticleV2 returns article on existing language if fallback is specified even if selected language does not exist"
  ) {
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    val result = service.toApiArticleV2(TestData.sampleDomainArticle, "en", fallback = true)
    result.get.title.language should be("nb")
    result.get.title.title should be(TestData.sampleDomainArticle.title.head.title)
    result.isFailure should be(false)
  }

  test("That oldToNewLicenseKey throws on invalid license") {
    assertThrows[ImportException] {
      service.oldToNewLicenseKey("publicdomain")
    }
  }

  test("That oldToNewLicenseKey converts correctly") {
    service.oldToNewLicenseKey("nolaw") should be("CC0-1.0")
    service.oldToNewLicenseKey("noc") should be("PD")
  }

  test("That oldToNewLicenseKey does not convert an license that should not be converted") {
    service.oldToNewLicenseKey("CC-BY-SA-4.0") should be("CC-BY-SA-4.0")
  }

  test("That hitAsArticleSummaryV2 returns correct summary") {
    val id                 = 8
    val title              = "Baldur har mareritt"
    val visualElement      = "image"
    val introduction       = "Baldur"
    val metaDescription    = "Hurr Durr"
    val metaImageAlt       = "Alt text is here"
    val license            = "publicdomain"
    val articleType        = "topic-article"
    val supportedLanguages = Seq("nb", "en")
    val availability       = "everyone"
    val hitString =
      s"""{  "availability": "everyone", "visualElement": {    "en": "$visualElement"  },  "introduction": {    "nb": "$introduction"  }, "metaImage": [{"imageId": "1", "altText": "$metaImageAlt", "language": "nb"}], "tags": {"nb": ["test"]},  "metaDescription": {    "nb": "$metaDescription"  },  "lastUpdated": "2017-12-29T07:18:27Z",  "tags.nb": [    "baldur"  ],  "license": "$license",  "id": $id,  "authors": [],  "content": {    "nb": "Bilde av Baldurs mareritt om Ragnarok."  },  "defaultTitle": "Baldur har mareritt",  "title": {    "nb": "Baldur har mareritt"  },  "articleType": "$articleType"}"""

    val result = service.hitAsArticleSummaryV2(hitString, "nb")

    result.id should equal(id)
    result.title.title should equal(title)
    result.visualElement.get.visualElement should equal(visualElement)
    result.introduction.get.introduction should equal(introduction)
    result.metaDescription.get.metaDescription should equal(metaDescription)
    result.metaImage.get.alt should equal(metaImageAlt)
    result.license should equal(license)
    result.articleType should equal(articleType)
    result.supportedLanguages should equal(supportedLanguages)
    result.availability should equal(availability)
  }

  test("That updateExistingTags updates tags correctly") {
    val existingTags = Seq(Tag(Seq("nb-tag1", "nb-tag2"), "nb"), Tag(Seq("Guten", "Tag"), "de"))
    val updatedTags = Seq(
      Tag(Seq("new-nb-tag1", "new-nb-tag2", "new-nb-tag3"), "nb"),
      Tag(Seq("new-nn-tag1"), "nn"),
      Tag(Seq("new-es-tag1", "new-es-tag2"), "es")
    )
    val expectedTags =
      Seq(Tag(Seq("new-nb-tag1", "new-nb-tag2", "new-nb-tag3"), "nb"), Tag(Seq("Guten", "Tag"), "de"))

    service.updateExistingTagsField(existingTags, updatedTags) should be(expectedTags)
    service.updateExistingTagsField(existingTags, Seq.empty) should be(existingTags)
    service.updateExistingTagsField(Seq.empty, updatedTags) should be(Seq.empty)
  }

  test("That updateExistingArticleMetaDescription updates metaDesc correctly") {
    val existingMetaDesc = Seq(Description("nb-content", "nb"), Description("en-content", "en"))
    val updatedMetaDesc = Seq(
      Description("new-nb-content", "nb"),
      Description("new-nn-content", "nn"),
      Description("new-es-content", "es")
    )
    val expectedMetaDesc =
      Seq(Description("new-nb-content", "nb"), Description("en-content", "en"))

    service.updateExistingMetaDescriptionField(existingMetaDesc, updatedMetaDesc) should be(expectedMetaDesc)
    service.updateExistingMetaDescriptionField(existingMetaDesc, Seq.empty) should be(existingMetaDesc)
    service.updateExistingMetaDescriptionField(Seq.empty, updatedMetaDesc) should be(Seq.empty)
  }

  test("That updateArticleFields updates all fields") {
    val existingArticle = TestData.sampleDomainArticle.copy(
      availability = Availability.everyone,
      grepCodes = Seq("old", "code"),
      copyright = Copyright("CC-BY-4.0", Some("origin"), Seq(), Seq(), Seq(), None, None, false),
      metaDescription = Seq(Description("gammelDesc", "nb")),
      relatedContent = Seq(Left(RelatedContentLink("title1", "url1")), Right(12L)),
      tags = Seq(Tag(Seq("gammel", "Tag"), "nb"))
    )

    val revisionDate = NDLADate.now()

    val partialArticle =
      api.PartialPublishArticle(
        availability = Some(Availability.teacher),
        grepCodes = Some(Seq("New", "grep", "codes")),
        license = Some("newLicense"),
        metaDescription = Some(Seq(api.ArticleMetaDescription("nyDesc", "nb"))),
        relatedContent = Some(
          Seq(
            Left(commonApi.RelatedContentLink("New Title", "New Url")),
            Left(commonApi.RelatedContentLink("Newer Title", "Newer Url")),
            Right(42L)
          )
        ),
        tags = Some(Seq(api.ArticleTag(Seq("nye", "Tags"), "nb"))),
        revisionDate = UpdateWith(revisionDate)
      )
    val updatedArticle = TestData.sampleDomainArticle.copy(
      availability = Availability.teacher,
      grepCodes = Seq("New", "grep", "codes"),
      copyright = Copyright("newLicense", Some("origin"), Seq(), Seq(), Seq(), None, None, false),
      metaDescription = Seq(Description("nyDesc", "nb")),
      relatedContent = Seq(
        Left(RelatedContentLink("New Title", "New Url")),
        Left(RelatedContentLink("Newer Title", "Newer Url")),
        Right(42L)
      ),
      tags = Seq(Tag(Seq("nye", "Tags"), "nb")),
      revisionDate = Some(revisionDate)
    )

    service.updateArticleFields(existingArticle, partialArticle) should be(updatedArticle)

  }

  test("That updateArticleFields does not create new fields") {
    val existingArticle = TestData.sampleDomainArticle.copy(
      availability = Availability.everyone,
      grepCodes = Seq("old", "code"),
      copyright = Copyright("CC-BY-4.0", Some("origin"), Seq(), Seq(), Seq(), None, None, false),
      metaDescription = Seq(Description("oldDesc", "de")),
      relatedContent =
        Seq(Left(RelatedContentLink("title1", "url1")), Left(RelatedContentLink("old title", "old url"))),
      tags = Seq(Tag(Seq("Gluten", "Tag"), "de"))
    )

    val revisionDate = NDLADate.now()
    val partialArticle =
      api.PartialPublishArticle(
        availability = Some(Availability.teacher),
        grepCodes = Some(Seq("New", "grep", "codes")),
        license = Some("newLicense"),
        metaDescription = Some(
          Seq(
            api.ArticleMetaDescription("nyDesc", "nb"),
            api.ArticleMetaDescription("newDesc", "en"),
            api.ArticleMetaDescription("neuDesc", "de")
          )
        ),
        relatedContent = Some(Seq(Right(42L), Right(420L), Right(4200L))),
        tags = Some(
          Seq(
            api.ArticleTag(Seq("nye", "Tags"), "nb"),
            api.ArticleTag(Seq("new", "Tagss"), "en"),
            api.ArticleTag(Seq("Guten", "Tag"), "de")
          )
        ),
        revisionDate = UpdateWith(revisionDate)
      )
    val updatedArticle = TestData.sampleDomainArticle.copy(
      availability = Availability.teacher,
      grepCodes = Seq("New", "grep", "codes"),
      copyright = Copyright("newLicense", Some("origin"), Seq(), Seq(), Seq(), None, None, false),
      metaDescription = Seq(Description("neuDesc", "de")),
      relatedContent = Seq(Right(42L), Right(420L), Right(4200L)),
      tags = Seq(Tag(Seq("Guten", "Tag"), "de")),
      revisionDate = Some(revisionDate)
    )

    service.updateArticleFields(existingArticle, partialArticle) should be(updatedArticle)
  }

}
