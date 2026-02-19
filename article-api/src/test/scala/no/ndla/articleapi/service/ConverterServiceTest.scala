/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.{ImportException, api}
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model
import no.ndla.common.model.{NDLADate, RelatedContentLink, api as commonApi}
import no.ndla.common.model.api.{LicenseDTO, UpdateWith}
import no.ndla.common.model.domain.{
  Author,
  Availability,
  ContributorType,
  Description,
  Introduction,
  RequiredLibrary,
  Tag,
  Title,
  article,
}
import no.ndla.common.model.domain.article.{
  ArticleMetaDescriptionDTO,
  ArticleTagDTO,
  Copyright,
  PartialPublishArticleDTO,
}
import no.ndla.mapping.License

import scala.util.Success

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val service                          = new ConverterService
  val contentTitle: Title              = Title("", "und")
  val author: Author                   = Author(ContributorType.Writer, "Henrik")
  val tag: Tag                         = Tag(List("asdf"), "nb")
  val requiredLibrary: RequiredLibrary = RequiredLibrary("", "", "")
  val nodeId                           = "1234"
  val sampleAlt                        = "Fotografi"

  test("toApiLicense defaults to unknown if the license was not found") {
    service.toApiLicense("invalid") should equal(LicenseDTO("unknown", None, None))
  }

  test("toApiLicense converts a short license string to a license object with description and url") {
    service.toApiLicense("CC-BY-4.0") should equal(
      model
        .api
        .LicenseDTO(
          "CC-BY-4.0",
          Some("Creative Commons Attribution 4.0 International"),
          Some("https://creativecommons.org/licenses/by/4.0/"),
        )
    )
  }

  test("toApiArticleTitle returns both title and htmlTitle") {
    val title = Title("Title with <span data-language=\"uk\">ukrainian</span> word", "en")
    service.toApiArticleTitle(title) should equal(
      api.ArticleTitleDTO(
        "Title with ukrainian word",
        "Title with <span data-language=\"uk\">ukrainian</span> word",
        "en",
      )
    )
  }

  test("toApiArticleIntroduction returns both introduction and htmlIntroduction") {
    val introduction = Introduction("Introduction with <em>emphasis</em>", "en")
    service.toApiArticleIntroduction(introduction) should equal(
      api.ArticleIntroductionDTO("Introduction with emphasis", "Introduction with <em>emphasis</em>", "en")
    )
  }

  test("toApiArticleV2 converts a domain.Article to an api.ArticleV2") {
    service.toApiArticleV2(TestData.sampleDomainArticle, "nb", List(TestData.externalId), false) should equal(
      Success(TestData.apiArticleV2)
    )
  }

  test("that toApiArticleV2 returns sorted supportedLanguages") {
    val result = service.toApiArticleV2(
      TestData.sampleDomainArticle.copy(title = TestData.sampleDomainArticle.title :+ Title("hehe", "und")),
      "nb",
      List(TestData.externalId),
      false,
    )
    result.get.supportedLanguages should be(Seq("nb", "und"))
  }

  test("toApiArticleV2 returns None when language is not supported") {
    service
      .toApiArticleV2(TestData.sampleDomainArticle, "someRandomLanguage", List(TestData.externalId), false)
      .isFailure should be(true)
    service.toApiArticleV2(TestData.sampleDomainArticle, "", List(TestData.externalId), false).isFailure should be(true)
  }

  test("toApiArticleV2 should always an article if language neutral") {
    val domainArticle = TestData.sampleDomainArticleWithLanguage("und")
    service.toApiArticleV2(domainArticle, "someRandomLanguage", List(TestData.externalId), false).isSuccess should be(
      true
    )
  }

  test(
    "toApiArticleV2 should return Failure if article does not exist on the language asked for and is not language neutral"
  ) {
    val domainArticle = TestData.sampleDomainArticleWithLanguage("en")
    service.toApiArticleV2(domainArticle, "someRandomLanguage", List(TestData.externalId), false).isFailure should be(
      true
    )
  }

  test("that toApiArticleV2 returns none if article does not exist on language, and fallback is not specified") {
    val result = service.toApiArticleV2(TestData.sampleDomainArticle, "en", List(TestData.externalId), false)
    result.isFailure should be(true)
  }

  test(
    "That toApiArticleV2 returns article on existing language if fallback is specified even if selected language does not exist"
  ) {
    val result = service.toApiArticleV2(TestData.sampleDomainArticle, "en", List(TestData.externalId), true)
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
    val license            = License.PublicDomain.toString
    val articleType        = "topic-article"
    val supportedLanguages = Seq("nb", "en")
    val availability       = "everyone"
    val hitString          =
      s"""{  "grepCodes": [], "availability": "everyone", "visualElement": {    "en": "$visualElement"  },  "introduction": {    "nb": "$introduction"  }, "metaImage": [{"imageId": "1", "altText": "$metaImageAlt", "language": "nb"}], "tags": {"nb": ["test"]},  "metaDescription": {    "nb": "$metaDescription"  },  "lastUpdated": "2017-12-29T07:18:27Z",  "tags.nb": [    "baldur"  ],  "license": "$license",  "id": $id,  "authors": [],  "content": {    "nb": "Bilde av Baldurs mareritt om Ragnarok."  },  "defaultTitle": "Baldur har mareritt",  "title": {    "nb": "Baldur har mareritt"  },  "articleType": "$articleType", "traits": []}"""

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
    val updatedTags  = Seq(
      Tag(Seq("new-nb-tag1", "new-nb-tag2", "new-nb-tag3"), "nb"),
      Tag(Seq("new-nn-tag1"), "nn"),
      Tag(Seq("new-es-tag1", "new-es-tag2"), "es"),
    )
    val expectedTags = Seq(Tag(Seq("new-nb-tag1", "new-nb-tag2", "new-nb-tag3"), "nb"), Tag(Seq("Guten", "Tag"), "de"))

    service.updateExistingTagsField(existingTags, updatedTags) should be(expectedTags)
    service.updateExistingTagsField(existingTags, Seq.empty) should be(existingTags)
    service.updateExistingTagsField(Seq.empty, updatedTags) should be(Seq.empty)
  }

  test("That updateExistingArticleMetaDescription updates metaDesc correctly") {
    val existingMetaDesc = Seq(Description("nb-content", "nb"), Description("en-content", "en"))
    val updatedMetaDesc  =
      Seq(Description("new-nb-content", "nb"), Description("new-nn-content", "nn"), Description("new-es-content", "es"))
    val expectedMetaDesc = Seq(Description("new-nb-content", "nb"), Description("en-content", "en"))

    service.updateExistingMetaDescriptionField(existingMetaDesc, updatedMetaDesc) should be(expectedMetaDesc)
    service.updateExistingMetaDescriptionField(existingMetaDesc, Seq.empty) should be(existingMetaDesc)
    service.updateExistingMetaDescriptionField(Seq.empty, updatedMetaDesc) should be(Seq.empty)
  }

  test("That updateArticleFields updates all fields") {
    val existingArticle = TestData
      .sampleDomainArticle
      .copy(
        copyright = Copyright("CC-BY-4.0", Some("origin"), Seq(), Seq(), Seq(), None, None, false),
        tags = Seq(Tag(Seq("gammel", "Tag"), "nb")),
        metaDescription = Seq(Description("gammelDesc", "nb")),
        grepCodes = Seq("old", "code"),
        availability = Availability.everyone,
        relatedContent = Seq(Left(RelatedContentLink("title1", "url1")), Right(12L)),
      )

    val revisionDate = NDLADate.now()

    val partialArticle = PartialPublishArticleDTO(
      availability = Some(Availability.teacher),
      grepCodes = Some(Seq("New", "grep", "codes")),
      license = Some("newLicense"),
      metaDescription = Some(Seq(ArticleMetaDescriptionDTO("nyDesc", "nb"))),
      relatedContent = Some(
        Seq(
          Left(commonApi.RelatedContentLinkDTO("New Title", "New Url")),
          Left(commonApi.RelatedContentLinkDTO("Newer Title", "Newer Url")),
          Right(42L),
        )
      ),
      tags = Some(Seq(ArticleTagDTO(Seq("nye", "Tags"), "nb"))),
      revisionDate = UpdateWith(revisionDate),
      published = Some(revisionDate),
    )
    val updatedArticle = TestData
      .sampleDomainArticle
      .copy(
        copyright = Copyright("newLicense", Some("origin"), Seq(), Seq(), Seq(), None, None, false),
        tags = Seq(Tag(Seq("nye", "Tags"), "nb")),
        metaDescription = Seq(Description("nyDesc", "nb")),
        published = revisionDate,
        grepCodes = Seq("New", "grep", "codes"),
        availability = Availability.teacher,
        relatedContent = Seq(
          Left(RelatedContentLink("New Title", "New Url")),
          Left(RelatedContentLink("Newer Title", "Newer Url")),
          Right(42L),
        ),
        revisionDate = Some(revisionDate),
      )

    service.updateArticleFields(existingArticle, partialArticle) should be(updatedArticle)

  }

  test("That updateArticleFields does not create new fields") {
    val existingArticle = TestData
      .sampleDomainArticle
      .copy(
        copyright = Copyright("CC-BY-4.0", Some("origin"), Seq(), Seq(), Seq(), None, None, false),
        tags = Seq(Tag(Seq("Gluten", "Tag"), "de")),
        metaDescription = Seq(Description("oldDesc", "de")),
        grepCodes = Seq("old", "code"),
        availability = Availability.everyone,
        relatedContent =
          Seq(Left(RelatedContentLink("title1", "url1")), Left(RelatedContentLink("old title", "old url"))),
      )

    val revisionDate   = NDLADate.now()
    val partialArticle = article.PartialPublishArticleDTO(
      availability = Some(Availability.teacher),
      grepCodes = Some(Seq("New", "grep", "codes")),
      license = Some("newLicense"),
      metaDescription = Some(
        Seq(
          article.ArticleMetaDescriptionDTO("nyDesc", "nb"),
          article.ArticleMetaDescriptionDTO("newDesc", "en"),
          article.ArticleMetaDescriptionDTO("neuDesc", "de"),
        )
      ),
      relatedContent = Some(Seq(Right(42L), Right(420L), Right(4200L))),
      tags = Some(
        Seq(
          article.ArticleTagDTO(Seq("nye", "Tags"), "nb"),
          article.ArticleTagDTO(Seq("new", "Tagss"), "en"),
          article.ArticleTagDTO(Seq("Guten", "Tag"), "de"),
        )
      ),
      revisionDate = UpdateWith(revisionDate),
      published = Some(revisionDate),
    )
    val updatedArticle = TestData
      .sampleDomainArticle
      .copy(
        copyright = Copyright("newLicense", Some("origin"), Seq(), Seq(), Seq(), None, None, false),
        tags = Seq(Tag(Seq("Guten", "Tag"), "de")),
        metaDescription = Seq(Description("neuDesc", "de")),
        published = revisionDate,
        grepCodes = Seq("New", "grep", "codes"),
        availability = Availability.teacher,
        relatedContent = Seq(Right(42L), Right(420L), Right(4200L)),
        revisionDate = Some(revisionDate),
      )

    service.updateArticleFields(existingArticle, partialArticle) should be(updatedArticle)
  }

}
