/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.domain.*
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.article.{Article, Copyright}
import no.ndla.common.model.domain.*
import no.ndla.mapping.License

trait TestData {
  this: Props =>

  class TestData {
    private val publicDomainCopyright =
      Copyright(License.PublicDomain.toString, None, List(), List(), List(), None, None, false)
    private val byNcSaCopyright =
      Copyright(
        License.CC_BY_NC_SA.toString,
        Some("Gotham City"),
        List(Author("Writer", "DC Comics")),
        List(),
        List(),
        None,
        None,
        false
      )
    private val copyrighted =
      Copyright(
        License.Copyrighted.toString,
        Some("New York"),
        List(Author("Writer", "Clark Kent")),
        List(),
        List(),
        None,
        None,
        false
      )
    private val today = NDLADate.now().withNano(0)

    val (articleId, externalId) = (1L, "751234")

    val sampleArticleV2: api.ArticleV2DTO = api.ArticleV2DTO(
      id = 1,
      oldNdlaUrl = None,
      revision = 1,
      title = api.ArticleTitleDTO("title", "title", "nb"),
      content = api.ArticleContentV2DTO("this is content", "nb"),
      copyright = model.api.CopyrightDTO(
        model.api.LicenseDTO("licence", None, None),
        Some("origin"),
        Seq(model.api.AuthorDTO("developer", "Per")),
        List(),
        List(),
        None,
        None,
        false
      ),
      tags = api.ArticleTagDTO(Seq("tag"), "nb"),
      requiredLibraries = Seq(api.RequiredLibraryDTO("JS", "JavaScript", "url")),
      visualElement = None,
      metaImage = None,
      introduction = None,
      metaDescription = api.ArticleMetaDescriptionDTO("metaDesc", "nb"),
      created = NDLADate.of(2017, 1, 1, 12, 15, 32),
      updated = NDLADate.of(2017, 4, 1, 12, 15, 32),
      updatedBy = "me",
      published = NDLADate.of(2017, 4, 1, 12, 15, 32),
      articleType = "standard",
      supportedLanguages = Seq("nb"),
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = Availability.everyone.toString,
      relatedContent = Seq.empty,
      revisionDate = None,
      slug = None
    )

    val apiArticleV2: api.ArticleV2DTO = api.ArticleV2DTO(
      articleId,
      Some(s"//red.ndla.no/node/$externalId"),
      2,
      api.ArticleTitleDTO("title", "title", "nb"),
      api.ArticleContentV2DTO("content", "nb"),
      model.api.CopyrightDTO(
        model.api.LicenseDTO(
          "CC-BY-4.0",
          Some("Creative Commons Attribution 4.0 International"),
          Some("https://creativecommons.org/licenses/by/4.0/")
        ),
        None,
        List(),
        List(),
        List(),
        None,
        None,
        false
      ),
      api.ArticleTagDTO(Seq("tag"), "nb"),
      Seq(),
      None,
      Some(api.ArticleMetaImageDTO(s"http://api-gateway.ndla-local/image-api/raw/id/11", "alt", "nb")),
      None,
      api.ArticleMetaDescriptionDTO("meta description", "nb"),
      today,
      today,
      "ndalId54321",
      today,
      "standard",
      Seq("nb"),
      Seq("COMPCODE1"),
      Seq(1),
      availability = Availability.everyone.toString,
      relatedContent = Seq.empty,
      revisionDate = None,
      slug = None
    )

    val sampleArticleWithPublicDomain: Article = Article(
      Option(1),
      Option(1),
      Seq(Title("test", "en")),
      Seq(ArticleContent("<section><div>test</div></section>", "en")),
      publicDomainCopyright,
      Seq(Tag(Seq("a", "b", "c"), "en")),
      Seq(),
      Seq(
        VisualElement(
          "<ndlaembed data-resource=\"image\" data-resource_id=\"1\" data-size=\"large\" data-align=\"center\" data-alt=\"alt\" />",
          "en"
        )
      ),
      Seq(Introduction("This is an introduction", "en")),
      Seq(Description("meta", "en")),
      Seq.empty,
      created = NDLADate.now().minusDays(4).withNano(0),
      updated = NDLADate.now().minusDays(2).withNano(0),
      "ndalId54321",
      published = NDLADate.now().minusDays(2).withNano(0),
      ArticleType.Standard,
      Seq("COMPCODE1"),
      Seq(1),
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionDate = Some(NDLADate.now().withNano(0)),
      slug = None,
      disclaimer = None
    )

    val sampleDomainArticle: Article = Article(
      Option(articleId),
      Option(2),
      Seq(Title("title", "nb")),
      Seq(ArticleContent("content", "nb")),
      Copyright("CC-BY-4.0", None, Seq(), Seq(), Seq(), None, None, false),
      Seq(Tag(Seq("tag"), "nb")),
      Seq(),
      Seq(),
      Seq(),
      Seq(Description("meta description", "nb")),
      Seq(ArticleMetaImage("11", "alt", "nb")),
      today,
      today,
      "ndalId54321",
      today,
      ArticleType.Standard,
      Seq("COMPCODE1"),
      Seq(1),
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionDate = None,
      slug = None,
      disclaimer = None
    )

    val sampleDomainArticle2: Article = Article(
      None,
      None,
      Seq(Title("test", "en")),
      Seq(ArticleContent("<article><div>test</div></article>", "en")),
      Copyright("publicdomain", None, Seq(), Seq(), Seq(), None, None, false),
      Seq(),
      Seq(),
      Seq(),
      Seq(),
      Seq(),
      Seq(),
      today,
      today,
      "ndalId54321",
      today,
      ArticleType.Standard,
      Seq("COMPCODE1"),
      Seq(1),
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionDate = None,
      slug = None,
      disclaimer = None
    )

    val sampleArticleWithByNcSa: Article      = sampleArticleWithPublicDomain.copy(copyright = byNcSaCopyright)
    val sampleArticleWithCopyrighted: Article = sampleArticleWithPublicDomain.copy(copyright = copyrighted)

    val sampleDomainArticleWithHtmlFault: Article = Article(
      Option(articleId),
      Option(2),
      Seq(Title("test", "en")),
      Seq(
        ArticleContent(
          """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
        |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
        |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
        |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin,
          "en"
        )
      ),
      Copyright("publicdomain", None, Seq(), Seq(), Seq(), None, None, false),
      Seq(),
      Seq(),
      Seq(),
      Seq(),
      Seq(Description("meta description", "nb")),
      Seq.empty,
      today,
      today,
      "ndalId54321",
      today,
      ArticleType.Standard,
      Seq(),
      Seq(),
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionDate = None,
      slug = None,
      disclaimer = None
    )

    val apiArticleWithHtmlFaultV2: api.ArticleV2DTO = api.ArticleV2DTO(
      1,
      None,
      1,
      api.ArticleTitleDTO("test", "test", "en"),
      api.ArticleContentV2DTO(
        """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
        |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
        |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
        |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin,
        "en"
      ),
      model.api
        .CopyrightDTO(model.api.LicenseDTO("publicdomain", None, None), None, Seq(), Seq(), Seq(), None, None, false),
      api.ArticleTagDTO(Seq.empty, "en"),
      Seq.empty,
      None,
      None,
      None,
      api.ArticleMetaDescriptionDTO("so meta", "en"),
      NDLADate.now().minusDays(4),
      NDLADate.now().minusDays(2),
      "ndalId54321",
      NDLADate.now().minusDays(2),
      "standard",
      Seq("en"),
      Seq.empty,
      Seq.empty,
      availability = Availability.everyone.toString,
      relatedContent = Seq.empty,
      revisionDate = None,
      slug = None
    )

    val (nodeId, nodeId2)  = ("1234", "4321")
    val sampleTitle: Title = Title("title", "en")

    val visualElement: VisualElement = VisualElement(
      s"""<$EmbedTagName  data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />""",
      "nb"
    )

    def sampleDomainArticleWithLanguage(lang: String): Article = {
      Article(
        Option(articleId),
        Option(2),
        Seq(Title("title", lang)),
        Seq(ArticleContent("content", lang)),
        Copyright("by", None, Seq(), Seq(), Seq(), None, None, false),
        Seq(),
        Seq(),
        Seq(),
        Seq(),
        Seq(Description("meta description", lang)),
        Seq(ArticleMetaImage("11", "alt", lang)),
        today,
        today,
        "ndalId54321",
        today,
        ArticleType.Standard,
        Seq(),
        Seq(),
        availability = Availability.everyone,
        relatedContent = Seq.empty,
        revisionDate = None,
        slug = None,
        disclaimer = None
      )
    }

    val sampleApiTagsSearchResult: api.TagsSearchResultDTO = api.TagsSearchResultDTO(10, 1, 1, "nb", Seq("a", "b"))

    val testSettings: SearchSettings = SearchSettings(
      query = None,
      withIdIn = List(),
      language = "nb",
      license = None,
      page = 1,
      pageSize = 10,
      sort = Sort.ByIdAsc,
      articleTypes = Seq.empty,
      fallback = false,
      grepCodes = Seq.empty,
      shouldScroll = false,
      availability = Seq.empty
    )

  }
}
