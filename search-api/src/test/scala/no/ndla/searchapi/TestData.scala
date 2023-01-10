/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.domain.{
  ArticleContent,
  ArticleMetaImage,
  ArticleType,
  Author,
  Availability,
  EditorNote,
  Introduction,
  Status,
  Tag,
  Title,
  VisualElement,
  draft
}
import no.ndla.common.model.domain.article.{Article, Copyright}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.{domain => common}
import no.ndla.language.Language.DefaultLanguage
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.domain._
import no.ndla.searchapi.model.domain.learningpath.{LearningPath, LearningPathStatus, LearningPathVerificationStatus}
import no.ndla.searchapi.model.grep.{GrepBundle, GrepElement, GrepTitle}
import no.ndla.searchapi.model.search._
import no.ndla.searchapi.model.search.settings.{MultiDraftSearchSettings, SearchSettings}
import no.ndla.searchapi.model.taxonomy._

import java.time.LocalDateTime

object TestData {

  private val publicDomainCopyright = Copyright("publicdomain", "", List(), List(), List(), None, None, None)
  private val byNcSaCopyright =
    Copyright("by-nc-sa", "Gotham City", List(Author("Writer", "DC Comics")), List(), List(), None, None, None)
  private val copyrighted =
    Copyright("copyrighted", "New York", List(Author("Writer", "Clark Kent")), List(), List(), None, None, None)
  val today: LocalDateTime = LocalDateTime.now().withNano(0)

  val sampleArticleTitle         = ArticleApiTitle("tittell", "nb")
  val sampleArticleVisualElement = ArticleApiVisualElement(s"""<$EmbedTagName data-resource="image">""", "nb")
  val sampleArticleIntro         = ArticleApiIntro("intro", "nb")

  val sampleArticleSearch = ArticleApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      ArticleApiSearchResult(
        1,
        sampleArticleTitle,
        Option(sampleArticleVisualElement),
        Option(sampleArticleIntro),
        "http://articles/1",
        "by",
        "standard",
        Seq("nb", "en")
      ),
      ArticleApiSearchResult(
        2,
        ArticleApiTitle("Another title", "nb"),
        Option(sampleArticleVisualElement),
        Option(sampleArticleIntro),
        "http://articles/2",
        "by",
        "standard",
        Seq("nb", "en")
      )
    )
  )

  val sampleImageSearch = ImageApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      ImageApiSearchResult(
        "1",
        ImageTitle("title", "en"),
        ImageAltText("alt text", "en"),
        "http://images/1.jpg",
        "http://images/1",
        "by",
        Seq("en")
      ),
      ImageApiSearchResult(
        "1",
        ImageTitle("title", "en"),
        ImageAltText("alt text", "en"),
        "http://images/1.jpg",
        "http://images/1",
        "by",
        Seq("en")
      )
    )
  )

  val sampleLearningpath = LearningpathApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      LearningpathApiSearchResult(
        1,
        LearningpathApiTitle("en title", "nb"),
        LearningpathApiDescription("en description", "nb"),
        LearningpathApiIntro("intro", "nb"),
        "http://learningpath/1",
        None,
        None,
        "PUBLISHED",
        "2016-07-06T09:08:08Z",
        LearningPathApiTags(Seq(), "nb"),
        Seq("nb"),
        None
      ),
      LearningpathApiSearchResult(
        2,
        LearningpathApiTitle("en annen titlel", "nb"),
        LearningpathApiDescription("beskrivelse", "nb"),
        LearningpathApiIntro("itroduksjon", "nb"),
        "http://learningpath/2",
        None,
        None,
        "PUBLISHED",
        "2016-07-06T09:08:08Z",
        LearningPathApiTags(Seq(), "nb"),
        Seq("nb"),
        None
      )
    )
  )

  val sampleAudio = AudioApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      AudioApiSearchResult(1, AudioApiTitle("en title", "nb"), "http://audio/1", "by", Seq("nb")),
      AudioApiSearchResult(2, AudioApiTitle("ny tlttle", "nb"), "http://audio/2", "by", Seq("nb"))
    )
  )

  val (articleId, externalId) = (1, "751234")

  val sampleArticleWithPublicDomain = Article(
    Option(1),
    Option(1),
    Seq(Title("test", "en")),
    Seq(ArticleContent("<section><div>test</div></section>", "en")),
    publicDomainCopyright,
    Seq(),
    Seq(),
    Seq(VisualElement("image", "en")),
    Seq(Introduction("This is an introduction", "en")),
    Seq(common.Description("meta", "en")),
    Seq(),
    today.minusDays(4),
    today.minusDays(2),
    "ndalId54321",
    today.minusDays(2),
    ArticleType.Standard,
    Seq.empty,
    Seq.empty,
    Availability.everyone,
    Seq.empty,
    None,
    slug = None
  )

  val sampleDomainArticle = Article(
    Option(articleId),
    Option(2),
    Seq(Title("title", "nb")),
    Seq(ArticleContent("content", "nb")),
    Copyright("by", "", Seq(), Seq(), Seq(), None, None, None),
    Seq(Tag(Seq("tag"), "nb")),
    Seq(),
    Seq(),
    Seq(),
    Seq(common.Description("meta description", "nb")),
    Seq(ArticleMetaImage("11", "alt", "nb")),
    today,
    today,
    "ndalId54321",
    today,
    ArticleType.Standard,
    Seq.empty,
    Seq.empty,
    Availability.everyone,
    Seq.empty,
    None,
    slug = None
  )

  val sampleDomainArticle2 = Article(
    None,
    None,
    Seq(Title("test", "en")),
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
    ArticleType.Standard,
    Seq.empty,
    Seq.empty,
    Availability.everyone,
    Seq.empty,
    None,
    slug = None
  )

  val sampleArticleWithByNcSa: Article =
    sampleArticleWithPublicDomain.copy(copyright = byNcSaCopyright, published = LocalDateTime.now())

  val sampleArticleWithCopyrighted: Article =
    sampleArticleWithPublicDomain.copy(copyright = copyrighted, published = LocalDateTime.now())

  val article1: Article = TestData.sampleArticleWithByNcSa.copy(
    id = Option(1),
    title = List(Title("Batmen er på vift med en bil", "nb")),
    content = List(
      ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")
    ),
    copyright = byNcSaCopyright.copy(creators = List(Author("Forfatter", "Kjekspolitiet"))),
    tags = List(Tag(List("fugl"), "nb")),
    visualElement = List.empty,
    introduction = List(Introduction("Batmen", "nb")),
    metaDescription = List.empty,
    created = today.minusDays(4),
    updated = today.minusDays(3),
    published = today.minusDays(3),
    grepCodes = Seq("KM123", "KE12")
  )

  val article2: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(2),
    title = List(Title("Pingvinen er ute og går", "nb")),
    content = List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb")),
    copyright = publicDomainCopyright
      .copy(creators = List(Author("Forfatter", "Pjolter")), processors = List(Author("Editorial", "Svims"))),
    tags = List(Tag(List("fugl"), "nb")),
    visualElement = List.empty,
    introduction = List(Introduction("Pingvinen", "nb")),
    metaDescription = List.empty,
    created = today.minusDays(4),
    updated = today.minusDays(2),
    published = today.minusDays(2),
    grepCodes = Seq("KE34", "KM123")
  )

  val article3: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(3),
    title = List(Title("Donald Duck kjører bil", "nb")),
    content = List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb")),
    tags = List(Tag(List("and"), "nb")),
    visualElement = List.empty,
    introduction = List(Introduction("Donald Duck", "nb")),
    metaDescription = List.empty,
    created = today.minusDays(4),
    updated = today.minusDays(1),
    published = today.minusDays(1),
    grepCodes = Seq("TT2", "KM123")
  )

  val article4: Article = TestData.sampleArticleWithCopyrighted.copy(
    id = Option(4),
    title = List(Title("Superman er ute og flyr", "nb")),
    content =
      List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb")),
    tags = List(Tag(List("supermann"), "nb")),
    visualElement = List.empty,
    introduction = List(Introduction("Superman", "nb")),
    metaDescription = List.empty,
    created = today.minusDays(4),
    updated = today,
    published = today
  )

  val article5: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(5),
    title = List(Title("Hulken løfter biler", "nb")),
    content = List(ArticleContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb")),
    tags = List(Tag(List("hulk"), "nb")),
    visualElement = List.empty,
    introduction = List(Introduction("Hulken", "nb")),
    metaDescription = List.empty,
    created = today.minusDays(40),
    updated = today.minusDays(35),
    published = today.minusDays(35),
    grepCodes = Seq("KE12", "TT2")
  )

  val article6: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(6),
    title = List(Title("Loke og Tor prøver å fange midgaardsormen", "nb")),
    content = List(
      ArticleContent(
        "<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>",
        "nb"
      )
    ),
    tags = List(Tag(List("Loke", "Tor", "Naglfar"), "nb")),
    visualElement = List.empty,
    introduction = List(Introduction("Loke og Tor", "nb")),
    metaDescription = List.empty,
    created = today.minusDays(30),
    updated = today.minusDays(25),
    published = today.minusDays(25)
  )

  val article7: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(7),
    title = List(Title("Yggdrasil livets tre", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb")),
    tags = List(Tag(List("yggdrasil"), "nb")),
    visualElement = List.empty,
    introduction = List(Introduction("Yggdrasil", "nb")),
    metaDescription = List.empty,
    created = today.minusDays(20),
    updated = today.minusDays(15),
    published = today.minusDays(15)
  )

  val article8: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(8),
    title = List(Title("Baldur har mareritt", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")),
    tags = List(Tag(List("baldur"), "nb")),
    visualElement = List.empty,
    introduction = List(Introduction("Baldur", "nb")),
    metaDescription = List.empty,
    created = today.minusDays(10),
    updated = today.minusDays(5),
    published = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val article9: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(9),
    title = List(Title("En Baldur har mareritt om Ragnarok", "nb")),
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")),
    tags = List(Tag(List("baldur"), "nb")),
    visualElement = List.empty,
    introduction = List(Introduction("Baldur", "nb")),
    metaDescription = List.empty,
    created = today.minusDays(10),
    updated = today.minusDays(5),
    published = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val article10: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(10),
    title = List(Title("This article is in english", "en")),
    content =
      List(ArticleContent("<p>artikkeltekst med fire deler</p><p>Something something <em>english</em> What", "en")),
    tags = List(Tag(List("englando"), "en")),
    visualElement = List.empty,
    introduction = List(Introduction("Engulsk", "en")),
    metaDescription = List.empty,
    metaImage = List(ArticleMetaImage("442", "alt", "en")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    published = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val article11: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(11),
    title = List(Title("Katter", "nb"), Title("Cats", "en"), Title("Chhattisgarhi", "hne")),
    content = List(
      ArticleContent(
        s"<p>Søkeord: delt?streng delt!streng delt&streng</p><$EmbedTagName data-resource=\"concept\" data-resource_id=\"222\" /><p>Noe om en katt</p>",
        "nb"
      ),
      ArticleContent("<p>Something about a cat</p>", "en"),
      ArticleContent("<p>Something about a Chhattisgarhi cat</p>", "hne")
    ),
    tags = List(Tag(List("ikkehund"), "nb"), Tag(List("notdog"), "en")),
    visualElement = List.empty,
    introduction = List(Introduction("Katter er store", "nb"), Introduction("Cats are big", "en")),
    metaDescription = List(common.Description("hurr durr ima sheep", "en")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    published = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val article12: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(12),
    title = List(Title("Ekstrastoff", "nb"), Title("extra", "en")),
    content = List(
      ArticleContent(
        s"Helsesøster H5P <p>delt-streng</p><$EmbedTagName data-title=\"Flubber\" data-resource=\"h5p\" data-path=\"/resource/id\"><$EmbedTagName data-resource=\"concept\" data-content-id=\"111\" data-title=\"Flubber\" /><$EmbedTagName data-videoid=\"77\" data-resource=\"video\"  /><$EmbedTagName data-resource=\"video\" data-resource_id=\"66\"  /><$EmbedTagName data-resource=\"video\" data-url=\"http://test\" data-resource_id=\"test-id1\"/>",
        "nb"
      ),
      ArticleContent(
        s"Header <$EmbedTagName data-resource_id=\"222\" /><$EmbedTagName data-resource=\"concept\" />",
        "en"
      )
    ),
    tags = List(Tag(List(""), "nb")),
    visualElement = List(VisualElement(s"<$EmbedTagName data-resource_id=\"333\">", "nb")),
    introduction = List(Introduction("Ekstra", "nb")),
    metaDescription = List(common.Description("", "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    published = today.minusDays(5),
    articleType = ArticleType.Standard
  )

  val article13: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(13),
    title = List(Title("Hemmelig og utilgjengelig", "nb")),
    content = List(
      ArticleContent(
        "Hemmelig",
        "nb"
      )
    ),
    tags = List(Tag(List(""), "nb")),
    visualElement = List(),
    introduction = List(Introduction("Intro", "nb")),
    metaDescription = List(common.Description("", "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    published = today.minusDays(5),
    articleType = ArticleType.Standard,
    availability = Availability.teacher
  )

  val article14: Article = TestData.sampleArticleWithPublicDomain.copy(
    id = Option(14),
    title = List(Title("Forsideartikkel", "nb")),
    slug = Some("forsideartikkel"),
    content = List(
      ArticleContent(
        s"Forsideartikkel <p>avsnitt</p><$EmbedTagName data-resource=\"concept\" data-content-id=\"123\" data-title=\"Forklaring\" data-type=\"block\" />",
        "nb"
      )
    ),
    tags = List(Tag(List(""), "nb")),
    visualElement = List(VisualElement(s"<$EmbedTagName data-resource_id=\"345\">", "nb")),
    introduction = List(Introduction("Ekstra", "nb")),
    metaDescription = List(common.Description("", "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    published = today.minusDays(5),
    articleType = ArticleType.FrontpageArticle
  )

  val articlesToIndex: Seq[Article] = List(
    article1,
    article2,
    article3,
    article4,
    article5,
    article6,
    article7,
    article8,
    article9,
    article10,
    article11,
    article12,
    article13,
    article14
  )

  val emptyDomainArticle: Article = Article(
    id = None,
    revision = None,
    title = Seq.empty,
    content = Seq.empty,
    copyright = Copyright("", "", Seq.empty, Seq.empty, Seq.empty, None, None, None),
    tags = Seq.empty,
    requiredLibraries = Seq.empty,
    visualElement = Seq.empty,
    introduction = Seq.empty,
    metaDescription = Seq.empty,
    metaImage = Seq.empty,
    created = today,
    updated = today,
    updatedBy = "",
    published = today,
    articleType = ArticleType.Standard,
    grepCodes = Seq.empty,
    conceptIds = Seq.empty,
    availability = Availability.everyone,
    relatedContent = Seq.empty,
    None,
    slug = None
  )

  val emptyDomainDraft: Draft = Draft(
    id = None,
    revision = None,
    status = Status(DraftStatus.DRAFT, Set.empty),
    title = Seq.empty,
    content = Seq.empty,
    copyright = None,
    tags = Seq.empty,
    requiredLibraries = Seq.empty,
    visualElement = Seq.empty,
    introduction = Seq.empty,
    metaDescription = Seq.empty,
    metaImage = Seq.empty,
    created = today,
    updated = today,
    updatedBy = "",
    published = today,
    articleType = ArticleType.Standard,
    notes = List.empty,
    previousVersionsNotes = List.empty,
    editorLabels = Seq.empty,
    grepCodes = Seq.empty,
    conceptIds = Seq.empty,
    availability = Availability.everyone,
    relatedContent = Seq.empty,
    revisionMeta = Seq.empty,
    responsible = None,
    slug = None
  )

  val draftStatus         = Status(DraftStatus.DRAFT, Set.empty)
  val importedDraftStatus = Status(DraftStatus.DRAFT, Set(DraftStatus.IMPORTED))

  val draftPublicDomainCopyright: draft.Copyright =
    draft.Copyright(Some("publicdomain"), Some(""), List.empty, List(), List(), None, None, None)

  val draftByNcSaCopyright = draft.Copyright(
    Some("by-nc-sa"),
    Some("Gotham City"),
    List(Author("Forfatter", "DC Comics")),
    List(),
    List(),
    None,
    None,
    None
  )

  val draftCopyrighted = draft.Copyright(
    Some("copyrighted"),
    Some("New York"),
    List(Author("Forfatter", "Clark Kent")),
    List(),
    List(),
    None,
    None,
    None
  )

  val sampleDraftWithPublicDomain = Draft(
    id = Option(1),
    revision = Option(1),
    status = draftStatus,
    title = Seq(Title("test", "en")),
    content = Seq(ArticleContent("<section><div>test</div></section>", "en")),
    copyright = Some(draftPublicDomainCopyright),
    tags = Seq.empty,
    requiredLibraries = Seq.empty,
    visualElement = Seq(VisualElement("image", "en")),
    introduction = Seq(Introduction("This is an introduction", "en")),
    metaDescription = Seq(common.Description("meta", "en")),
    metaImage = Seq.empty,
    created = LocalDateTime.now().minusDays(4),
    updated = LocalDateTime.now().minusDays(2),
    updatedBy = "ndalId54321",
    published = LocalDateTime.now().minusDays(2),
    articleType = ArticleType.Standard,
    notes = List.empty,
    previousVersionsNotes = List.empty,
    editorLabels = Seq.empty,
    grepCodes = Seq.empty,
    conceptIds = Seq.empty,
    availability = Availability.everyone,
    relatedContent = Seq.empty,
    revisionMeta = Seq.empty,
    responsible = None,
    slug = None
  )

  val sampleDraftWithByNcSa: Draft      = sampleDraftWithPublicDomain.copy(copyright = Some(draftByNcSaCopyright))
  val sampleDraftWithCopyrighted: Draft = sampleDraftWithPublicDomain.copy(copyright = Some(draftCopyrighted))

  val draft1: Draft = TestData.sampleDraftWithByNcSa.copy(
    id = Option(1),
    title = List(Title("Batmen er på vift med en bil", "nb")),
    introduction = List(Introduction("Batmen", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(
      ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")
    ),
    tags = List(Tag(List("fugl"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(3),
    copyright = Some(draftByNcSaCopyright.copy(creators = List(Author("Forfatter", "Kjekspolitiet")))),
    grepCodes = Seq("K123", "K456")
  )

  val draft2: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(2),
    title = List(Title("Pingvinen er ute og går", "nb")),
    introduction = List(Introduction("Pingvinen", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av en</p><p> en <em>pingvin</em> som vagger borover en gate</p>", "nb")),
    tags = List(Tag(List("fugl"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(2),
    copyright = Some(
      draftPublicDomainCopyright
        .copy(creators = List(Author("Forfatter", "Pjolter")), processors = List(Author("Editorial", "Svims")))
    ),
    grepCodes = Seq("K456", "K123")
  )

  val draft3: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(3),
    title = List(Title("Donald Duck kjører bil", "nb")),
    introduction = List(Introduction("Donald Duck", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av en en and</p><p> som <strong>kjører</strong> en rød bil.</p>", "nb")),
    tags = List(Tag(List("and"), "nb")),
    created = today.minusDays(4),
    updated = today.minusDays(1),
    grepCodes = Seq("K123")
  )

  val draft4: Draft = TestData.sampleDraftWithCopyrighted.copy(
    id = Option(4),
    title = List(Title("Superman er ute og flyr", "nb")),
    introduction = List(Introduction("Superman", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content =
      List(ArticleContent("<p>Bilde av en flygende mann</p><p> som <strong>har</strong> superkrefter.</p>", "nb")),
    tags = List(Tag(List("supermann"), "nb")),
    created = today.minusDays(4),
    updated = today
  )

  val draft5: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(5),
    title = List(Title("Hulken løfter biler", "nb")),
    introduction = List(Introduction("Hulken", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av hulk</p><p> som <strong>løfter</strong> en rød bil.</p>", "nb")),
    tags = List(Tag(List("hulk"), "nb")),
    created = today.minusDays(40),
    updated = today.minusDays(35),
    notes = List(
      EditorNote(
        "kakemonster",
        "ndalId54321",
        Status(DraftStatus.DRAFT, Set.empty),
        today.minusDays(30)
      )
    ),
    previousVersionsNotes = List(
      EditorNote(
        "kultgammeltnotat",
        "ndalId12345",
        Status(DraftStatus.DRAFT, Set.empty),
        today.minusDays(31)
      )
    ),
    grepCodes = Seq("K456")
  )

  val draft6: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(6),
    title = List(Title("Loke og Tor prøver å fange midgaardsormen", "nb")),
    introduction = List(Introduction("Loke og Tor", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(
      ArticleContent(
        "<p>Bilde av <em>Loke</em> og <em>Tor</em></p><p> som <strong>fisker</strong> fra Naglfar.</p>",
        "nb"
      )
    ),
    tags = List(Tag(List("Loke", "Tor", "Naglfar"), "nb")),
    created = today.minusDays(30),
    updated = today.minusDays(25)
  )

  val draft7: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(7),
    title = List(Title("Yggdrasil livets tre", "nb")),
    introduction = List(Introduction("Yggdrasil", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Yggdrasil</em> livets tre med alle dyrene som bor i det.", "nb")),
    tags = List(Tag(List("yggdrasil"), "nb")),
    created = today.minusDays(20),
    updated = today.minusDays(15)
  )

  val draft8: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(8),
    title = List(Title("Baldur har mareritt", "nb")),
    introduction = List(Introduction("Baldur", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> mareritt om Ragnarok.", "nb")),
    tags = List(Tag(List("baldur"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val draft9: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(9),
    title = List(Title("Baldur har mareritt om Ragnarok", "nb")),
    introduction = List(Introduction("Baldur", "nb")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Bilde av <em>Baldurs</em> som har  mareritt.", "nb")),
    tags = List(Tag(List("baldur"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val draft10: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(10),
    status = Status(DraftStatus.PROPOSAL, Set.empty),
    title = List(Title("This article is in english", "en")),
    introduction = List(Introduction("Engulsk", "en")),
    metaDescription = List.empty,
    visualElement = List.empty,
    content = List(ArticleContent("<p>Something something <em>english</em> What", "en")),
    tags = List(Tag(List("englando"), "en")),
    metaImage = List(ArticleMetaImage("123", "alt", "en")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val draft11: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(11),
    status = Status(DraftStatus.PROPOSAL, Set.empty),
    title = List(Title("Katter", "nb"), Title("Cats", "en")),
    introduction = List(Introduction("Katter er store", "nb"), Introduction("Cats are big", "en")),
    content = List(ArticleContent("<p>Noe om en katt</p>", "nb"), ArticleContent("<p>Something about a cat</p>", "en")),
    tags = List(Tag(List("katt"), "nb"), Tag(List("cat"), "en")),
    metaDescription = List(common.Description("hurr dirr ima sheep", "en")),
    visualElement = List.empty,
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val draft12: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(12),
    status = importedDraftStatus,
    title = List(Title("Ekstrastoff", "nb")),
    introduction = List(Introduction("Ekstra", "nb")),
    metaDescription = List(common.Description("", "nb")),
    content = List(
      ArticleContent(
        s"<section><p>artikkeltekst med fire deler</p><$EmbedTagName data-resource=\"concept\" data-resource_id=\"222\" /><$EmbedTagName data-resource=\"image\" data-resource_id=\"test-image.id\"  data-url=\"test-image.url\"/><$EmbedTagName data-resource=\"image\" data-resource_id=\"55\"/><$EmbedTagName data-resource=\"concept\" data-content-id=\"111\" data-title=\"Flubber\" /><$EmbedTagName data-videoid=\"77\" data-resource=\"video\"  /><$EmbedTagName data-resource=\"video\" data-resource_id=\"66\"  /><$EmbedTagName data-resource=\"video\"  data-url=\"http://test.test\" />",
        "nb"
      )
    ),
    visualElement = List(VisualElement(s"<$EmbedTagName data-resource_id=\"333\">", "nb")),
    tags = List(Tag(List(""), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5)
  )

  val draft13: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(13),
    title = List(Title("Luringen", "nb"), Title("English title", "en"), Title("Chhattisgarhi title", "hne")),
    introduction = List(Introduction("Luringen", "nb")),
    metaDescription = List(common.Description("", "nb")),
    content = List(
      ArticleContent("<section><p>Helsesøster</p><p>Søkeord: delt?streng delt!streng delt&streng</p></section>", "nb"),
      ArticleContent(
        s"Header <$EmbedTagName data-resource_id=\"222\" /><$EmbedTagName data-resource=\"concept\" />",
        "en"
      ),
      ArticleContent(
        s"Header in Chhattisgarhi <$EmbedTagName data-resource_id=\"222\" /><$EmbedTagName data-resource=\"concept\" />",
        "hne"
      )
    ),
    visualElement = List.empty,
    tags = List(Tag(List(""), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    updatedBy = "someotheruser",
    articleType = ArticleType.TopicArticle
  )

  val draft14: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(14),
    title = List(Title("Slettet", "nb")),
    introduction = List(Introduction("Slettet", "nb")),
    metaDescription = List(common.Description("", "nb")),
    content = List(ArticleContent("", "nb")),
    visualElement = List.empty,
    tags = List(Tag(List(""), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    status = Status(
      current = DraftStatus.ARCHIVED,
      other = Set.empty
    )
  )

  val draft15: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(15),
    title = List(Title("Engler og demoner", "nb")),
    introduction = List(Introduction("Religion", "nb")),
    metaDescription = List(common.Description("metareligion", "nb")),
    content = List(
      ArticleContent("<section><p>Vanlig i gamle testamentet</p><p>delt-streng</p></section>", "nb"),
      ArticleContent("<p>Christianity!</p>", "en")
    ),
    visualElement = List.empty,
    tags = List(Tag(List("engel"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.TopicArticle
  )

  val draft16: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(16),
    title = List(Title("Engler og demoner", "nb")),
    slug = Some("engler-og-demoner"),
    introduction = List(Introduction("Religion", "nb")),
    metaDescription = List(common.Description("metareligion", "nb")),
    content = List(
      ArticleContent("<section><p>Vanlig i gamle testamentet</p></section>", "nb")
    ),
    visualElement = List.empty,
    tags = List(Tag(List("engel"), "nb")),
    created = today.minusDays(10),
    updated = today.minusDays(5),
    articleType = ArticleType.FrontpageArticle
  )

  val draftsToIndex: List[Draft] = List(
    draft1,
    draft2,
    draft3,
    draft4,
    draft5,
    draft6,
    draft7,
    draft8,
    draft9,
    draft10,
    draft11,
    draft12,
    draft13,
    draft14,
    draft15,
    draft16
  )

  val paul              = Author("author", "Truly Weird Rand Paul")
  val license           = "publicdomain"
  val copyright         = common.learningpath.Copyright(license, List(paul))
  val visibleMetadata   = Some(Metadata(Seq.empty, visible = true))
  val invisibleMetadata = Some(Metadata(Seq.empty, visible = false))

  val DefaultLearningPath = LearningPath(
    id = None,
    revision = None,
    externalId = None,
    isBasedOn = None,
    title = List(),
    description = List(),
    coverPhotoId = None,
    duration = Some(0),
    status = LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    lastUpdated = today,
    tags = List(),
    owner = "owner",
    copyright = copyright
  )

  val PenguinId   = 1
  val BatmanId    = 2
  val DonaldId    = 3
  val UnrelatedId = 4
  val EnglandoId  = 5
  val KekId       = 6

  val learningPath1: LearningPath = DefaultLearningPath.copy(
    id = Some(PenguinId),
    title = List(Title("Pingvinen er en kjeltring", "nb")),
    description = List(learningpath.Description("Dette handler om fugler", "nb")),
    duration = Some(1),
    lastUpdated = today.minusDays(34),
    tags = List(Tag(List("superhelt", "kanikkefly"), "nb"))
  )

  val learningPath2: LearningPath = DefaultLearningPath.copy(
    id = Some(BatmanId),
    title = List(Title("Batman er en tøff og morsom helt", "nb"), Title("Batman is a tough guy", "en")),
    description = List(learningpath.Description("Dette handler om flaggermus, som kan ligne litt på en fugl", "nb")),
    duration = Some(2),
    lastUpdated = today.minusDays(3),
    tags = List(Tag(Seq("superhelt", "kanfly"), "nb"))
  )

  val learningPath3: LearningPath = DefaultLearningPath.copy(
    id = Some(DonaldId),
    title = List(Title("Donald er en tøff, rar og morsom and", "nb"), Title("Donald is a weird duck", "en")),
    description =
      List(learningpath.Description("Dette handler om en and, som også minner om både flaggermus og fugler.", "nb")),
    duration = Some(3),
    lastUpdated = today.minusDays(4),
    tags = List(Tag(Seq("disney", "kanfly"), "nb"))
  )

  val learningPath4: LearningPath = DefaultLearningPath.copy(
    id = Some(UnrelatedId),
    title = List(Title("Unrelated", "en"), Title("Urelatert", "nb")),
    description = List(
      learningpath.Description("This is unrelated", "en"),
      learningpath.Description("Dette er en urelatert", "nb")
    ),
    duration = Some(4),
    lastUpdated = today.minusDays(5),
    tags = List()
  )

  val learningPath5: LearningPath = DefaultLearningPath.copy(
    id = Some(EnglandoId),
    title = List(Title("Englando", "en")),
    description = List(learningpath.Description("This is a englando learningpath", "en")),
    duration = Some(5),
    lastUpdated = today.minusDays(6),
    tags = List(),
    copyright = copyright.copy(contributors = List(Author("Writer", "Svims")))
  )

  val learningPath6: LearningPath = DefaultLearningPath.copy(
    id = Some(KekId),
    title = List(Title("Kek", "en")),
    description = List(learningpath.Description("This is kek", "en")),
    duration = Some(5),
    lastUpdated = today.minusDays(7),
    tags = List()
  )

  val learningPathsToIndex: List[LearningPath] = List(
    learningPath1,
    learningPath2,
    learningPath3,
    learningPath4,
    learningPath5,
    learningPath6
  )

  val subjects = List(
    TaxSubject("urn:subject:1", "Matte", None, Some("/subject:1"), visibleMetadata, List.empty),
    TaxSubject("urn:subject:2", "Historie", None, Some("/subject:2"), visibleMetadata, List.empty),
    TaxSubject("urn:subject:3", "Religion", None, Some("/subject:3"), invisibleMetadata, List.empty)
  )

  val relevances = List(
    Relevance("urn:relevance:core", "Kjernestoff", List.empty),
    Relevance("urn:relevance:supplementary", "Tilleggsstoff", List.empty)
  )

  val resourceTypes = List(
    ResourceType("urn:resourcetype:learningpath", "Læringssti", None, List.empty),
    ResourceType(
      "urn:resourcetype:subjectMaterial",
      "Fagstoff",
      Some(
        List(
          ResourceType("urn:resourcetype:academicArticle", "Fagartikkel", None, List.empty),
          ResourceType("urn:resourcetype:guidance", "Veiledning", None, List.empty)
        )
      ),
      List.empty
    ),
    ResourceType(
      "urn:resourcetype:reviewResource",
      "Vurderingsressurs",
      Some(
        List(
          ResourceType("urn:resourcetype:teacherEvaluation", "Lærervurdering", None, List.empty),
          ResourceType("urn:resourcetype:selfEvaluation", "Egenvurdering", None, List.empty),
          ResourceType(
            "urn:resourcetype:peerEvaluation",
            "Medelevvurdering",
            Some(
              List(
                ResourceType("urn:resourcetype:nested", "SuperNested ResourceType", None, List.empty)
              )
            ),
            List.empty
          )
        )
      ),
      List.empty
    )
  )

  val resources = List(
    Resource(
      "urn:resource:1",
      article1.title.head.title,
      Some(s"urn:article:${article1.id.get}"),
      Some("/subject:3/topic:5/resource:1"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:2",
      article2.title.head.title,
      Some(s"urn:article:${article2.id.get}"),
      Some("/subject:1/topic:1/resource:2"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:3",
      article3.title.head.title,
      Some(s"urn:article:${article3.id.get}"),
      Some("/subject:1/topic:3/resource:3"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:4",
      article4.title.head.title,
      Some(s"urn:article:${article4.id.get}"),
      Some("/subject:1/topic:1/topic:2/resource:4"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:5",
      article5.title.head.title,
      Some(s"urn:article:${article5.id.get}"),
      Some("/subject:2/topic:4/resource:5"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:6",
      article6.title.head.title,
      Some(s"urn:article:${article6.id.get}"),
      Some("/subject:2/topic:4/resource:6"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:7",
      article7.title.head.title,
      Some(s"urn:article:${article7.id.get}"),
      Some("/subject:2/topic:4/resource:7"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:8",
      learningPath1.title.head.title,
      Some(s"urn:learningpath:${learningPath1.id.get}"),
      Some("/subject:1/topic:1/resource:8"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:9",
      learningPath2.title.head.title,
      Some(s"urn:learningpath:${learningPath2.id.get}"),
      Some("/subject:1/topic:1/resource:9"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:10",
      learningPath3.title.head.title,
      Some(s"urn:learningpath:${learningPath3.id.get}"),
      Some("/subject:1/topic:3/resource:10"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:11",
      learningPath4.title.head.title,
      Some(s"urn:learningpath:${learningPath4.id.get}"),
      Some("/subject:1/topic:1/topic:2/resource:11"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:12",
      learningPath5.title.head.title,
      Some(s"urn:learningpath:${learningPath5.id.get}"),
      Some("/subject:2/topic:4/resource:12"),
      visibleMetadata,
      List.empty
    ),
    Resource(
      "urn:resource:13",
      article12.title.head.title,
      Some(s"urn:article:${article12.id.get}"),
      Some("/subject:2/topic:4/resource:13"),
      visibleMetadata,
      List.empty
    )
  )

  val topics = List(
    Topic(
      "urn:topic:1",
      article8.title.head.title,
      Some(s"urn:article:${article8.id.get}"),
      Some("/subject:1/topic:1"),
      visibleMetadata,
      List.empty
    ),
    Topic(
      "urn:topic:2",
      article9.title.head.title,
      Some(s"urn:article:${article9.id.get}"),
      Some("/subject:1/topic:1/topic:2"),
      visibleMetadata,
      List.empty
    ),
    Topic(
      "urn:topic:3",
      article10.title.head.title,
      Some(s"urn:article:${article10.id.get}"),
      Some("/subject:1/topic:3"),
      visibleMetadata,
      List.empty
    ),
    Topic(
      "urn:topic:4",
      article11.title.head.title,
      Some(s"urn:article:${article11.id.get}"),
      Some("/subject:2/topic:4"),
      visibleMetadata,
      List.empty
    ),
    Topic(
      "urn:topic:5",
      draft15.title.head.title,
      Some(s"urn:article:${draft15.id.get}"),
      Some("/subject:3/topic:5"),
      invisibleMetadata,
      List.empty
    )
  )

  val subjectTopicConnections = List(
    SubjectTopicConnection(
      "urn:subject:1",
      "urn:topic:1",
      "urn:subject-topic:1",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    SubjectTopicConnection(
      "urn:subject:1",
      "urn:topic:3",
      "urn:subject-topic:2",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    SubjectTopicConnection(
      "urn:subject:2",
      "urn:topic:4",
      "urn:subject-topic:3",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    SubjectTopicConnection(
      "urn:subject:3",
      "urn:topic:5",
      "urn:subject-topic:4",
      primary = true,
      1,
      Some("urn:relevance:supplementary")
    )
  )

  val topicResourceConnections = List(
    TopicResourceConnection(
      "urn:topic:1",
      "urn:resource:1",
      "urn:topic-resource:1",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    TopicResourceConnection(
      "urn:topic:4",
      "urn:resource:1",
      "urn:topic-resource:2",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    TopicResourceConnection(
      "urn:topic:1",
      "urn:resource:2",
      "urn:topic-resource:3",
      primary = true,
      1,
      Some("urn:relevance:supplementary")
    ),
    TopicResourceConnection(
      "urn:topic:3",
      "urn:resource:3",
      "urn:topic-resource:4",
      primary = true,
      1,
      Some("urn:relevance:supplementary")
    ),
    TopicResourceConnection(
      "urn:topic:2",
      "urn:resource:4",
      "urn:topic-resource:5",
      primary = true,
      1,
      Some("urn:relevance:supplementary")
    ),
    TopicResourceConnection(
      "urn:topic:4",
      "urn:resource:5",
      "urn:topic-resource:6",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    TopicResourceConnection(
      "urn:topic:4",
      "urn:resource:6",
      "urn:topic-resource:7",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    TopicResourceConnection(
      "urn:topic:4",
      "urn:resource:7",
      "urn:topic-resource:8",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    TopicResourceConnection(
      "urn:topic:3",
      "urn:resource:5",
      "urn:topic-resource:9",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    TopicResourceConnection(
      "urn:topic:1",
      "urn:resource:8",
      "urn:topic-resource:10",
      primary = true,
      1,
      Some("urn:relevance:supplementary")
    ),
    TopicResourceConnection(
      "urn:topic:1",
      "urn:resource:9",
      "urn:topic-resource:11",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    TopicResourceConnection(
      "urn:topic:3",
      "urn:resource:10",
      "urn:topic-resource:12",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    TopicResourceConnection(
      "urn:topic:2",
      "urn:resource:11",
      "urn:topic-resource:13",
      primary = true,
      1,
      Some("urn:relevance:supplementary")
    ),
    TopicResourceConnection(
      "urn:topic:4",
      "urn:resource:12",
      "urn:topic-resource:14",
      primary = true,
      1,
      Some("urn:relevance:supplementary")
    ),
    TopicResourceConnection(
      "urn:topic:1",
      "urn:resource:13",
      "urn:topic-resource:15",
      primary = true,
      1,
      Some("urn:relevance:core")
    ),
    TopicResourceConnection(
      "urn:topic:4",
      "urn:resource:13",
      "urn:topic-resource:16",
      primary = true,
      1,
      Some("urn:relevance:supplementary")
    ),
    TopicResourceConnection(
      "urn:topic:5",
      "urn:resource:1",
      "urn:topic-resource:17",
      primary = true,
      1,
      Some("urn:relevance:core")
    )
  )

  val topicSubtopicConnections = List(
    TopicSubtopicConnection(
      "urn:topic:1",
      "urn:topic:2",
      "urn:topic-subtopic:1",
      primary = true,
      1,
      Some("urn:relevance:core")
    )
  )

  val resourceResourceTypeConnections = List(
    ResourceResourceTypeConnection("urn:resource:1", "urn:resourcetype:subjectMaterial", "urn:resource-resourcetype:1"),
    ResourceResourceTypeConnection("urn:resource:2", "urn:resourcetype:subjectMaterial", "urn:resource-resourcetype:2"),
    ResourceResourceTypeConnection("urn:resource:2", "urn:resourcetype:academicArticle", "urn:resource-resourcetype:3"),
    ResourceResourceTypeConnection("urn:resource:3", "urn:resourcetype:subjectMaterial", "urn:resource-resourcetype:4"),
    ResourceResourceTypeConnection("urn:resource:4", "urn:resourcetype:subjectMaterial", "urn:resource-resourcetype:5"),
    ResourceResourceTypeConnection("urn:resource:5", "urn:resourcetype:academicArticle", "urn:resource-resourcetype:6"),
    ResourceResourceTypeConnection("urn:resource:6", "urn:resourcetype:subjectMaterial", "urn:resource-resourcetype:7"),
    ResourceResourceTypeConnection("urn:resource:7", "urn:resourcetype:guidance", "urn:resource-resourcetype:8"),
    ResourceResourceTypeConnection("urn:resource:7", "urn:resourcetype:nested", "urn:resource-resourcetype:9"),
    ResourceResourceTypeConnection("urn:resource:8", "urn:resourcetype:learningpath", "urn:resource-resourcetype:10"),
    ResourceResourceTypeConnection("urn:resource:9", "urn:resourcetype:learningpath", "urn:resource-resourcetype:11"),
    ResourceResourceTypeConnection("urn:resource:10", "urn:resourcetype:learningpath", "urn:resource-resourcetype:12"),
    ResourceResourceTypeConnection("urn:resource:11", "urn:resourcetype:learningpath", "urn:resource-resourcetype:13"),
    ResourceResourceTypeConnection("urn:resource:12", "urn:resourcetype:learningpath", "urn:resource-resourcetype:14"),
    ResourceResourceTypeConnection(
      "urn:resource:13",
      "urn:resourcetype:subjectMaterial",
      "urn:resource-resourcetype:15"
    )
  )

  val taxonomyTestBundle = TaxonomyBundle(
    relevances = relevances,
    resourceResourceTypeConnections = resourceResourceTypeConnections,
    resourceTypes = resourceTypes,
    resources = resources,
    subjectTopicConnections = subjectTopicConnections,
    subjects = subjects,
    topicResourceConnections = topicResourceConnections,
    topicSubtopicConnections = topicSubtopicConnections,
    topics = topics
  )

  val emptyGrepBundle = GrepBundle(
    kjerneelementer = List.empty,
    kompetansemaal = List.empty,
    tverrfagligeTemaer = List.empty
  )

  val grepBundle = emptyGrepBundle.copy(
    kjerneelementer = List(
      GrepElement("KE12", Seq(GrepTitle("default", "Utforsking og problemløysing"))),
      GrepElement("KE34", Seq(GrepTitle("default", "Abstraksjon og generalisering")))
    ),
    kompetansemaal = List(
      GrepElement(
        "KM123",
        Seq(GrepTitle("default", "bruke ulike kilder på en kritisk, hensiktsmessig og etterrettelig måte"))
      )
    ),
    tverrfagligeTemaer = List(GrepElement("TT2", Seq(GrepTitle("default", "Demokrati og medborgerskap"))))
  )

  val searchSettings: SearchSettings = SearchSettings(
    query = None,
    fallback = false,
    language = DefaultLanguage,
    license = None,
    page = 1,
    pageSize = 20,
    sort = Sort.ByIdAsc,
    withIdIn = List.empty,
    subjects = List.empty,
    resourceTypes = List.empty,
    learningResourceTypes = List.empty,
    supportedLanguages = List.empty,
    relevanceIds = List.empty,
    grepCodes = List.empty,
    shouldScroll = false,
    filterByNoResourceType = false,
    aggregatePaths = List.empty,
    embedResource = List.empty,
    embedId = None,
    availability = List.empty,
    articleTypes = List.empty
  )

  val multiDraftSearchSettings: MultiDraftSearchSettings = MultiDraftSearchSettings(
    query = None,
    noteQuery = None,
    fallback = false,
    language = DefaultLanguage,
    license = None,
    page = 1,
    pageSize = 20,
    sort = Sort.ByIdAsc,
    withIdIn = List.empty,
    subjects = List.empty,
    topics = List.empty,
    resourceTypes = List.empty,
    learningResourceTypes = List.empty,
    supportedLanguages = List.empty,
    relevanceIds = List.empty,
    statusFilter = List.empty,
    userFilter = List.empty,
    grepCodes = List.empty,
    shouldScroll = false,
    searchDecompounded = false,
    aggregatePaths = List.empty,
    embedResource = List.empty,
    embedId = None,
    includeOtherStatuses = false,
    revisionDateFilterFrom = None,
    revisionDateFilterTo = None,
    excludeRevisionHistory = false,
    responsibleIdFilter = List.empty,
    articleTypes = List.empty
  )

  val searchableResourceTypes = List(
    SearchableTaxonomyResourceType(
      "urn:resourcetype:subjectMaterial",
      SearchableLanguageValues(Seq(LanguageValue("nb", "Fagstoff")))
    ),
    SearchableTaxonomyResourceType(
      "urn:resourcetype:academicArticle",
      SearchableLanguageValues(Seq(LanguageValue("nb", "Fagartikkel")))
    )
  )

  val singleSearchableTaxonomyContext =
    SearchableTaxonomyContext(
      id = "urn:resource:101",
      subjectId = "urn:subject:1",
      subject = SearchableLanguageValues(Seq(LanguageValue("nb", "Matte"))),
      path = "/subject:3/topic:1/topic:151/resource:101",
      breadcrumbs = SearchableLanguageList(
        Seq(
          LanguageValue("nb", Seq("Matte", "Østen for solen", "Vesten for månen"))
        )
      ),
      contextType = LearningResourceType.Article.toString,
      relevanceId = Some("urn:relevance:core"),
      relevance = SearchableLanguageValues(Seq(LanguageValue("nb", "Kjernestoff"))),
      resourceTypes = searchableResourceTypes,
      parentTopicIds = List("urn:topic:1"),
      isPrimaryConnection = true
    )

  val searchableTaxonomyContexts = List(
    singleSearchableTaxonomyContext
  )
}
