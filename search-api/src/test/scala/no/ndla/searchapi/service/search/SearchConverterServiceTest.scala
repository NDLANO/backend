/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.searchapi.caching.Memoize
import no.ndla.searchapi.model.domain.article.{Article, ArticleContent}
import no.ndla.searchapi.model.domain.{Tag, Title}
import no.ndla.searchapi.model.grep.{GrepElement, GrepTitle}
import no.ndla.searchapi.model.search.{
  SearchableArticle,
  SearchableGrepContext,
  SearchableLanguageList,
  SearchableLanguageValues
}
import no.ndla.searchapi.model.taxonomy._
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.invocation.InvocationOnMock

import scala.util.{Success, Try}

class SearchConverterServiceTest extends UnitSuite with TestEnvironment {

  override val searchConverterService = new SearchConverterService
  val sampleArticle: Article = TestData.sampleArticleWithPublicDomain.copy()

  val titles = List(
    Title("Bokmål tittel", "nb"),
    Title("Nynorsk tittel", "nn"),
    Title("English title", "en"),
    Title("Titre francais", "fr"),
    Title("Deutsch titel", "de"),
    Title("Titulo espanol", "es"),
    Title("Nekonata titolo", "und")
  )

  val articles = Seq(
    ArticleContent("Bokmål artikkel", "nb"),
    ArticleContent("Nynorsk artikkel", "nn"),
    ArticleContent("English article", "en"),
    ArticleContent("Francais article", "fr"),
    ArticleContent("Deutsch Artikel", "de"),
    ArticleContent("Articulo espanol", "es"),
    ArticleContent("Nekonata artikolo", "und")
  )

  val articleTags = Seq(
    Tag(Seq("fugl", "fisk"), "nb"),
    Tag(Seq("fugl", "fisk"), "nn"),
    Tag(Seq("bird", "fish"), "en"),
    Tag(Seq("got", "tired"), "fr"),
    Tag(Seq("of", "translating"), "de"),
    Tag(Seq("all", "of"), "es"),
    Tag(Seq("the", "words"), "und")
  )

  val visibleMetadata: Option[Metadata] = Some(Metadata(Seq.empty, visible = true))
  val invisibleMetadata: Option[Metadata] = Some(Metadata(Seq.empty, visible = false))

  val resources = List(
    Resource("urn:resource:1",
             "Resource1",
             Some("urn:article:1"),
             Some("/subject:1/topic:10/resource:1"),
             visibleMetadata))

  val topics = List(
    Topic("urn:topic:10", "Topic1", Some("urn:article:10"), Some("/subject:1/topic:10"), visibleMetadata))

  val topicResourceConnections = List(
    TopicResourceConnection("urn:topic:10",
                            "urn:resource:1",
                            "urn:topic-resource:abc123",
                            primary = true,
                            1,
                            Some("urn:relevance:core")))
  val subject1: TaxSubject = TaxSubject("urn:subject:1", "Subject1", None, Some("/subject:1"), visibleMetadata)
  val subjects = List(subject1)

  val subjectTopicConnections = List(
    SubjectTopicConnection("urn:subject:1",
                           "urn:topic:10",
                           "urn:subject-topic:8180abc",
                           primary = true,
                           1,
                           Some("urn:relevance:core")))

  val emptyBundle: TaxonomyBundle = TaxonomyBundle(
    relevances = List.empty,
    resourceResourceTypeConnections = List.empty,
    resourceTypes = List.empty,
    resources = resources,
    subjectTopicConnections = subjectTopicConnections,
    subjects = subjects,
    topicResourceConnections = topicResourceConnections,
    topicSubtopicConnections = List.empty,
    topics = topics
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(converterService.withAgreementCopyright(any[Article])).thenAnswer((invocation: InvocationOnMock) =>
      invocation.getArgument[Article](0))

    when(taxonomyApiClient.getTaxonomyBundle)
      .thenReturn(new Memoize[Try[TaxonomyBundle]](0, () => Success(emptyBundle)))
  }

  test("That asSearchableArticle converts titles with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = titles)
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, Some(TestData.emptyGrepBundle))
    verifyTitles(searchableArticle)
  }

  test("That asSearchable converts articles with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = articles)
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, Some(TestData.emptyGrepBundle))
    verifyArticles(searchableArticle)
  }

  test("That asSearchable converts tags with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(tags = articleTags)
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, Some(TestData.emptyGrepBundle))
    verifyTags(searchableArticle)
  }

  test("That asSearchable converts all fields with correct language") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = titles, content = articles, tags = articleTags)
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, Some(TestData.emptyGrepBundle))

    verifyTitles(searchableArticle)
    verifyArticles(searchableArticle)
    verifyTags(searchableArticle)
  }

  test("That asSearchableArticle converts titles with license from agreement") {
    val article = TestData.sampleArticleWithByNcSa.copy(title = titles)
    when(converterService.withAgreementCopyright(any[Article]))
      .thenReturn(article.copy(copyright = article.copyright.copy(license = "gnu")))
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, Some(TestData.emptyGrepBundle))
    searchableArticle.license should equal("gnu")
  }

  test("That resource types are derived correctly") {
    val Success(searchable2) =
      searchConverterService.asSearchableArticle(TestData.article2,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable7) =
      searchConverterService.asSearchableArticle(TestData.article7,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))

    searchable2.contexts.head.resourceTypes.map(_.id).sorted should be(
      Seq("urn:resourcetype:subjectMaterial", "urn:resourcetype:academicArticle").sorted)
    searchable4.contexts.head.resourceTypes.map(_.id).sorted should be(Seq("urn:resourcetype:subjectMaterial").sorted)
    searchable7.contexts.head.resourceTypes.map(_.id).sorted should be(
      Seq(
        "urn:resourcetype:nested",
        "urn:resourcetype:peerEvaluation",
        "urn:resourcetype:reviewResource",
        "urn:resourcetype:guidance",
        "urn:resourcetype:subjectMaterial"
      ).sorted)
  }

  test("That breadcrumbs are derived correctly") {
    val Success(searchable1) =
      searchConverterService.asSearchableArticle(TestData.article1,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable6) =
      searchConverterService.asSearchableArticle(TestData.article6,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))

    searchable1.contexts.size should be(2)
    searchable1.contexts.head.breadcrumbs.languageValues.map(_.value) should be(
      Seq(
        Seq(
          "Matte",
          "Baldur har mareritt"
        )))

    searchable1.contexts(1).breadcrumbs.languageValues.map(_.value) should be(
      Seq(
        Seq(
          "Historie",
          "Katter"
        )))

    searchable4.contexts.size should be(1)
    searchable4.contexts.head.breadcrumbs.languageValues.map(_.value) should be(
      Seq(
        Seq(
          "Matte",
          "Baldur har mareritt",
          "En Baldur har mareritt om Ragnarok"
        )))

    searchable6.contexts.size should be(1)
    searchable6.contexts.head.breadcrumbs.languageValues.map(_.value) should be(
      Seq(
        Seq(
          "Historie",
          "Katter"
        )))
  }

  test("That subjects are derived correctly from taxonomy") {
    val Success(searchable1) =
      searchConverterService.asSearchableArticle(TestData.article1,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable5) =
      searchConverterService.asSearchableArticle(TestData.article5,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))

    searchable1.contexts.size should be(2)
    searchable1.contexts.map(_.subject.languageValues.map(_.value)) should be(Seq(Seq("Matte"), Seq("Historie")))

    searchable4.contexts.size should be(1)
    searchable4.contexts.head.subject.languageValues.map(_.value) should be(Seq("Matte"))

    searchable5.contexts.size should be(2)
    searchable5.contexts.map(_.subject.languageValues.map(_.value)) should be(Seq(Seq("Historie"), Seq("Matte")))
  }

  test("That invisible contexts are not indexed") {
    val taxonomyBundleInvisibleMetadata = TestData.taxonomyTestBundle.copy(resources = resources.map(resource =>
      resource.copy(metadata = invisibleMetadata)))
    val Success(searchable1) =
      searchConverterService.asSearchableArticle(TestData.article1,
                                                 taxonomyBundleInvisibleMetadata,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 taxonomyBundleInvisibleMetadata,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable5) =
      searchConverterService.asSearchableArticle(TestData.article5,
                                                 taxonomyBundleInvisibleMetadata,
                                                 Some(TestData.emptyGrepBundle))

    searchable1.contexts.size should be(0)
    searchable4.contexts.size should be(0)
    searchable5.contexts.size should be(0)
  }

  test("That invisible subjects are not indexed") {
    val taxonomyBundleInvisibleMetadata =
      TestData.taxonomyTestBundle.copy(subjects = subjects.map(subject => subject.copy(metadata = invisibleMetadata)))
    val Success(searchable1) =
      searchConverterService.asSearchableArticle(TestData.article1,
                                                 taxonomyBundleInvisibleMetadata,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 taxonomyBundleInvisibleMetadata,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable5) =
      searchConverterService.asSearchableArticle(TestData.article5,
                                                 taxonomyBundleInvisibleMetadata,
                                                 Some(TestData.emptyGrepBundle))

    searchable1.contexts.size should be(0)
    searchable4.contexts.size should be(0)
    searchable5.contexts.size should be(0)
  }

  test("That taxonomy filters are derived correctly") {
    val Success(searchable1) =
      searchConverterService.asSearchableArticle(TestData.article1,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable4) =
      searchConverterService.asSearchableArticle(TestData.article4,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))
    val Success(searchable5) =
      searchConverterService.asSearchableArticle(TestData.article5,
                                                 TestData.taxonomyTestBundle,
                                                 Some(TestData.emptyGrepBundle))

    searchable1.contexts.size should be(2)
    searchable4.contexts.size should be(1)
    searchable5.contexts.size should be(2)
  }

  test("That invisible taxonomy filters are added correctly in drafts") {
    val Success(searchable1) =
      searchConverterService.asSearchableDraft(TestData.draft1,
                                               TestData.taxonomyTestBundle,
                                               Some(TestData.emptyGrepBundle))

    searchable1.contexts.size should be(3)
  }

  test("That asSearchableArticle converts grepContexts correctly based on article grepCodes if grepBundle is empty") {
    val article = TestData.emptyDomainArticle.copy(id = Some(99), grepCodes = Seq("KE12", "KM123", "TT2"))
    val grepContexts = List(SearchableGrepContext("KE12", None),
                            SearchableGrepContext("KM123", None),
                            SearchableGrepContext("TT2", None))
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, Some(TestData.emptyGrepBundle))
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableArticle converts grepContexts correctly based on grepBundle if article has grepCodes") {
    val article = TestData.emptyDomainArticle.copy(id = Some(99), grepCodes = Seq("KE12", "KM123", "TT2"))
    val grepContexts = List(
      SearchableGrepContext("KE12", Some("Utforsking og problemløysing")),
      SearchableGrepContext("KM123", Some("bruke ulike kilder på en kritisk, hensiktsmessig og etterrettelig måte")),
      SearchableGrepContext("TT2", Some("Demokrati og medborgerskap"))
    )
    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, Some(TestData.grepBundle))
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableArticle converts grepContexts correctly based on grepBundle if article has no grepCodes") {
    val article = TestData.emptyDomainArticle.copy(id = Some(99), grepCodes = Seq.empty)
    val grepContexts = List.empty

    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, Some(TestData.grepBundle))
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableDraft converts grepContexts correctly based on draft grepCodes if grepBundle is empty") {
    val draft = TestData.emptyDomainDraft.copy(id = Some(99), grepCodes = Seq("KE12", "KM123", "TT2"))
    val grepContexts = List(SearchableGrepContext("KE12", None),
                            SearchableGrepContext("KM123", None),
                            SearchableGrepContext("TT2", None))
    val Success(searchableArticle) =
      searchConverterService.asSearchableDraft(draft, emptyBundle, Some(TestData.emptyGrepBundle))
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableDraft converts grepContexts correctly based on grepBundle if draft has grepCodes") {
    val draft = TestData.emptyDomainDraft.copy(id = Some(99), grepCodes = Seq("KE12", "KM123", "TT2"))
    val grepBundle = TestData.emptyGrepBundle.copy(
      kjerneelementer = List(GrepElement("KE12", Seq(GrepTitle("default", "tittel12"))),
                             GrepElement("KE34", Seq(GrepTitle("default", "tittel34")))),
      kompetansemaal = List(GrepElement("KM123", Seq(GrepTitle("default", "tittel123")))),
      tverrfagligeTemaer = List(GrepElement("TT2", Seq(GrepTitle("default", "tittel2"))))
    )
    val grepContexts = List(SearchableGrepContext("KE12", Some("tittel12")),
                            SearchableGrepContext("KM123", Some("tittel123")),
                            SearchableGrepContext("TT2", Some("tittel2")))
    val Success(searchableArticle) =
      searchConverterService.asSearchableDraft(draft, emptyBundle, Some(grepBundle))
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableDraft converts grepContexts correctly based on grepBundle if draft has no grepCodes") {
    val draft = TestData.emptyDomainDraft.copy(id = Some(99), grepCodes = Seq.empty)
    val grepBundle = TestData.emptyGrepBundle.copy(
      kjerneelementer = List(GrepElement("KE12", Seq(GrepTitle("default", "tittel12"))),
                             GrepElement("KE34", Seq(GrepTitle("default", "tittel34")))),
      kompetansemaal = List(GrepElement("KM123", Seq(GrepTitle("default", "tittel123")))),
      tverrfagligeTemaer = List(GrepElement("TT2", Seq(GrepTitle("default", "tittel2"))))
    )
    val grepContexts = List.empty

    val Success(searchableArticle) =
      searchConverterService.asSearchableDraft(draft, emptyBundle, Some(grepBundle))
    searchableArticle.grepContexts should equal(grepContexts)
  }

  test("That asSearchableArticle extracts traits correctly") {
    val article =
      TestData.emptyDomainArticle.copy(
        id = Some(99),
        content = Seq(
          ArticleContent("Sjekk denne h5p-en <embed data-resource=\"h5p\" data-path=\"/resource/id\">", "nb"),
          ArticleContent("Fil <embed data-resource=\"file\" data-path=\"/file/path\">", "nn")
        )
      )

    val Success(searchableArticle) =
      searchConverterService.asSearchableArticle(article, emptyBundle, Some(TestData.emptyGrepBundle))
    searchableArticle.traits should equal(List("H5P"))

    val article2 =
      TestData.emptyDomainArticle.copy(
        id = Some(99),
        content = Seq(
          ArticleContent("Skikkelig bra h5p: <embed data-resource=\"h5p\" data-path=\"/resource/id\">", "nb"),
          ArticleContent("Fin video <embed data-resource=\"external\" data-url=\"https://youtu.be/id\">", "nn"),
          ArticleContent(
            "Movie trailer <embed data-resource=\"iframe\" data-url=\"https://www.imdb.com/video/vi3074735641\">",
            "en")
        )
      )

    val Success(searchableArticle2) =
      searchConverterService.asSearchableArticle(article2, emptyBundle, Some(TestData.emptyGrepBundle))
    searchableArticle2.traits should equal(List("H5P", "VIDEO"))
  }

  test("That extracting attributes extracts data-title but not all attributes") {
    val html =
      """<section>Hei<p align="center">Heihei</p><embed class="testklasse" tulleattributt data-resource_id="55" data-title="For ei tittel" />"""
    val result = searchConverterService.getAttributes(html)
    result should be(List("For ei tittel"))
  }

  test("That asSearchableDraft extracts all users from notes correctly") {
    val draft = searchConverterService.asSearchableDraft(TestData.draft5, emptyBundle, Some(TestData.emptyGrepBundle))
    draft.get.users.length should be(2)
    draft.get.users should be(List("ndalId54321", "ndalId12345"))
  }

  private def verifyTitles(searchableArticle: SearchableArticle): Unit = {
    searchableArticle.title.languageValues.size should equal(titles.size)
    languageValueWithLang(searchableArticle.title, "nb") should equal(titleForLang(titles, "nb"))
    languageValueWithLang(searchableArticle.title, "nn") should equal(titleForLang(titles, "nn"))
    languageValueWithLang(searchableArticle.title, "en") should equal(titleForLang(titles, "en"))
    languageValueWithLang(searchableArticle.title, "fr") should equal(titleForLang(titles, "fr"))
    languageValueWithLang(searchableArticle.title, "de") should equal(titleForLang(titles, "de"))
    languageValueWithLang(searchableArticle.title, "es") should equal(titleForLang(titles, "es"))
    languageValueWithLang(searchableArticle.title) should equal(titleForLang(titles))
  }

  private def verifyArticles(searchableArticle: SearchableArticle): Unit = {
    searchableArticle.content.languageValues.size should equal(articles.size)
    languageValueWithLang(searchableArticle.content, "nb") should equal(articleForLang(articles, "nb"))
    languageValueWithLang(searchableArticle.content, "nn") should equal(articleForLang(articles, "nn"))
    languageValueWithLang(searchableArticle.content, "en") should equal(articleForLang(articles, "en"))
    languageValueWithLang(searchableArticle.content, "fr") should equal(articleForLang(articles, "fr"))
    languageValueWithLang(searchableArticle.content, "de") should equal(articleForLang(articles, "de"))
    languageValueWithLang(searchableArticle.content, "es") should equal(articleForLang(articles, "es"))
    languageValueWithLang(searchableArticle.content) should equal(articleForLang(articles))
  }

  private def verifyTags(searchableArticle: SearchableArticle): Unit = {
    languageListWithLang(searchableArticle.tags, "nb") should equal(tagsForLang(articleTags, "nb"))
    languageListWithLang(searchableArticle.tags, "nn") should equal(tagsForLang(articleTags, "nn"))
    languageListWithLang(searchableArticle.tags, "en") should equal(tagsForLang(articleTags, "en"))
    languageListWithLang(searchableArticle.tags, "fr") should equal(tagsForLang(articleTags, "fr"))
    languageListWithLang(searchableArticle.tags, "de") should equal(tagsForLang(articleTags, "de"))
    languageListWithLang(searchableArticle.tags, "es") should equal(tagsForLang(articleTags, "es"))
    languageListWithLang(searchableArticle.tags) should equal(tagsForLang(articleTags))
  }

  private def languageValueWithLang(languageValues: SearchableLanguageValues, lang: String = "und"): String = {
    languageValues.languageValues.find(_.language == lang).get.value
  }

  private def languageListWithLang(languageList: SearchableLanguageList, lang: String = "und"): Seq[String] = {
    languageList.languageValues.find(_.language == lang).get.value
  }

  private def titleForLang(titles: Seq[Title], lang: String = "und"): String = {
    titles.find(_.language == lang).get.title
  }

  private def articleForLang(articles: Seq[ArticleContent], lang: String = "und"): String = {
    articles.find(_.language == lang).get.content
  }

  private def tagsForLang(tags: Seq[Tag], lang: String = "und") = {
    tags.find(_.language == lang).get.tags
  }
}
