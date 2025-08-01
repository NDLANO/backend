/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.api.MyNDLABundleDTO
import no.ndla.common.model.api.search.LearningResourceType
import no.ndla.common.model.domain.ContributorType
import no.ndla.common.model.domain.{
  ArticleContent,
  ArticleMetaImage,
  ArticleType,
  Author,
  Availability,
  EditorNote,
  Introduction,
  Priority,
  Responsible,
  Status,
  Tag,
  Title,
  VisualElement,
  draft
}
import no.ndla.common.model.domain.article.{Article, Copyright}
import no.ndla.common.model.domain.concept.{
  Concept,
  ConceptContent,
  ConceptEditorNote,
  ConceptStatus,
  ConceptType,
  GlossData,
  GlossExample,
  WordClass
}
import no.ndla.common.model.domain.draft.{Draft, DraftCopyright, DraftStatus, RevisionMeta, RevisionStatus}
import no.ndla.common.model.domain.language.OptLanguageFields
import no.ndla.common.model.domain.learningpath.{
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningpathCopyright,
  Description as LPDescription
}
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.language.Language.DefaultLanguage
import no.ndla.mapping.License
import no.ndla.search.model.domain.EmbedValues
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.api.grep.GrepStatusDTO
import no.ndla.searchapi.model.domain.*
import no.ndla.searchapi.model.grep.{
  BelongsToObj,
  GrepBundle,
  GrepKjerneelement,
  GrepKompetansemaal,
  GrepLaererplan,
  GrepTextObj,
  GrepTitle,
  GrepTverrfagligTema
}
import no.ndla.searchapi.model.search.*
import no.ndla.searchapi.model.search.settings.{MultiDraftSearchSettings, SearchSettings}
import no.ndla.searchapi.model.taxonomy.*

import java.net.URI
import java.util.UUID
import scala.util.Random

object TestData {

  private val publicDomainCopyright =
    Copyright(License.PublicDomain.toString, None, List(), List(), List(), None, None, false)
  private val byNcSaCopyright =
    Copyright(
      License.CC_BY_NC_SA.toString,
      Some("Gotham City"),
      List(Author(ContributorType.Writer, "DC Comics")),
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
      List(Author(ContributorType.Writer, "Clark Kent")),
      List(),
      List(),
      None,
      None,
      false
    )
  val today: NDLADate = NDLADate.now().withNano(0)

  val sampleArticleTitle: ArticleApiTitle                 = ArticleApiTitle("tittell", "tittell", "nb")
  val sampleArticleVisualElement: ArticleApiVisualElement =
    ArticleApiVisualElement(s"""<$EmbedTagName data-resource="image">""", "nb")
  val sampleArticleIntro: ArticleApiIntro = ArticleApiIntro("intro", "intro", "nb")

  val sampleArticleSearch: ArticleApiSearchResults = ArticleApiSearchResults(
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
        ArticleApiTitle("Another title", "Another title", "nb"),
        Option(sampleArticleVisualElement),
        Option(sampleArticleIntro),
        "http://articles/2",
        "by",
        "standard",
        Seq("nb", "en")
      )
    )
  )

  val sampleImageSearch: ImageApiSearchResults = ImageApiSearchResults(
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

  val sampleLearningpath: LearningpathApiSearchResults = LearningpathApiSearchResults(
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

  val sampleAudio: AudioApiSearchResults = AudioApiSearchResults(
    totalCount = 2,
    page = 1,
    pageSize = 10,
    language = "nb",
    results = Seq(
      AudioApiSearchResult(1, AudioApiTitle("en title", "nb"), "http://audio/1", "by", Seq("nb")),
      AudioApiSearchResult(2, AudioApiTitle("ny tlttle", "nb"), "http://audio/2", "by", Seq("nb"))
    )
  )

  val (articleId, externalId) = (1L, "751234")

  val sampleArticleWithPublicDomain: Article = Article(
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
    slug = None,
    disclaimer = OptLanguageFields.empty
  )

  val sampleDomainArticle: Article = Article(
    Option(articleId),
    Option(2),
    Seq(Title("title", "nb")),
    Seq(ArticleContent("content", "nb")),
    Copyright("by", None, Seq(), Seq(), Seq(), None, None, false),
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
    slug = None,
    disclaimer = OptLanguageFields.empty
  )

  val sampleDomainArticle2: Article = Article(
    None,
    None,
    Seq(Title("test", "en")),
    Seq(ArticleContent("<article><div>test</div></article>", "en")),
    Copyright(License.PublicDomain.toString, None, Seq(), Seq(), Seq(), None, None, false),
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
    slug = None,
    disclaimer = OptLanguageFields.empty
  )

  val sampleArticleWithByNcSa: Article =
    sampleArticleWithPublicDomain.copy(copyright = byNcSaCopyright, published = NDLADate.now())

  val sampleArticleWithCopyrighted: Article =
    sampleArticleWithPublicDomain.copy(copyright = copyrighted, published = NDLADate.now())

  val article1: Article = TestData.sampleArticleWithByNcSa.copy(
    id = Option(1),
    title = List(Title("Batmen er på vift med en bil", "nb")),
    content = List(
      ArticleContent("Bilde av en <strong>bil</strong> flaggermusmann som vifter med vingene <em>bil</em>.", "nb")
    ),
    copyright = byNcSaCopyright.copy(creators = List(Author(ContributorType.Writer, "Kjekspolitiet"))),
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
      .copy(
        creators = List(Author(ContributorType.Writer, "Pjolter")),
        processors = List(Author(ContributorType.Editorial, "Svims"))
      ),
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
        s"Helsesøster H5P <p>delt-streng</p><$EmbedTagName data-title=\"Flubber\" data-resource=\"h5p\" data-path=\"/resource/id\"></$EmbedTagName><$EmbedTagName data-resource=\"concept\" data-content-id=\"111\" data-title=\"Flubber\"></$EmbedTagName><$EmbedTagName data-videoid=\"77\" data-resource=\"video\"></$EmbedTagName><$EmbedTagName data-resource=\"video\" data-resource_id=\"66\"></$EmbedTagName><$EmbedTagName data-resource=\"video\" data-url=\"http://test\" data-resource_id=\"test-id1\"></$EmbedTagName>",
        "nb"
      ),
      ArticleContent(
        s"Header <$EmbedTagName data-resource_id=\"222\" /><$EmbedTagName data-resource=\"concept\"></$EmbedTagName>",
        "en"
      )
    ),
    tags = List(Tag(List(""), "nb")),
    visualElement = List(VisualElement(s"<$EmbedTagName data-resource_id=\"333\"></$EmbedTagName>", "nb")),
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
        s"Forsideartikkel <p>avsnitt</p><$EmbedTagName data-resource=\"concept\" data-content-id=\"123\" data-title=\"Forklaring\" data-type=\"block\"></$EmbedTagName>",
        "nb"
      )
    ),
    tags = List(Tag(List(""), "nb")),
    visualElement = List(VisualElement(s"<$EmbedTagName data-resource_id=\"345\"></$EmbedTagName>", "nb")),
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
    copyright = Copyright("", None, Seq.empty, Seq.empty, Seq.empty, None, None, false),
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
    revisionDate = None,
    slug = None,
    disclaimer = OptLanguageFields.empty
  )

  val emptyDomainDraft: Draft = Draft(
    id = None,
    revision = None,
    status = Status(DraftStatus.PLANNED, Set.empty),
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
    slug = None,
    comments = Seq.empty,
    priority = Priority.Unspecified,
    started = false,
    qualityEvaluation = None,
    disclaimer = OptLanguageFields.empty
  )

  val draftStatus: Status         = Status(DraftStatus.PLANNED, Set.empty)
  val importedDraftStatus: Status = Status(DraftStatus.PLANNED, Set(DraftStatus.IMPORTED))

  val draftPublicDomainCopyright: draft.DraftCopyright =
    draft.DraftCopyright(Some(License.PublicDomain.toString), Some(""), List.empty, List(), List(), None, None, false)

  val draftByNcSaCopyright: DraftCopyright = draft.DraftCopyright(
    Some(License.CC_BY_NC_SA.toString),
    Some("Gotham City"),
    List(Author(ContributorType.Writer, "DC Comics")),
    List(),
    List(),
    None,
    None,
    false
  )

  val draftCopyrighted: DraftCopyright = draft.DraftCopyright(
    Some(License.Copyrighted.toString),
    Some("New York"),
    List(Author(ContributorType.Writer, "Clark Kent")),
    List(),
    List(),
    None,
    None,
    false
  )

  val sampleDraftWithPublicDomain: Draft = Draft(
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
    created = NDLADate.now().withNano(0).minusDays(4),
    updated = NDLADate.now().withNano(0).minusDays(2),
    updatedBy = "ndalId54321",
    published = NDLADate.now().withNano(0).minusDays(2),
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
    slug = None,
    comments = Seq.empty,
    priority = Priority.Unspecified,
    started = false,
    qualityEvaluation = None,
    disclaimer = OptLanguageFields.empty
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
    copyright = Some(draftByNcSaCopyright.copy(creators = List(Author(ContributorType.Writer, "Kjekspolitiet")))),
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
        .copy(
          creators = List(Author(ContributorType.Writer, "Pjolter")),
          processors = List(Author(ContributorType.Editorial, "Svims"))
        )
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
        Status(DraftStatus.PLANNED, Set.empty),
        today.minusDays(30)
      )
    ),
    previousVersionsNotes = List(
      EditorNote(
        "kultgammeltnotat",
        "ndalId12345",
        Status(DraftStatus.PLANNED, Set.empty),
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
    status = Status(DraftStatus.IN_PROGRESS, Set.empty),
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
    status = Status(DraftStatus.IN_PROGRESS, Set.empty),
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
        s"<section><p>artikkeltekst med fire deler</p><$EmbedTagName data-resource=\"concept\" data-resource_id=\"222\"></$EmbedTagName><$EmbedTagName data-resource=\"image\" data-resource_id=\"test-image.id\"  data-url=\"test-image.url\"></$EmbedTagName><$EmbedTagName data-resource=\"image\" data-resource_id=\"55\"></$EmbedTagName><$EmbedTagName data-resource=\"concept\" data-content-id=\"111\" data-title=\"Flubber\"></$EmbedTagName><$EmbedTagName data-videoid=\"77\" data-resource=\"video\"></$EmbedTagName><$EmbedTagName data-resource=\"video\" data-resource_id=\"66\"></$EmbedTagName><$EmbedTagName data-resource=\"video\"  data-url=\"http://test.test\"></$EmbedTagName>",
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
        s"Header <$EmbedTagName data-resource_id=\"222\" /><$EmbedTagName data-resource=\"concept\"></$EmbedTagName>",
        "en"
      ),
      ArticleContent(
        s"Header in Chhattisgarhi <$EmbedTagName data-resource_id=\"222\" /><$EmbedTagName data-resource=\"concept\"></$EmbedTagName>",
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

  val draft17: Draft = TestData.sampleDraftWithPublicDomain.copy(
    id = Option(17),
    status = Status(DraftStatus.UNPUBLISHED, Set.empty),
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
    articleType = ArticleType.TopicArticle
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
    draft16,
    draft17
  )

  val paul: Author                        = Author(ContributorType.Writer, "Truly Weird Rand Paul")
  val license: String                     = License.PublicDomain.toString
  val copyright: LearningpathCopyright    = common.learningpath.LearningpathCopyright(license, List(paul))
  val visibleMetadata: Option[Metadata]   = Some(Metadata(Seq.empty, visible = true, Map.empty))
  val invisibleMetadata: Option[Metadata] = Some(Metadata(Seq.empty, visible = false, Map.empty))

  val DefaultLearningPath: LearningPath = LearningPath(
    id = None,
    revision = Some(1),
    externalId = None,
    isBasedOn = None,
    title = List(),
    description = List(),
    coverPhotoId = None,
    duration = Some(0),
    status = LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = today,
    lastUpdated = today,
    tags = List(),
    owner = "owner",
    copyright = copyright,
    isMyNDLAOwner = false,
    responsible = None,
    comments = Seq.empty
  )

  val PenguinId   = 1L
  val BatmanId    = 2L
  val DonaldId    = 3L
  val UnrelatedId = 4L
  val EnglandoId  = 5L
  val KekId       = 6L

  val learningPath1: LearningPath = DefaultLearningPath.copy(
    id = Some(PenguinId),
    title = List(Title("Pingvinen er en kjeltring", "nb")),
    description = List(LPDescription("Dette handler om fugler", "nb")),
    duration = Some(1),
    lastUpdated = today.minusDays(34),
    tags = List(Tag(List("superhelt", "kanikkefly"), "nb"))
  )

  val learningPath2: LearningPath = DefaultLearningPath.copy(
    id = Some(BatmanId),
    title = List(Title("Batman er en tøff og morsom helt", "nb"), Title("Batman is a tough guy", "en")),
    description = List(LPDescription("Dette handler om flaggermus, som kan ligne litt på en fugl", "nb")),
    duration = Some(2),
    lastUpdated = today.minusDays(3),
    tags = List(Tag(Seq("superhelt", "kanfly"), "nb"))
  )

  val learningPath3: LearningPath = DefaultLearningPath.copy(
    id = Some(DonaldId),
    title = List(Title("Donald er en tøff, rar og morsom and", "nb"), Title("Donald is a weird duck", "en")),
    description = List(LPDescription("Dette handler om en and, som også minner om både flaggermus og fugler.", "nb")),
    duration = Some(3),
    lastUpdated = today.minusDays(4),
    tags = List(Tag(Seq("disney", "kanfly"), "nb"))
  )

  val learningPath4: LearningPath = DefaultLearningPath.copy(
    id = Some(UnrelatedId),
    title = List(Title("Unrelated", "en"), Title("Urelatert", "nb")),
    description = List(
      LPDescription("This is unrelated", "en"),
      LPDescription("Dette er en urelatert", "nb")
    ),
    duration = Some(4),
    lastUpdated = today.minusDays(5),
    tags = List()
  )

  val learningPath5: LearningPath = DefaultLearningPath.copy(
    id = Some(EnglandoId),
    title = List(Title("Englando", "en")),
    description = List(LPDescription("This is a englando learningpath", "en")),
    duration = Some(5),
    lastUpdated = today.minusDays(6),
    tags = List(),
    copyright = copyright.copy(contributors = List(Author(ContributorType.Writer, "Svims")))
  )

  val learningPath6: LearningPath = DefaultLearningPath.copy(
    id = Some(KekId),
    title = List(Title("Kek", "en")),
    description = List(LPDescription("This is kek", "en")),
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

  val core: Relevance = Relevance("urn:relevance:core", "Kjernestoff", List.empty)
  val supp: Relevance = Relevance("urn:relevance:supplementary", "Tilleggsstoff", List.empty)

  val relevances: List[Relevance] = List(core, supp)

  val rtLearningpath: ResourceType  = ResourceType("urn:resourcetype:learningpath", "Læringssti", None, List.empty)
  val academicArticle: ResourceType = ResourceType("urn:resourcetype:academicArticle", "Fagartikkel", None, List.empty)
  val guidance: ResourceType        = ResourceType("urn:resourcetype:guidance", "Veiledning", None, List.empty)
  val subjectMaterial: ResourceType =
    ResourceType("urn:resourcetype:subjectMaterial", "Fagstoff", Some(List(academicArticle, guidance)), List.empty)
  val nested: ResourceType = ResourceType("urn:resourcetype:nested", "SuperNested ResourceType", None, List.empty)
  val teacherEvaluation: ResourceType =
    ResourceType("urn:resourcetype:teacherEvaluation", "Lærervurdering", None, List.empty)
  val selfEvaluation: ResourceType = ResourceType("urn:resourcetype:selfEvaluation", "Egenvurdering", None, List.empty)
  val peerEvaluation: ResourceType =
    ResourceType("urn:resourcetype:peerEvaluation", "Medelevvurdering", Some(List(nested)), List.empty)
  val reviewResource: ResourceType = ResourceType(
    "urn:resourcetype:reviewResource",
    "Vurderingsressurs",
    Some(List(teacherEvaluation, selfEvaluation, peerEvaluation)),
    List.empty
  )
  val resourceTypes: List[ResourceType] = List(rtLearningpath, subjectMaterial, reviewResource)

  def generateContexts(
      node: Node,
      root: Node,
      parent: Node,
      resourceTypes: List[ResourceType],
      contextType: Option[String],
      relevance: Relevance,
      isPrimary: Boolean,
      isVisible: Boolean,
      isActive: Boolean
  ): List[TaxonomyContext] = {
    parent.contexts.map(context => {
      val path = s"${context.path}/${URI.create(node.id).getSchemeSpecificPart}"
      TaxonomyContext(
        publicId = node.id,
        rootId = root.id,
        root = SearchableLanguageValues(Seq(LanguageValue("nb", root.name))),
        path = path,
        breadcrumbs = SearchableLanguageList.addValue(context.breadcrumbs, parent.name),
        contextType = contextType,
        relevanceId = relevance.id,
        relevance = SearchableLanguageValues(Seq(LanguageValue("nb", relevance.name))),
        resourceTypes = resourceTypes.map(rt =>
          SearchableTaxonomyResourceType(rt.id, SearchableLanguageValues(Seq(LanguageValue("nb", rt.name))))
        ),
        parentIds = context.parentIds :+ parent.id,
        isPrimary = isPrimary,
        contextId = Random.alphanumeric.take(12).mkString,
        isVisible = parent.metadata.map(m => m.visible && isVisible).getOrElse(isVisible),
        isActive = isActive,
        url = path
      )
    })
  }

  val context_1: TaxonomyContext = TaxonomyContext(
    publicId = "urn:subject:1",
    rootId = "urn:subject:1",
    root = SearchableLanguageValues(Seq(LanguageValue("nb", "Matte"))),
    path = "/subject:1",
    breadcrumbs = SearchableLanguageList(Seq(LanguageValue("nb", Seq.empty))),
    contextType = None,
    relevanceId = core.id,
    relevance = SearchableLanguageValues(Seq.empty),
    resourceTypes = List.empty,
    parentIds = List.empty,
    isPrimary = true,
    contextId = "asdf1234",
    isVisible = true,
    isActive = true,
    url = "/f/matte/asdf1234"
  )
  val subject_1: Node = Node(
    context_1.publicId,
    "Matte",
    None,
    Some(context_1.path),
    Some(context_1.url),
    visibleMetadata,
    List.empty,
    NodeType.SUBJECT,
    List(context_1.contextId),
    Some(context_1),
    List(context_1)
  )
  val context_2: TaxonomyContext = TaxonomyContext(
    publicId = "urn:subject:2",
    rootId = "urn:subject:2",
    root = SearchableLanguageValues(Seq(LanguageValue("nb", "Historie"))),
    path = "/subject:2",
    breadcrumbs = SearchableLanguageList(Seq(LanguageValue("nb", Seq.empty))),
    contextType = None,
    relevanceId = core.id,
    relevance = SearchableLanguageValues(Seq.empty),
    resourceTypes = List.empty,
    parentIds = List.empty,
    isPrimary = true,
    contextId = "asdf1235",
    isVisible = true,
    isActive = true,
    url = "/f/historie/asdf1235"
  )
  val subject_2: Node = Node(
    context_2.publicId,
    "Historie",
    None,
    Some(context_2.path),
    Some(context_2.url),
    visibleMetadata,
    List.empty,
    NodeType.SUBJECT,
    List(context_2.contextId),
    Some(context_2),
    List(context_2)
  )
  val context_3: TaxonomyContext = TaxonomyContext(
    publicId = "urn:subject:3",
    rootId = "urn:subject:3",
    root = SearchableLanguageValues(Seq(LanguageValue("nb", "Religion"))),
    path = "/subject:3",
    breadcrumbs = SearchableLanguageList(Seq(LanguageValue("nb", Seq.empty))),
    contextType = None,
    relevanceId = core.id,
    relevance = SearchableLanguageValues(Seq.empty),
    resourceTypes = List.empty,
    parentIds = List.empty,
    isPrimary = true,
    contextId = "asdf1236",
    isVisible = false,
    isActive = true,
    url = "/f/religion/asdf1236"
  )
  val subject_3: Node = Node(
    context_3.publicId,
    "Religion",
    None,
    Some(context_3.path),
    Some(context_3.url),
    invisibleMetadata,
    List.empty,
    NodeType.SUBJECT,
    List(context_3.contextId),
    Some(context_3),
    List(context_3)
  )
  val topic_1: Node = Node(
    "urn:topic:1",
    article8.title.head.title,
    Some(s"urn:article:${article8.id.get}"),
    Some("/subject:1/topic:1"),
    Some("/e/baldur-har-mareritt/asdf1237"),
    visibleMetadata,
    List.empty,
    NodeType.TOPIC,
    List("asdf1237"),
    None,
    List.empty
  )
  topic_1.contexts =
    generateContexts(topic_1, subject_1, subject_1, List.empty, Some("topic-article"), core, true, true, true)
  val topic_2: Node = Node(
    "urn:topic:2",
    article9.title.head.title,
    Some(s"urn:article:${article9.id.get}"),
    Some("/subject:1/topic:1/topic:2"),
    Some("/e/en-baldur-har-mareritt-om-ragnarok/asdf1238"),
    visibleMetadata,
    List.empty,
    NodeType.TOPIC,
    List("asdf1238"),
    None,
    List.empty
  )
  topic_2.contexts =
    generateContexts(topic_2, subject_1, topic_1, List.empty, Some("topic-article"), core, true, true, true)
  val topic_3: Node = Node(
    "urn:topic:3",
    article10.title.head.title,
    Some(s"urn:article:${article10.id.get}"),
    Some("/subject:1/topic:3"),
    Some("/e/this-article-is-in-english/asdf1239"),
    visibleMetadata,
    List.empty,
    NodeType.TOPIC,
    List("asdf1239"),
    None,
    List.empty
  )
  topic_3.contexts =
    generateContexts(topic_3, subject_1, subject_1, List.empty, Some("topic-article"), core, true, true, true)
  val topic_4: Node = Node(
    "urn:topic:4",
    article11.title.head.title,
    Some(s"urn:article:${article11.id.get}"),
    Some("/subject:2/topic:4"),
    Some("/e/hamstere/asdf1240"),
    visibleMetadata,
    List.empty,
    NodeType.TOPIC,
    List("asdf1240"),
    None,
    List.empty
  )
  topic_4.contexts =
    generateContexts(topic_4, subject_2, subject_2, List.empty, Some("topic-article"), core, true, true, true)
  val topic_5: Node = Node(
    "urn:topic:5",
    draft15.title.head.title,
    Some(s"urn:article:${draft15.id.get}"),
    Some("/subject:3/topic:5"),
    Some("/e/engler-og-demoner/asdf1241"),
    invisibleMetadata,
    List.empty,
    NodeType.TOPIC,
    List("asdf1241"),
    None,
    List.empty
  )
  topic_5.contexts =
    generateContexts(topic_5, subject_3, subject_3, List.empty, Some("topic-article"), supp, true, true, true)
  val resource_1: Node = Node(
    "urn:resource:1",
    article1.title.head.title,
    Some(s"urn:article:${article1.id.get}"),
    Some("/subject:3/topic:5/resource:1"),
    Some("/r/batmen-er-pa-vift-med-en-bil/asdf1242"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1242"),
    None,
    List.empty
  )
  resource_1.contexts = generateContexts(
    resource_1,
    subject_3,
    topic_5,
    List(subjectMaterial),
    Some("standard"),
    core,
    true,
    true,
    true
  ) ++
    generateContexts(
      resource_1,
      subject_1,
      topic_1,
      List(subjectMaterial),
      Some("standard"),
      core,
      true,
      true,
      true
    ) ++
    generateContexts(
      resource_1,
      subject_2,
      topic_4,
      List(subjectMaterial),
      Some("standard"),
      core,
      true,
      true,
      false
    )
  val resource_2: Node = Node(
    "urn:resource:2",
    article2.title.head.title,
    Some(s"urn:article:${article2.id.get}"),
    Some("/subject:1/topic:1/resource:2"),
    Some("/r/pingvinen-er-ute-og-gar/asdf1243"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1243"),
    None,
    List.empty
  )
  resource_2.contexts = generateContexts(
    resource_2,
    subject_1,
    topic_1,
    List(subjectMaterial, academicArticle),
    Some("standard"),
    supp,
    true,
    true,
    true
  )
  val resource_3: Node = Node(
    "urn:resource:3",
    article3.title.head.title,
    Some(s"urn:article:${article3.id.get}"),
    Some("/subject:1/topic:3/resource:3"),
    Some("/r/donald-duck-kjorer-bil/asdf1244"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1244"),
    None,
    List.empty
  )
  resource_3.contexts = generateContexts(
    resource_3,
    subject_1,
    topic_3,
    List(subjectMaterial),
    Some("standard"),
    supp,
    true,
    true,
    true
  )
  val resource_4: Node = Node(
    "urn:resource:4",
    article4.title.head.title,
    Some(s"urn:article:${article4.id.get}"),
    Some("/subject:1/topic:1/topic:2/resource:4"),
    Some("/r/superman-er-ute-og-flyr/asdf1245"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1245"),
    None,
    List.empty
  )
  resource_4.contexts = generateContexts(
    resource_4,
    subject_1,
    topic_2,
    List(subjectMaterial),
    Some("standard"),
    supp,
    true,
    true,
    true
  )
  val resource_5: Node = Node(
    "urn:resource:5",
    article5.title.head.title,
    Some(s"urn:article:${article5.id.get}"),
    Some("/subject:2/topic:4/resource:5"),
    Some("/r/hulken-lofter-biler/asdf1246"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1246"),
    None,
    List.empty
  )
  resource_5.contexts = generateContexts(
    resource_5,
    subject_2,
    topic_4,
    List(academicArticle, subjectMaterial),
    Some("standard"),
    core,
    true,
    true,
    false
  ) ++
    generateContexts(
      resource_5,
      subject_1,
      topic_3,
      List(academicArticle, subjectMaterial),
      Some("standard"),
      core,
      true,
      true,
      true
    )
  val resource_6: Node = Node(
    "urn:resource:6",
    article6.title.head.title,
    Some(s"urn:article:${article6.id.get}"),
    Some("/subject:2/topic:4/resource:6"),
    Some("/r/loke-og-tor-forsoeker-aa-fange-midgaardsormen/asdf1247"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1247"),
    None,
    List.empty
  )
  resource_6.contexts = generateContexts(
    resource_6,
    subject_2,
    topic_4,
    List(subjectMaterial),
    Some("standard"),
    core,
    true,
    true,
    true
  )
  val resource_7: Node = Node(
    "urn:resource:7",
    article7.title.head.title,
    Some(s"urn:article:${article7.id.get}"),
    Some("/subject:2/topic:4/resource:7"),
    Some("/r/yggdrasil-livets-tre/asdf1248"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1248"),
    None,
    List.empty
  )
  resource_7.contexts = generateContexts(
    resource_7,
    subject_2,
    topic_4,
    List(guidance, subjectMaterial, nested, peerEvaluation, reviewResource),
    Some("standard"),
    core,
    true,
    true,
    false
  )
  val resource_8: Node = Node(
    "urn:resource:8",
    learningPath1.title.head.title,
    Some(s"urn:learningpath:${learningPath1.id.get}"),
    Some("/subject:1/topic:1/resource:8"),
    Some("/r/pingvinen-er-en-kjeltring/asdf1249"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1249"),
    None,
    List.empty
  )
  resource_8.contexts = generateContexts(
    resource_8,
    subject_1,
    topic_1,
    List(rtLearningpath),
    Some("learningpath"),
    supp,
    true,
    true,
    true
  )
  val resource_9: Node = Node(
    "urn:resource:9",
    learningPath2.title.head.title,
    Some(s"urn:learningpath:${learningPath2.id.get}"),
    Some("/subject:1/topic:1/resource:9"),
    Some("/r/batman-er-en-toeff-og-morsom-helt/asdf1250"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1250"),
    None,
    List.empty
  )
  resource_9.contexts = generateContexts(
    resource_9,
    subject_1,
    topic_1,
    List(rtLearningpath),
    Some("learningpath"),
    core,
    true,
    true,
    true
  )
  val resource_10: Node = Node(
    "urn:resource:10",
    learningPath3.title.head.title,
    Some(s"urn:learningpath:${learningPath3.id.get}"),
    Some("/subject:1/topic:3/resource:10"),
    Some("/r/donald-er-en-toeff-rar-og-morsom-and/asdf1251"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1251"),
    None,
    List.empty
  )
  resource_10.contexts = generateContexts(
    resource_10,
    subject_1,
    topic_3,
    List(rtLearningpath),
    Some("learningpath"),
    core,
    true,
    true,
    true
  )
  val resource_11: Node = Node(
    "urn:resource:11",
    learningPath4.title.head.title,
    Some(s"urn:learningpath:${learningPath4.id.get}"),
    Some("/subject:1/topic:1/topic:2/resource:11"),
    Some("/r/unrelated/asdf1252"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1252"),
    None,
    List.empty
  )
  resource_11.contexts = generateContexts(
    resource_11,
    subject_1,
    topic_2,
    List(rtLearningpath),
    Some("learningpath"),
    supp,
    true,
    true,
    true
  )
  val resource_12: Node = Node(
    "urn:resource:12",
    learningPath5.title.head.title,
    Some(s"urn:learningpath:${learningPath5.id.get}"),
    Some("/subject:2/topic:4/resource:12"),
    Some("/r/englando/asdf1253"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1253"),
    None,
    List.empty
  )
  resource_12.contexts = generateContexts(
    resource_12,
    subject_2,
    topic_4,
    List(rtLearningpath),
    Some("learningpath"),
    supp,
    true,
    true,
    true
  )
  val resource_13: Node = Node(
    "urn:resource:13",
    article12.title.head.title,
    Some(s"urn:article:${article12.id.get}"),
    Some("/subject:2/topic:4/resource:13"),
    Some("/r/ekstrastoff/asdf1254"),
    visibleMetadata,
    List.empty,
    NodeType.RESOURCE,
    List("asdf1254", "asdf1255"), // asdf1255 is a deleted context
    None,
    List.empty
  )
  resource_13.contexts = generateContexts(
    resource_13,
    subject_1,
    topic_1,
    List(subjectMaterial),
    Some("standard"),
    core,
    true,
    true,
    true
  ) ++
    generateContexts(
      resource_13,
      subject_2,
      topic_4,
      List(subjectMaterial),
      Some("standard"),
      supp,
      true,
      true,
      true
    )

  val nodes: List[Node] = List(
    subject_1,
    subject_2,
    subject_3,
    topic_1,
    topic_2,
    topic_3,
    topic_4,
    topic_5,
    resource_1,
    resource_2,
    resource_3,
    resource_4,
    resource_5,
    resource_6,
    resource_7,
    resource_8,
    resource_9,
    resource_10,
    resource_11,
    resource_12,
    resource_13
  )

  val taxonomyTestBundle: TaxonomyBundle = TaxonomyBundle(nodes = nodes)

  val myndlaTestBundle: MyNDLABundleDTO = MyNDLABundleDTO(Map.empty)

  val emptyGrepBundle: GrepBundle = GrepBundle(
    kjerneelementer = List.empty,
    kompetansemaal = List.empty,
    kompetansemaalsett = List.empty,
    tverrfagligeTemaer = List.empty,
    laereplaner = List.empty
  )

  val grepBundle: GrepBundle = emptyGrepBundle.copy(
    kjerneelementer = List(
      GrepKjerneelement(
        kode = "KE12",
        GrepStatusDTO.Published,
        tittel = GrepTextObj(List(GrepTitle("default", "Utforsking og problemløysing"))),
        beskrivelse = GrepTextObj(List(GrepTitle("default", ""))),
        `tilhoerer-laereplan` = BelongsToObj("LP1", GrepStatusDTO.Published, "Dette er LP1")
      ),
      GrepKjerneelement(
        kode = "KE34",
        GrepStatusDTO.Published,
        tittel = GrepTextObj(List(GrepTitle("default", "Abstraksjon og generalisering"))),
        beskrivelse = GrepTextObj(List(GrepTitle("default", ""))),
        `tilhoerer-laereplan` = BelongsToObj("LP1", GrepStatusDTO.Published, "Dette er LP2")
      )
    ),
    kompetansemaal = List(
      GrepKompetansemaal(
        kode = "KM123",
        GrepStatusDTO.Published,
        tittel = GrepTextObj(
          List(GrepTitle("default", "bruke ulike kilder på en kritisk, hensiktsmessig og etterrettelig måte"))
        ),
        `tilhoerer-laereplan` = BelongsToObj("LP1", GrepStatusDTO.Published, "Dette er LP1"),
        `tilhoerer-kompetansemaalsett` = BelongsToObj(
          "KMS1",
          GrepStatusDTO.Published,
          "Dette er KMS1"
        ),
        `tilknyttede-tverrfaglige-temaer` = List(),
        `tilknyttede-kjerneelementer` = List(),
        `gjenbruk-av` = None
      )
    ),
    tverrfagligeTemaer = List(
      GrepTverrfagligTema(
        "TT2",
        GrepStatusDTO.Published,
        Seq(GrepTitle("default", "Demokrati og medborgerskap"))
      )
    ),
    laereplaner = List(
      GrepLaererplan(
        "LP1",
        GrepStatusDTO.Published,
        GrepTextObj(List(GrepTitle("default", "Læreplan i norsk (NOR01-04)"))),
        `erstattes-av` = List.empty
      )
    )
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
    traits = List.empty,
    shouldScroll = false,
    filterByNoResourceType = false,
    aggregatePaths = List.empty,
    embedResource = List.empty,
    embedId = None,
    availability = List.empty,
    articleTypes = List.empty,
    filterInactive = false,
    resultTypes = None,
    nodeTypeFilter = List.empty
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
    traits = List.empty,
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
    articleTypes = List.empty,
    filterInactive = false,
    prioritized = None,
    priority = List.empty,
    publishedFilterFrom = None,
    publishedFilterTo = None,
    resultTypes = None
  )

  val searchableResourceTypes: List[SearchableTaxonomyResourceType] = List(
    SearchableTaxonomyResourceType(
      "urn:resourcetype:subjectMaterial",
      SearchableLanguageValues(Seq(LanguageValue("nb", "Fagstoff")))
    ),
    SearchableTaxonomyResourceType(
      "urn:resourcetype:academicArticle",
      SearchableLanguageValues(Seq(LanguageValue("nb", "Fagartikkel")))
    )
  )

  val singleSearchableTaxonomyContext: SearchableTaxonomyContext =
    SearchableTaxonomyContext(
      publicId = "urn:resource:101",
      contextId = "contextId",
      rootId = "urn:subject:1",
      path = "/subject:3/topic:1/topic:151/resource:101",
      breadcrumbs = SearchableLanguageList(
        Seq(
          LanguageValue("nb", Seq("Matte", "Østen for solen", "Vesten for månen"))
        )
      ),
      contextType = LearningResourceType.Article.toString,
      relevanceId = "urn:relevance:core",
      resourceTypeIds = searchableResourceTypes.map(_.id),
      parentIds = List("urn:topic:1"),
      isPrimary = true,
      isActive = true,
      isVisible = true,
      url = "/subject:3/topic:1/topic:151/resource:101",
      domainObject = TaxonomyContext(
        publicId = "urn:resource:101",
        rootId = "urn:subject:1",
        root = SearchableLanguageValues(Seq(LanguageValue("nb", "Matte"))),
        path = "/subject:3/topic:1/topic:151/resource:101",
        breadcrumbs = SearchableLanguageList(
          Seq(
            LanguageValue("nb", Seq("Matte", "Østen for solen", "Vesten for månen"))
          )
        ),
        contextType = Some(LearningResourceType.Article.toString),
        relevanceId = "urn:relevance:core",
        relevance = SearchableLanguageValues(Seq(LanguageValue("nb", "Kjernestoff"))),
        resourceTypes = resourceTypes.map(rt =>
          SearchableTaxonomyResourceType(rt.id, SearchableLanguageValues(Seq(LanguageValue("nb", rt.name))))
        ),
        parentIds = List("urn:topic:1"),
        isPrimary = true,
        contextId = Random.alphanumeric.take(12).mkString,
        isVisible = true,
        isActive = true,
        url = "/subject:3/topic:1/topic:151/resource:101"
      )
    )

  val searchableTaxonomyContexts: List[SearchableTaxonomyContext] = List(
    singleSearchableTaxonomyContext
  )

  val searchableTitles: SearchableLanguageValues = SearchableLanguageValues.from(
    "nb" -> "Christian Tut",
    "en" -> "Christian Honk"
  )

  val searchableContents: SearchableLanguageValues = SearchableLanguageValues.from(
    "nn" -> "Eg kjøyrar rundt i min fine bil",
    "nb" -> "Jeg kjører rundt i tutut",
    "en" -> "I'm in my mums car wroomwroom"
  )

  val searchableVisualElements: SearchableLanguageValues = SearchableLanguageValues.from(
    "nn" -> "image",
    "nb" -> "image"
  )

  val searchableIntroductions: SearchableLanguageValues    = SearchableLanguageValues.from("en" -> "Wroom wroom")
  val searchableMetaDescriptions: SearchableLanguageValues = SearchableLanguageValues.from("nb" -> "Mammas bil")
  val searchableTags: SearchableLanguageList       = SearchableLanguageList.from("en" -> Seq("Mum", "Car", "Wroom"))
  val searchableEmbedAttrs: SearchableLanguageList = SearchableLanguageList.from(
    "nb" -> Seq("En norsk", "To norsk"),
    "en" -> Seq("One english")
  )

  val searchableEmbedResourcesAndIds: List[EmbedValues] = List(
    EmbedValues(resource = Some("test resource 1"), id = List("test id 1"), language = "nb")
  )

  val olddate: NDLADate = today.minusDays(5)

  val searchableRevisionMeta: List[RevisionMeta] = List(
    RevisionMeta(
      id = UUID.randomUUID(),
      revisionDate = today,
      note = "some note",
      status = RevisionStatus.NeedsRevision
    ),
    RevisionMeta(
      id = UUID.randomUUID(),
      revisionDate = olddate,
      note = "some other note",
      status = RevisionStatus.NeedsRevision
    )
  )
  val searchableDraft: SearchableDraft = SearchableDraft(
    id = 100,
    title = searchableTitles,
    content = searchableContents,
    introduction = searchableIntroductions,
    metaDescription = searchableMetaDescriptions,
    tags = searchableTags,
    lastUpdated = TestData.today,
    license = Some(License.CC_BY_SA.toString),
    authors = List("Jonas", "Papi"),
    articleType = LearningResourceType.Article.toString,
    defaultTitle = Some("Christian Tut"),
    supportedLanguages = List("en", "nb", "nn"),
    notes = List("Note1", "note2"),
    None,
    contexts = searchableTaxonomyContexts,
    contextids = searchableTaxonomyContexts.map(_.contextId),
    draftStatus = SearchableStatus(DraftStatus.PLANNED.toString, Seq(DraftStatus.IN_PROGRESS.toString)),
    status = DraftStatus.PLANNED.toString,
    users = List("ndalId54321", "ndalId12345"),
    previousVersionsNotes = List("OldNote"),
    grepContexts = List(),
    traits = List.empty,
    embedAttributes = searchableEmbedAttrs,
    embedResourcesAndIds = searchableEmbedResourcesAndIds,
    revisionMeta = searchableRevisionMeta,
    nextRevision = searchableRevisionMeta.lastOption,
    responsible = Some(Responsible("some responsible", TestData.today)),
    domainObject = TestData.draft1.copy(
      status = Status(DraftStatus.IN_PROGRESS, Set(DraftStatus.PUBLISHED)),
      notes = Seq(
        EditorNote(
          note = "Hei",
          user = "user",
          timestamp = TestData.today,
          status = Status(
            current = DraftStatus.IN_PROGRESS,
            other = Set(DraftStatus.PUBLISHED)
          )
        )
      )
    ),
    priority = Priority.Unspecified,
    defaultParentTopicName = searchableTitles.defaultValue,
    parentTopicName = searchableTitles,
    defaultRoot = searchableTitles.defaultValue,
    primaryRoot = searchableTitles,
    resourceTypeName = searchableTitles,
    defaultResourceTypeName = searchableTitles.defaultValue,
    published = TestData.today,
    favorited = 0,
    learningResourceType = LearningResourceType.Article,
    typeName = List()
  )

  val sampleNbDomainConcept: Concept = Concept(
    id = Some(1),
    revision = Some(1),
    title = Seq(common.Title("Tittel", "nb")),
    content = Seq(ConceptContent("Innhold", "nb")),
    copyright = None,
    created = today,
    updated = today,
    updatedBy = Seq("noen"),
    tags = Seq(common.Tag(Seq("stor", "kaktus"), "nb")),
    status = no.ndla.common.model.domain.concept.Status(
      ConceptStatus.LANGUAGE,
      Set(ConceptStatus.PUBLISHED)
    ),
    visualElement = Seq(
      no.ndla.common.model.domain.concept.VisualElement(
        s"""<$EmbedTagName data-caption="some capt" data-align="" data-resource_id="1" data-resource="image" data-alt="some alt" data-size="full"></$EmbedTagName>""",
        "nb"
      )
    ),
    responsible = Some(Responsible("some-id", today)),
    conceptType = ConceptType.CONCEPT,
    glossData = Some(
      GlossData(
        gloss = "hei",
        wordClass = WordClass.TIME_WORD,
        originalLanguage = "nb",
        transcriptions = Map("pling" -> "plong"),
        examples = List(
          List(
            GlossExample(
              example = "hei",
              language = "nb",
              transcriptions = Map("nb" -> "lai")
            )
          )
        )
      )
    ),
    editorNotes = Seq(
      ConceptEditorNote(
        note = "hei",
        user = "some-id",
        status = no.ndla.common.model.domain.concept.Status(ConceptStatus.LANGUAGE, Set(ConceptStatus.PUBLISHED)),
        timestamp = today
      )
    )
  )

}
