/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.DraftApiProperties.resourceHtmlEmbedTag
import no.ndla.draftapi.model.api.NewAgreement
import org.joda.time.{DateTime, DateTimeZone}

object TestData {
  private val publicDomainCopyright= Copyright("publicdomain", "", List(), List(), List(), None, None, None)
  private val byNcSaCopyright = Copyright("by-nc-sa", "Gotham City", List(Author("Forfatter", "DC Comics")), List(), List(), None, None, None)
  private val copyrighted = Copyright("copyrighted", "New York", List(Author("Forfatter", "Clark Kent")), List(), List(), None, None, None)
  private val today = new DateTime().toDate

  private val embedUrl = "http://www.example.org"

  val (articleId, externalId) = (1, "751234")

  val sampleArticleV2 = api.Article(
    id=1,
    oldNdlaUrl = None,
    revision=1,
    title=api.ArticleTitle("title", "nb"),
    content=api.ArticleContentV2("this is content", "nb"),
    copyright = api.Copyright(api.License("licence", None, None), "origin", Seq(api.Author("developer", "Per")), List(), List(), None, None, None),
    tags = api.ArticleTag(Seq("tag"), "nb"),
    requiredLibraries = Seq(api.RequiredLibrary("JS", "JavaScript", "url")),
    visualElement = None,
    introduction = None,
    metaDescription = api.ArticleMetaDescription("metaDesc", "nb"),
    created = new DateTime(2017, 1, 1, 12, 15, 32, DateTimeZone.UTC).toDate,
    updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate,
    updatedBy = "me",
    articleType = "standard",
    supportedLanguages = Seq("nb")
  )

  val articleHit1 = """
                      |{
                      |  "id": "4",
                      |  "title": [
                      |    {
                      |      "title": "8. mars, den internasjonale kvinnedagen",
                      |      "language": "nb"
                      |    },
                      |    {
                      |      "title": "8. mars, den internasjonale kvinnedagen",
                      |      "language": "nn"
                      |    }
                      |  ],
                      |  "introduction": [
                      |    {
                      |      "introduction": "8. mars er den internasjonale kvinnedagen.",
                      |      "language": "nb"
                      |    },
                      |    {
                      |      "introduction": "8. mars er den internasjonale kvinnedagen.",
                      |      "language": "nn"
                      |    }
                      |  ],
                      |  "url": "http://localhost:30002/article-api/v2/articles/4",
                      |  "license": "by-sa",
                      |  "articleType": "standard"
                      |}
                    """.stripMargin

  val apiArticleV2 = api.Article(
    articleId,
    Some(s"//red.ndla.no/node/$externalId"),
    2,
    api.ArticleTitle("title", "nb"),
    api.ArticleContentV2("content", "nb"),
    api.Copyright(api.License("by", Some("Creative Commons Attribution 2.0 Generic"), Some("https://creativecommons.org/licenses/by/2.0/")), "", Seq(), List(), List(), None, None, None),
    api.ArticleTag(Seq(), "nb"),
    Seq(),
    None,
    None,
    api.ArticleMetaDescription("meta description", "nb"),
    today,
    today,
    "ndalId54321",
    "standard",
    Seq("nb")
  )


  val requestNewArticleV2Body = """
                                  |{
                                  |  "copyright": {
                                  |    "license": {
                                  |      "license": "by-sa",
                                  |      "description": "something"
                                  |    },
                                  |    "origin": "fromSomeWhere",
                                  |    "authors": [
                                  |      {
                                  |        "type": "string",
                                  |        "name": "Christian P"
                                  |      }
                                  |    ]
                                  |  },
                                  |  "language": "nb",
                                  |  "visualElement": "string",
                                  |  "introduction": "string",
                                  |  "metaDescription": "string",
                                  |  "tags": [
                                  |	    "string"
                                  |	  ],
                                  |  "content": "string",
                                  |  "footNotes": [ "string " ],
                                  |  "title": "string",
                                  |  "articleType": "standard",
                                  |  "metaImageId": "22",
                                  |  "requiredLibraries": [
                                  |    {
                                  |      "mediaType": "string",
                                  |      "name": "string"
                                  |    }
                                  |  ]
                                  |}
                                """.stripMargin

  val sampleArticleWithPublicDomain = Article(
    Option(1),
    Option(1),
    Seq(ArticleTitle("test", "en")),
    Seq(ArticleContent("<section><div>test</div></section>", "en")),
    publicDomainCopyright,
    Seq(),
    Seq(),
    Seq(VisualElement("image", "en")),
    Seq(ArticleIntroduction("This is an introduction", "en")),
    Seq(),
    None,
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    "ndalId54321",
    ArticleType.Standard.toString)

  val sampleDomainArticle = Article(
    Option(articleId),
    Option(2),
    Seq(ArticleTitle("title", "nb")),
    Seq(ArticleContent("content", "nb")),
    Copyright("by", "", Seq(), List(), List(), None, None, None),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(ArticleMetaDescription("meta description", "nb")),
    None,
    today,
    today,
    "ndalId54321",
    ArticleType.Standard.toString
  )

  val sampleDomainArticle2 = Article(
    None,
    None,
    Seq(ArticleTitle("test", "en")),
    Seq(ArticleContent("<article><div>test</div></article>", "en")),
    Copyright("publicdomain", "", Seq(), List(), List(), None, None, None),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    None,
    today,
    today,
    "ndalId54321",
    ArticleType.Standard.toString
  )

  val newArticleV2 = api.NewArticle(
    "test",
    "<article><div>test</div></article>",
    Seq(),
    None,
    None,
    None,
    None,
    api.Copyright(api.License("publicdomain", None, None), "", Seq(), List(), List(), None, None, None),
    None,
    "standard",
    "en"
  )

  val sampleArticleWithByNcSa = sampleArticleWithPublicDomain.copy(copyright=byNcSaCopyright)
  val sampleArticleWithCopyrighted = sampleArticleWithPublicDomain.copy(copyright=copyrighted )

  val sampleDomainArticleWithHtmlFault = Article(
    Option(articleId),
    Option(2),
    Seq(ArticleTitle("test", "en")),
    Seq(ArticleContent(
    """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
      |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
      |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
      |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
    """.stripMargin, "en")),
    Copyright("publicdomain", "", Seq(), List(), List(), None, None, None),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(ArticleMetaDescription("meta description", "nb")),
    None,
    today,
    today,
    "ndalId54321",
    ArticleType.Standard.toString
  )

  val apiArticleWithHtmlFaultV2 = api.Article(
    1,
    None,
    1,
    api.ArticleTitle("test", "en"),
    api.ArticleContentV2(
      """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
        |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
        |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
        |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin, "en"),
    api.Copyright(api.License("publicdomain", None, None), "", Seq(), List(), List(), None, None, None),
    api.ArticleTag(Seq.empty, "en"),
    Seq.empty,
    None,
    None,
    api.ArticleMetaDescription("so meta", "en"),
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    "ndalId54321",
    "standard",
    Seq("en")
  )

  val (nodeId, nodeId2) = ("1234", "4321")
  val sampleTitle = ArticleTitle("title", "en")

  val visualElement = VisualElement(s"""<$resourceHtmlEmbedTag  data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />""", "nb")

  val sampleConcept = Concept(
    Some(1),
    Seq(ConceptTitle("Tittel for begrep", "nb")),
    Seq(ConceptContent("Innhold for begrep", "nb")),
    Some(Copyright("publicdomain", "", Seq(), List(), List(), None, None, None)),
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate
  )

  val sampleApiConcept = api.Concept(
    1,
    api.ConceptTitle("Tittel for begrep", "nb"),
    api.ConceptContent("Innhold for begrep", "nb"),
    Some(api.Copyright(api.License("publicdomain", None, None), "", Seq(), List(), List(), None, None, None)),
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    Set("nb")
  )

  val sampleApiAgreement = api.Agreement(
    1,
    "title",
    "content",
    api.Copyright(api.License("publicdomain", None, None), "", Seq(), List(), List(), None, None, None),
    DateTime.now().minusDays(4).toDate,
    DateTime.now().minusDays(2).toDate,
    "ndalId54321"
  )

  val sampleDomainAgreement = Agreement(
    id = Some(1),
    title = "tittledur",
    content = "contentur",
    copyright = byNcSaCopyright,
    created = DateTime.now().minusDays(4).toDate,
    updated = DateTime.now().minusDays(2).toDate,
    updatedBy = "ndalId54321"
  )

  val newAgreement = NewAgreement("newTitle", "newString", api.Copyright(api.License("by-sa", None, None), "", List(),List(),List(), None, None, None))

}


