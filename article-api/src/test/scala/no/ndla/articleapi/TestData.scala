/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.domain._
import no.ndla.mapping.License
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag

import java.time.LocalDateTime

trait TestData {
  this: Props =>

  class TestData {
    private val publicDomainCopyright =
      Copyright(License.PublicDomain.toString, "", List(), List(), List(), None, None, None)
    private val byNcSaCopyright =
      Copyright(
        License.CC_BY_NC_SA.toString,
        "Gotham City",
        List(Author("Writer", "DC Comics")),
        List(),
        List(),
        None,
        None,
        None
      )
    private val copyrighted =
      Copyright(
        License.Copyrighted.toString,
        "New York",
        List(Author("Writer", "Clark Kent")),
        List(),
        List(),
        None,
        None,
        None
      )
    private val today = LocalDateTime.now().withNano(0)

    val (articleId, externalId) = (1, "751234")

    val sampleArticleV2 = api.ArticleV2(
      id = 1,
      oldNdlaUrl = None,
      revision = 1,
      title = api.ArticleTitle("title", "nb"),
      content = api.ArticleContentV2("this is content", "nb"),
      copyright = api.Copyright(
        api.License("licence", None, None),
        "origin",
        Seq(api.Author("developer", "Per")),
        List(),
        List(),
        None,
        None,
        None
      ),
      tags = api.ArticleTag(Seq("tag"), "nb"),
      requiredLibraries = Seq(api.RequiredLibrary("JS", "JavaScript", "url")),
      visualElement = None,
      metaImage = None,
      introduction = None,
      metaDescription = api.ArticleMetaDescription("metaDesc", "nb"),
      created = LocalDateTime.of(2017, 1, 1, 12, 15, 32),
      updated = LocalDateTime.of(2017, 4, 1, 12, 15, 32),
      updatedBy = "me",
      published = LocalDateTime.of(2017, 4, 1, 12, 15, 32),
      articleType = "standard",
      supportedLanguages = Seq("nb"),
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = Availability.everyone.toString,
      relatedContent = Seq.empty,
      revisionDate = None
    )

    val apiArticleV2 = api.ArticleV2(
      articleId,
      Some(s"//red.ndla.no/node/$externalId"),
      2,
      api.ArticleTitle("title", "nb"),
      api.ArticleContentV2("content", "nb"),
      api.Copyright(
        api.License(
          "CC-BY-4.0",
          Some("Creative Commons Attribution 4.0 International"),
          Some("https://creativecommons.org/licenses/by/4.0/")
        ),
        "",
        List(),
        List(),
        List(),
        None,
        None,
        None
      ),
      api.ArticleTag(Seq("tag"), "nb"),
      Seq(),
      None,
      Some(api.ArticleMetaImage(s"http://api-gateway.ndla-local/image-api/raw/id/11", "alt", "nb")),
      None,
      api.ArticleMetaDescription("meta description", "nb"),
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
      revisionDate = None
    )

    val sampleArticleWithPublicDomain = Article(
      Option(1),
      Option(1),
      Seq(ArticleTitle("test", "en")),
      Seq(ArticleContent("<section><div>test</div></section>", "en")),
      publicDomainCopyright,
      Seq(ArticleTag(Seq("a", "b", "c"), "en")),
      Seq(),
      Seq(VisualElement("image", "en")),
      Seq(ArticleIntroduction("This is an introduction", "en")),
      Seq(ArticleMetaDescription("meta", "en")),
      Seq.empty,
      LocalDateTime.now().minusDays(4).withNano(0),
      LocalDateTime.now().minusDays(2).withNano(0),
      "ndalId54321",
      LocalDateTime.now().minusDays(2).withNano(0),
      ArticleType.Standard.toString,
      Seq("COMPCODE1"),
      Seq(1),
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionDate = Some(LocalDateTime.now().withNano(0))
    )

    val sampleDomainArticle = Article(
      Option(articleId),
      Option(2),
      Seq(ArticleTitle("title", "nb")),
      Seq(ArticleContent("content", "nb")),
      Copyright("CC-BY-4.0", "", Seq(), Seq(), Seq(), None, None, None),
      Seq(ArticleTag(Seq("tag"), "nb")),
      Seq(),
      Seq(),
      Seq(),
      Seq(ArticleMetaDescription("meta description", "nb")),
      Seq(ArticleMetaImage("11", "alt", "nb")),
      today,
      today,
      "ndalId54321",
      today,
      ArticleType.Standard.toString,
      Seq("COMPCODE1"),
      Seq(1),
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionDate = None
    )

    val sampleDomainArticle2 = Article(
      None,
      None,
      Seq(ArticleTitle("test", "en")),
      Seq(ArticleContent("<article><div>test</div></article>", "en")),
      Copyright("publicdomain", "", Seq(), Seq(), Seq(), None, None, None),
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
      ArticleType.Standard.toString,
      Seq("COMPCODE1"),
      Seq(1),
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionDate = None
    )

    val sampleArticleWithByNcSa: Article      = sampleArticleWithPublicDomain.copy(copyright = byNcSaCopyright)
    val sampleArticleWithCopyrighted: Article = sampleArticleWithPublicDomain.copy(copyright = copyrighted)

    val sampleDomainArticleWithHtmlFault = Article(
      Option(articleId),
      Option(2),
      Seq(ArticleTitle("test", "en")),
      Seq(
        ArticleContent(
          """<ul><li><h1>Det er ikke lov ?? gj??re dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
        |<ul><li><h2>Det er ikke lov ?? gj??re dette.</h2></li><li>Dette er helt ok</li></ul>
        |<ol><li><h3>Det er ikke lov ?? gj??re dette.</h3></li><li>Dette er helt ok</li></ol>
        |<ol><li><h4>Det er ikke lov ?? gj??re dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin,
          "en"
        )
      ),
      Copyright("publicdomain", "", Seq(), Seq(), Seq(), None, None, None),
      Seq(),
      Seq(),
      Seq(),
      Seq(),
      Seq(ArticleMetaDescription("meta description", "nb")),
      Seq.empty,
      today,
      today,
      "ndalId54321",
      today,
      ArticleType.Standard.toString,
      Seq(),
      Seq(),
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionDate = None
    )

    val apiArticleWithHtmlFaultV2 = api.ArticleV2(
      1,
      None,
      1,
      api.ArticleTitle("test", "en"),
      api.ArticleContentV2(
        """<ul><li><h1>Det er ikke lov ?? gj??re dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
        |<ul><li><h2>Det er ikke lov ?? gj??re dette.</h2></li><li>Dette er helt ok</li></ul>
        |<ol><li><h3>Det er ikke lov ?? gj??re dette.</h3></li><li>Dette er helt ok</li></ol>
        |<ol><li><h4>Det er ikke lov ?? gj??re dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin,
        "en"
      ),
      api.Copyright(api.License("publicdomain", None, None), "", Seq(), Seq(), Seq(), None, None, None),
      api.ArticleTag(Seq.empty, "en"),
      Seq.empty,
      None,
      None,
      None,
      api.ArticleMetaDescription("so meta", "en"),
      LocalDateTime.now().minusDays(4),
      LocalDateTime.now().minusDays(2),
      "ndalId54321",
      LocalDateTime.now().minusDays(2),
      "standard",
      Seq("en"),
      Seq.empty,
      Seq.empty,
      availability = Availability.everyone.toString,
      relatedContent = Seq.empty,
      revisionDate = None
    )

    val (nodeId, nodeId2) = ("1234", "4321")
    val sampleTitle       = ArticleTitle("title", "en")

    val visualElement = VisualElement(
      s"""<$ResourceHtmlEmbedTag  data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />""",
      "nb"
    )

    def sampleDomainArticleWithLanguage(lang: String): Article = {
      Article(
        Option(articleId),
        Option(2),
        Seq(ArticleTitle("title", lang)),
        Seq(ArticleContent("content", lang)),
        Copyright("by", "", Seq(), Seq(), Seq(), None, None, None),
        Seq(),
        Seq(),
        Seq(),
        Seq(),
        Seq(ArticleMetaDescription("meta description", lang)),
        Seq(ArticleMetaImage("11", "alt", lang)),
        today,
        today,
        "ndalId54321",
        today,
        ArticleType.Standard.toString,
        Seq(),
        Seq(),
        availability = Availability.everyone,
        relatedContent = Seq.empty,
        revisionDate = None
      )
    }

    val sampleApiTagsSearchResult = api.TagsSearchResult(10, 1, 1, "nb", Seq("a", "b"))

    val testSettings = SearchSettings(
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
