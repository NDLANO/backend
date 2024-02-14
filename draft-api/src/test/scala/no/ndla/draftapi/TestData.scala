/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model
import no.ndla.common.model.api.{DraftCopyright, Missing}
import no.ndla.common.model.domain.Priority
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.draft.DraftStatus._
import no.ndla.common.model.{NDLADate, api => commonApi, domain => common}
import no.ndla.draftapi.integration.{LearningPath, Title}
import no.ndla.draftapi.model.api._
import no.ndla.draftapi.model.{api, domain}
import no.ndla.mapping.License.{CC_BY, CC_BY_NC_SA}
import no.ndla.network.tapir.auth.Permission.{DRAFT_API_ADMIN, DRAFT_API_PUBLISH, DRAFT_API_WRITE}
import no.ndla.network.tapir.auth.TokenUser

object TestData {

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJkcmFmdHM6d3JpdGUiXSwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.5jpF98NxQZlkQQ5-rxVO3oTkNOQRQLDlAexyDnLiZFY"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6W10sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.vw9YhRtgUQr_vuDhLNHfBsZz-4XLhCc1Kwxi0w0_qGI"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJjb25jZXB0OndyaXRlIl0sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.RAP-ab3l9qPOpYQRreqLi_RRmgybk-G_VKRPHIOqq5A"

  val authHeaderWithAllRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJkcmFmdHM6d3JpdGUiLCJkcmFmdHM6cHVibGlzaCIsImRyYWZ0czpodG1sIiwiZHJhZnRzOmFkbWluIiwiYXJ0aWNsZXM6d3JpdGUiLCJhcnRpY2xlczpwdWJsaXNoIl0sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.0HD_oOqKSMNopVF9zZpRr0guIweNB9v0Yi9kyWrH5DE"

  val userWithNoRoles: TokenUser       = TokenUser("unit test", Set.empty, None)
  val userWithWriteAccess: TokenUser   = TokenUser("unit test", Set(DRAFT_API_WRITE), None)
  val userWithPublishAccess: TokenUser = TokenUser("unit test", Set(DRAFT_API_WRITE, DRAFT_API_PUBLISH), None)
  val userWithAdminAccess: TokenUser =
    TokenUser("unit test", Set(DRAFT_API_WRITE, DRAFT_API_PUBLISH, DRAFT_API_ADMIN), None)

  val publicDomainCopyright: common.draft.DraftCopyright =
    common.draft.DraftCopyright(Some("publicdomain"), Some(""), List.empty, List(), List(), None, None, false)
  private val byNcSaCopyright = common.draft.DraftCopyright(
    Some(CC_BY_NC_SA.toString),
    Some("Gotham City"),
    List(common.Author("Forfatter", "DC Comics")),
    List(),
    List(),
    None,
    None,
    false
  )
  private val copyrighted = common.draft.DraftCopyright(
    Some("copyrighted"),
    Some("New York"),
    List(common.Author("Forfatter", "Clark Kent")),
    List(),
    List(),
    None,
    None,
    false
  )
  val today: NDLADate = NDLADate.now()

  val (articleId, externalId) = (1L, "751234")

  val sampleArticleV2: api.Article = api.Article(
    id = 1,
    oldNdlaUrl = None,
    revision = 1,
    status = api.Status(PLANNED.toString, Seq.empty),
    title = Some(api.ArticleTitle("title", "title", "nb")),
    content = Some(api.ArticleContent("this is content", "nb")),
    copyright = Some(
      DraftCopyright(
        Some(commonApi.License("licence", None, None)),
        Some("origin"),
        Seq(commonApi.Author("developer", "Per")),
        List(),
        List(),
        None,
        None,
        false
      )
    ),
    tags = Some(api.ArticleTag(Seq("tag"), "nb")),
    requiredLibraries = Seq(api.RequiredLibrary("JS", "JavaScript", "url")),
    visualElement = None,
    introduction = None,
    metaDescription = Some(api.ArticleMetaDescription("metaDesc", "nb")),
    None,
    created = NDLADate.of(2017, 1, 1, 12, 15, 32),
    updated = NDLADate.of(2017, 4, 1, 12, 15, 32),
    updatedBy = "me",
    published = NDLADate.of(2017, 4, 1, 12, 15, 32),
    articleType = "standard",
    supportedLanguages = Seq("nb"),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    availability = "everyone",
    Seq.empty,
    Seq.empty,
    responsible = None,
    None,
    Seq.empty,
    priority = Priority.Unspecified.entryName,
    started = false,
    prioritized = false
  )

  val blankUpdatedArticle: UpdatedArticle = api.UpdatedArticle(
    revision = 1,
    language = None,
    title = None,
    status = None,
    published = None,
    content = None,
    tags = None,
    introduction = None,
    metaDescription = None,
    metaImage = Missing,
    visualElement = None,
    copyright = None,
    requiredLibraries = None,
    articleType = None,
    notes = None,
    editorLabels = None,
    grepCodes = None,
    conceptIds = None,
    createNewVersion = None,
    availability = None,
    relatedContent = None,
    revisionMeta = None,
    responsibleId = Missing,
    slug = None,
    comments = None,
    prioritized = None,
    priority = None
  )

  val sampleApiUpdateArticle: UpdatedArticle = blankUpdatedArticle.copy(
    revision = 1,
    language = Some("nb"),
    title = Some("tittel")
  )

  val articleHit1: String = """
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

  val apiArticleV2: api.Article = api.Article(
    articleId,
    Some(s"//red.ndla.no/node/$externalId"),
    2,
    api.Status(PLANNED.toString, Seq.empty),
    Some(api.ArticleTitle("title", "title", "nb")),
    Some(api.ArticleContent("content", "nb")),
    Some(
      model.api.DraftCopyright(
        Some(
          commonApi.License(
            CC_BY.toString,
            Some("Creative Commons Attribution 4.0 International"),
            Some("https://creativecommons.org/licenses/by/4.0/")
          )
        ),
        Some(""),
        Seq.empty,
        List(),
        List(),
        None,
        None,
        false
      )
    ),
    None,
    Seq.empty,
    None,
    None,
    Some(api.ArticleMetaDescription("meta description", "nb")),
    None,
    today,
    today,
    "ndalId54321",
    today,
    "standard",
    Seq("nb"),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    availability = "everyone",
    Seq.empty,
    Seq.empty,
    None,
    None,
    Seq.empty,
    false,
    Priority.Unspecified.entryName,
    false
  )

  val apiArticleUserTest: api.Article = api.Article(
    articleId,
    Some(s"//red.ndla.no/node/$externalId"),
    2,
    api.Status(EXTERNAL_REVIEW.toString, Seq.empty),
    Some(api.ArticleTitle("title", "title", "nb")),
    Some(api.ArticleContent("content", "nb")),
    Some(
      model.api.DraftCopyright(
        Some(
          commonApi.License(
            CC_BY.toString,
            Some("Creative Commons Attribution 4.0 International"),
            Some("https://creativecommons.org/licenses/by/4.0/")
          )
        ),
        Some(""),
        Seq.empty,
        List(),
        List(),
        None,
        None,
        false
      )
    ),
    None,
    Seq.empty,
    None,
    None,
    Some(api.ArticleMetaDescription("meta description", "nb")),
    None,
    today,
    today,
    "ndalId54321",
    today,
    "standard",
    Seq("nb"),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    availability = "everyone",
    Seq.empty,
    Seq.empty,
    None,
    None,
    Seq.empty,
    false,
    Priority.Unspecified.entryName,
    false
  )

  val sampleTopicArticle: Draft = Draft(
    Option(1),
    Option(1),
    common.Status(PLANNED, Set.empty),
    Seq(common.Title("test", "en")),
    Seq(common.ArticleContent("<section><div>test</div></section>", "en")),
    Some(publicDomainCopyright),
    Seq.empty,
    Seq.empty,
    Seq(common.VisualElement("image", "en")),
    Seq(common.Introduction("This is an introduction", "en")),
    Seq.empty,
    Seq.empty,
    NDLADate.now().minusDays(4).withNano(0),
    NDLADate.now().minusDays(2).withNano(0),
    userWithWriteAccess.id,
    NDLADate.now().minusDays(2).withNano(0),
    common.ArticleType.TopicArticle,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    common.Availability.everyone,
    Seq.empty,
    Seq.empty,
    None,
    None,
    Seq.empty,
    Priority.Unspecified,
    false
  )

  val sampleArticleWithPublicDomain: Draft = Draft(
    Option(1),
    Option(1),
    common.Status(PLANNED, Set.empty),
    Seq(common.Title("test", "en")),
    Seq(common.ArticleContent("<section><div>test</div></section>", "en")),
    Some(publicDomainCopyright),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq(common.Introduction("This is an introduction", "en")),
    Seq.empty,
    Seq.empty,
    NDLADate.now().minusDays(4).withNano(0),
    NDLADate.now().minusDays(2).withNano(0),
    userWithWriteAccess.id,
    NDLADate.now().minusDays(2).withNano(0),
    common.ArticleType.Standard,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    common.Availability.everyone,
    Seq.empty,
    common.draft.RevisionMeta.default,
    None,
    None,
    Seq.empty,
    Priority.Unspecified,
    false
  )

  val sampleDomainArticle: Draft = Draft(
    Option(articleId),
    Option(2),
    common.Status(PLANNED, Set.empty),
    Seq(common.Title("title", "nb")),
    Seq(common.ArticleContent("content", "nb")),
    Some(
      common.draft.DraftCopyright(Some(CC_BY.toString), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, false)
    ),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq(common.Description("meta description", "nb")),
    Seq.empty,
    today,
    today,
    "ndalId54321",
    today,
    common.ArticleType.Standard,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    common.Availability.everyone,
    Seq.empty,
    Seq.empty,
    None,
    None,
    Seq.empty,
    Priority.Unspecified,
    false
  )

  val newArticle: NewArticle = api.NewArticle(
    "en",
    "test",
    Some(today),
    Some("<article><div>test</div></article>"),
    Seq.empty,
    None,
    None,
    None,
    None,
    Some(
      model.api.DraftCopyright(
        Some(commonApi.License("publicdomain", None, None)),
        Some(""),
        Seq.empty,
        Seq.empty,
        Seq.empty,
        None,
        None,
        false
      )
    ),
    Seq.empty,
    "standard",
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    availability = None,
    Seq.empty,
    None,
    None,
    None,
    List.empty,
    None,
    None
  )

  val sampleArticleWithByNcSa: Draft =
    sampleArticleWithPublicDomain.copy(copyright = Some(byNcSaCopyright))
  val sampleArticleWithCopyrighted: Draft =
    sampleArticleWithPublicDomain.copy(copyright = Some(copyrighted))

  val sampleDomainArticleWithHtmlFault: Draft = Draft(
    id = Option(articleId),
    revision = Option(2),
    status = common.Status(PLANNED, Set.empty),
    title = Seq(common.Title("test", "en")),
    content = Seq(
      common.ArticleContent(
        """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
          |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
          |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
          |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
    """.stripMargin,
        "en"
      )
    ),
    copyright = Some(
      common.draft.DraftCopyright(Some("publicdomain"), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, false)
    ),
    tags = Seq.empty,
    requiredLibraries = Seq.empty,
    visualElement = Seq.empty,
    introduction = Seq.empty,
    metaDescription = Seq(common.Description("meta description", "nb")),
    metaImage = Seq.empty,
    created = today,
    updated = today,
    updatedBy = "ndalId54321",
    published = today,
    articleType = common.ArticleType.Standard,
    notes = Seq.empty,
    previousVersionsNotes = Seq.empty,
    editorLabels = Seq.empty,
    grepCodes = Seq.empty,
    conceptIds = Seq.empty,
    availability = common.Availability.everyone,
    relatedContent = Seq.empty,
    revisionMeta = Seq.empty,
    responsible = None,
    slug = None,
    comments = Seq.empty,
    priority = Priority.Unspecified,
    started = false
  )

  val apiArticleWithHtmlFaultV2: api.Article = api.Article(
    id = 1,
    oldNdlaUrl = None,
    revision = 1,
    status = api.Status(PLANNED.toString, Seq.empty),
    title = Some(api.ArticleTitle("test", "title", "en")),
    content = Some(
      api.ArticleContent(
        """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
          |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
          |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
          |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin,
        "en"
      )
    ),
    copyright = Some(
      model.api.DraftCopyright(
        Some(commonApi.License("publicdomain", None, None)),
        Some(""),
        Seq.empty,
        Seq.empty,
        Seq.empty,
        None,
        None,
        false
      )
    ),
    tags = Some(api.ArticleTag(Seq.empty, "en")),
    requiredLibraries = Seq.empty,
    visualElement = None,
    introduction = None,
    metaDescription = Some(api.ArticleMetaDescription("so meta", "en")),
    metaImage = None,
    created = NDLADate.now().minusDays(4),
    updated = NDLADate.now().minusDays(2),
    updatedBy = "ndalId54321",
    published = NDLADate.now().minusDays(2),
    articleType = "standard",
    supportedLanguages = Seq("en"),
    notes = Seq.empty,
    editorLabels = Seq.empty,
    grepCodes = Seq.empty,
    conceptIds = Seq.empty,
    availability = "everyone",
    relatedContent = Seq.empty,
    revisions = Seq.empty,
    responsible = None,
    slug = None,
    comments = Seq.empty,
    prioritized = false,
    priority = Priority.Unspecified.entryName,
    started = false
  )

  val (nodeId, nodeId2)         = ("1234", "4321")
  val sampleTitle: common.Title = common.Title("title", "en")

  val visualElement: common.VisualElement = common.VisualElement(
    s"""<$EmbedTagName data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />""",
    "nb"
  )

  val emptyDomainUserData: domain.UserData =
    domain.UserData(
      id = None,
      userId = "",
      savedSearches = None,
      latestEditedArticles = None,
      favoriteSubjects = None,
      latestEditedConcepts = None
    )

  val emptyApiUserData: api.UserData =
    api.UserData(
      userId = "",
      savedSearches = None,
      latestEditedArticles = None,
      favoriteSubjects = None,
      latestEditedConcepts = None
    )

  val statusWithPublished: common.Status      = common.Status(PUBLISHED, Set.empty)
  val statusWithPlanned: common.Status        = common.Status(PLANNED, Set.empty)
  val statusWithInProcess: common.Status      = common.Status(IN_PROGRESS, Set.empty)
  val statusWithExternalReview: common.Status = common.Status(EXTERNAL_REVIEW, Set.empty)
  val statusWithInternalReview: common.Status = common.Status(INTERNAL_REVIEW, Set.empty)
  val statusWithEndControl: common.Status     = common.Status(END_CONTROL, Set.empty)

  val sampleLearningPath: LearningPath = LearningPath(1, Title("Title", "nb"))

  val sampleApiGrepCodesSearchResult: GrepCodesSearchResult = api.GrepCodesSearchResult(10, 1, 1, Seq("a", "b"))
  val sampleApiTagsSearchResult: TagsSearchResult           = api.TagsSearchResult(10, 1, 1, "nb", Seq("a", "b"))

  val searchSettings: domain.SearchSettings = domain.SearchSettings(
    query = None,
    withIdIn = List.empty,
    searchLanguage = "nb",
    license = None,
    page = 1,
    pageSize = 10,
    sort = domain.Sort.ByIdAsc,
    articleTypes = Seq.empty,
    fallback = false,
    grepCodes = Seq.empty,
    shouldScroll = false
  )

  val agreementSearchSettings: domain.AgreementSearchSettings = domain.AgreementSearchSettings(
    query = None,
    withIdIn = List.empty,
    license = None,
    page = 1,
    pageSize = 10,
    sort = domain.Sort.ByIdAsc,
    shouldScroll = false
  )
}
