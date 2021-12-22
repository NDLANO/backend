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
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.validation.{ResourceType, TagAttributes}
import org.joda.time.DateTime
import org.mockito.Mockito._

import scala.util.Success

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val service = new ConverterService
  val contentTitle = ArticleTitle("", "und")
  val author = Author("forfatter", "Henrik")
  val tag = ArticleTag(List("asdf"), "nb")
  val requiredLibrary = RequiredLibrary("", "", "")
  val nodeId = "1234"
  val sampleAlt = "Fotografi"

  test("toApiLicense defaults to unknown if the license was not found") {
    service.toApiLicense("invalid") should equal(api.License("unknown", None, None))
  }

  test("toApiLicense converts a short license string to a license object with description and url") {
    service.toApiLicense("CC-BY-4.0") should equal(
      api.License("CC-BY-4.0",
                  Some("Creative Commons Attribution 4.0 International"),
                  Some("https://creativecommons.org/licenses/by/4.0/")))
  }

  test("toApiArticleV2 converts a domain.Article to an api.ArticleV2") {
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    service.toApiArticleV2(TestData.sampleDomainArticle, "nb") should equal(Success(TestData.apiArticleV2))
  }

  test("that toApiArticleV2 returns sorted supportedLanguages") {
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    val result = service.toApiArticleV2(
      TestData.sampleDomainArticle.copy(title = TestData.sampleDomainArticle.title :+ ArticleTitle("hehe", "und")),
      "nb")
    result.get.supportedLanguages should be(Seq("nb", "und"))
  }

  test("toApiArticleV2 returns None when language is not supported") {
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    service.toApiArticleV2(TestData.sampleDomainArticle, "someRandomLanguage").isFailure should be(true)
    service.toApiArticleV2(TestData.sampleDomainArticle, "").isFailure should be(true)
  }

  test("toApiArticleV2 should always an article if language neutral") {
    val domainArticle = TestData.sampleDomainArticleWithLanguage("und")
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    service.toApiArticleV2(domainArticle, "someRandomLanguage").isSuccess should be(true)
  }

  test(
    "toApiArticleV2 should return Failure if article does not exist on the language asked for and is not language neutral") {
    val domainArticle = TestData.sampleDomainArticleWithLanguage("en")
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    service.toApiArticleV2(domainArticle, "someRandomLanguage").isFailure should be(true)
  }

  test("toApiArticleV2 converts a domain.Article to an api.ArticleV2 with Agreement Copyright") {
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    val from = DateTime.now().minusDays(5).toDate
    val to = DateTime.now().plusDays(10).toDate
    val agreementCopyright = api.Copyright(
      api.License("gnu", Some("gpl"), None),
      "http://tjohei.com/",
      List(),
      List(),
      List(api.Author("Supplier", "Mads LakseService")),
      None,
      Some(from),
      Some(to)
    )
    when(draftApiClient.getAgreementCopyright(1)).thenReturn(Some(agreementCopyright))

    val apiArticle = service.toApiArticleV2(
      TestData.sampleDomainArticle.copy(
        copyright = TestData.sampleDomainArticle.copyright.copy(
          processors = List(Author("Idea", "Kaptein Snabelfant")),
          rightsholders = List(Author("Publisher", "KjeksOgKakerAS")),
          agreementId = Some(1)
        )),
      "nb"
    )

    apiArticle.get.copyright.creators.size should equal(0)
    apiArticle.get.copyright.processors.head.name should equal("Kaptein Snabelfant")
    apiArticle.get.copyright.rightsholders.head.name should equal("Mads LakseService")
    apiArticle.get.copyright.rightsholders.size should equal(1)
    apiArticle.get.copyright.license.license should equal("gnu")
    apiArticle.get.copyright.validFrom.get should equal(from)
    apiArticle.get.copyright.validTo.get should equal(to)
  }

  test("that toApiArticleV2 returns none if article does not exist on language, and fallback is not specified") {
    when(articleRepository.getExternalIdsFromId(TestData.articleId)).thenReturn(List(TestData.externalId))
    val result = service.toApiArticleV2(TestData.sampleDomainArticle, "en")
    result.isFailure should be(true)
  }

  test(
    "That toApiArticleV2 returns article on existing language if fallback is specified even if selected language does not exist") {
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

  test("That mergeLanguageFields returns original list when updated is empty") {
    val existing =
      Seq(ArticleTitle("Tittel 1", "nb"), ArticleTitle("Tittel 2", "nn"), ArticleTitle("Tittel 3", "und"))
    service.mergeLanguageFields(existing, Seq()) should equal(existing)
  }

  test("That mergeLanguageFields updated the english title only when specified") {
    val tittel1 = ArticleTitle("Tittel 1", "nb")
    val tittel2 = ArticleTitle("Tittel 2", "nn")
    val tittel3 = ArticleTitle("Tittel 3", "en")
    val oppdatertTittel3 = ArticleTitle("Title 3 in english", "en")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel3)

    service.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel2, oppdatertTittel3))
  }

  test("That mergeLanguageFields removes a title that is empty") {
    val tittel1 = ArticleTitle("Tittel 1", "nb")
    val tittel2 = ArticleTitle("Tittel 2", "nn")
    val tittel3 = ArticleTitle("Tittel 3", "en")
    val tittelToRemove = ArticleTitle("", "nn")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(tittelToRemove)

    service.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3))
  }

  test("That mergeLanguageFields updates the title with undefined language specified") {
    val tittel1 = ArticleTitle("Tittel 1", "nb")
    val tittel2 = ArticleTitle("Tittel 2", "und")
    val tittel3 = ArticleTitle("Tittel 3", "en")
    val oppdatertTittel2 = ArticleTitle("Tittel 2 er oppdatert", "und")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel2)

    service.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3, oppdatertTittel2))
  }

  test("That mergeLanguageFields also updates the correct content") {
    val desc1 = ArticleContent("Beskrivelse 1", "nb")
    val desc2 = ArticleContent("Beskrivelse 2", "und")
    val desc3 = ArticleContent("Beskrivelse 3", "en")
    val oppdatertDesc2 = ArticleContent("Beskrivelse 2 er oppdatert", "und")

    val existing = Seq(desc1, desc2, desc3)
    val updated = Seq(oppdatertDesc2)

    service.mergeLanguageFields(existing, updated) should equal(Seq(desc1, desc3, oppdatertDesc2))
  }

  test("That hitAsArticleSummaryV2 returns correct summary") {
    val id = 8
    val title = "Baldur har mareritt"
    val visualElement = "image"
    val introduction = "Baldur"
    val metaDescription = "Hurr Durr"
    val metaImageAlt = "Alt text is here"
    val license = "publicdomain"
    val articleType = "topic-article"
    val supportedLanguages = Seq("nb", "en")
    val availability = "everyone"
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

  test("That authors are translated correctly") {
    val authors = List(
      Author("Opphavsmann", "A"),
      Author("Redaksjonelt", "B"),
      Author("redaKsJoNelT", "C"),
      Author("distributør", "D"),
      Author("leVerandør", "E"),
      Author("Språklig", "F")
    )

    val copyright = service.toDomainCopyright("CC-BY-SA-4.0", authors)
    copyright.creators should contain(Author("Originator", "A"))
    copyright.processors should contain(Author("Editorial", "B"))
    copyright.processors should contain(Author("Editorial", "C"))

    copyright.rightsholders should contain(Author("Distributor", "D"))
    copyright.rightsholders should contain(Author("Supplier", "E"))

    copyright.processors should contain(Author("Linguistic", "F"))
  }

  test("That updateExistingTags updates tags correctly") {
    val existingTags = Seq(ArticleTag(Seq("nb-tag1", "nb-tag2"), "nb"), ArticleTag(Seq("Guten", "Tag"), "de"))
    val updatedTags = Seq(ArticleTag(Seq("new-nb-tag1", "new-nb-tag2", "new-nb-tag3"), "nb"),
                          ArticleTag(Seq("new-nn-tag1"), "nn"),
                          ArticleTag(Seq("new-es-tag1", "new-es-tag2"), "es"))
    val expectedTags =
      Seq(ArticleTag(Seq("new-nb-tag1", "new-nb-tag2", "new-nb-tag3"), "nb"), ArticleTag(Seq("Guten", "Tag"), "de"))

    service.updateExistingTagsField(existingTags, updatedTags) should be(expectedTags)
    service.updateExistingTagsField(existingTags, Seq.empty) should be(existingTags)
    service.updateExistingTagsField(Seq.empty, updatedTags) should be(Seq.empty)
  }

  test("That updateExistingArticleMetaDescription updates metaDesc correctly") {
    val existingMetaDesc = Seq(ArticleMetaDescription("nb-content", "nb"), ArticleMetaDescription("en-content", "en"))
    val updatedMetaDesc = Seq(ArticleMetaDescription("new-nb-content", "nb"),
                              ArticleMetaDescription("new-nn-content", "nn"),
                              ArticleMetaDescription("new-es-content", "es"))
    val expectedMetaDesc =
      Seq(ArticleMetaDescription("new-nb-content", "nb"), ArticleMetaDescription("en-content", "en"))

    service.updateExistingMetaDescriptionField(existingMetaDesc, updatedMetaDesc) should be(expectedMetaDesc)
    service.updateExistingMetaDescriptionField(existingMetaDesc, Seq.empty) should be(existingMetaDesc)
    service.updateExistingMetaDescriptionField(Seq.empty, updatedMetaDesc) should be(Seq.empty)
  }

  test("That updateArticleFields updates all fields") {
    val existingArticle = TestData.sampleDomainArticle.copy(
      availability = Availability.student,
      grepCodes = Seq("old", "code"),
      copyright = Copyright("CC-BY-4.0", "origin", Seq(), Seq(), Seq(), None, None, None),
      metaDescription = Seq(ArticleMetaDescription("gammelDesc", "nb")),
      relatedContent = Seq(Left(RelatedContentLink("title1", "url1")), Right(12L)),
      tags = Seq(ArticleTag(Seq("gammel", "Tag"), "nb"))
    )
    val partialArticle =
      api.PartialPublishArticle(
        availability = Some(Availability.teacher),
        grepCodes = Some(Seq("New", "grep", "codes")),
        license = Some("newLicense"),
        metaDescription = Some(Seq(ArticleMetaDescription("nyDesc", "nb"))),
        relatedContent = Some(
          Seq(Left(api.RelatedContentLink("New Title", "New Url")),
              Left(api.RelatedContentLink("Newer Title", "Newer Url")),
              Right(42L))),
        tags = Some(Seq(ArticleTag(Seq("nye", "Tags"), "nb")))
      )
    val updatedArticle = TestData.sampleDomainArticle.copy(
      availability = Availability.teacher,
      grepCodes = Seq("New", "grep", "codes"),
      copyright = Copyright("newLicense", "origin", Seq(), Seq(), Seq(), None, None, None),
      metaDescription = Seq(ArticleMetaDescription("nyDesc", "nb")),
      relatedContent = Seq(Left(RelatedContentLink("New Title", "New Url")),
                           Left(RelatedContentLink("Newer Title", "Newer Url")),
                           Right(42L)),
      tags = Seq(ArticleTag(Seq("nye", "Tags"), "nb"))
    )

    service.updateArticleFields(existingArticle, partialArticle) should be(updatedArticle)

  }

  test("That updateArticleFields does not create new fields") {
    val existingArticle = TestData.sampleDomainArticle.copy(
      availability = Availability.everyone,
      grepCodes = Seq("old", "code"),
      copyright = Copyright("CC-BY-4.0", "origin", Seq(), Seq(), Seq(), None, None, None),
      metaDescription = Seq(ArticleMetaDescription("oldDesc", "de")),
      relatedContent = Seq(Left(RelatedContentLink("title1", "url1")), Left(RelatedContentLink("old title", "old url"))),
      tags = Seq(ArticleTag(Seq("Gluten", "Tag"), "de"))
    )
    val partialArticle =
      api.PartialPublishArticle(
        availability = Some(Availability.student),
        grepCodes = Some(Seq("New", "grep", "codes")),
        license = Some("newLicense"),
        metaDescription = Some(
          Seq(ArticleMetaDescription("nyDesc", "nb"),
              ArticleMetaDescription("newDesc", "en"),
              ArticleMetaDescription("neuDesc", "de"))),
        relatedContent = Some(Seq(Right(42L), Right(420L), Right(4200L))),
        tags = Some(
          Seq(ArticleTag(Seq("nye", "Tags"), "nb"),
              ArticleTag(Seq("new", "Tagss"), "en"),
              ArticleTag(Seq("Guten", "Tag"), "de")))
      )
    val updatedArticle = TestData.sampleDomainArticle.copy(
      availability = Availability.student,
      grepCodes = Seq("New", "grep", "codes"),
      copyright = Copyright("newLicense", "origin", Seq(), Seq(), Seq(), None, None, None),
      metaDescription = Seq(ArticleMetaDescription("neuDesc", "de")),
      relatedContent = Seq(Right(42L), Right(420L), Right(4200L)),
      tags = Seq(ArticleTag(Seq("Guten", "Tag"), "de"))
    )

    service.updateArticleFields(existingArticle, partialArticle) should be(updatedArticle)
  }

}
