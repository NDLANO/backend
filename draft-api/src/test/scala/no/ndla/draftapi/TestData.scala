/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import no.ndla.common.model.{domain => common}
import no.ndla.common.model.domain.draft.DraftStatus._
import no.ndla.draftapi.auth.{Role, UserInfo}
import no.ndla.draftapi.integration.{LearningPath, Title}
import no.ndla.draftapi.model.api.{GrepCodesSearchResult, NewAgreement, NewArticle, TagsSearchResult, UpdatedArticle}
import no.ndla.draftapi.model.{api, domain}
import no.ndla.mapping.License.{CC_BY, CC_BY_NC_SA, CC_BY_SA}

import java.time.LocalDateTime

object TestData {

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiZHJhZnRzLXRlc3Q6d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.i_wvbN24VZMqOTQPiEqvqKZy23-m-2ZxTligof8n33k3z-BjXqn4bhKTv7sFdQG9Wf9TFx8UzjoOQ6efQgpbRzl8blZ-6jAZOy6xDjDW0dIwE0zWD8riG8l27iQ88fbY_uCyIODyYp2JNbVmWZNJ9crKKevKmhcXvMRUTrcyE9g"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.fb9eTuBwIlbGDgDKBQ5FVpuSUdgDVBZjCenkOrWLzUByVCcaFhbFU8CVTWWKhKJqt6u-09-99hh86szURLqwl3F5rxSX9PrnbyhI9LsPut_3fr6vezs6592jPJRbdBz3-xLN0XY5HIiJElJD3Wb52obTqJCrMAKLZ5x_GLKGhcY"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoic29tZTpvdGhlciIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.Hbmh9KX19nx7yT3rEcP9pyzRO0uQJBRucfqH9QEZtLyXjYj_fAyOhsoicOVEbHSES7rtdiJK43-gijSpWWmGWOkE6Ym7nHGhB_nLdvp_25PDgdKHo-KawZdAyIcJFr5_t3CJ2Z2IPVbrXwUd99vuXEBaV0dMwkT0kDtkwHuS-8E"

  val authHeaderWithAllRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXJ0aWNsZXMtdGVzdDpwdWJsaXNoIGRyYWZ0cy10ZXN0OndyaXRlIGRyYWZ0cy10ZXN0OnNldF90b19wdWJsaXNoIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"

  val userWithNoRoles: UserInfo       = UserInfo("unit test", Set.empty)
  val userWithWriteAccess: UserInfo   = UserInfo("unit test", Set(Role.WRITE))
  val userWithPublishAccess: UserInfo = UserInfo("unit test", Set(Role.WRITE, Role.PUBLISH))
  val userWithAdminAccess: UserInfo   = UserInfo("unit test", Set(Role.WRITE, Role.PUBLISH, Role.ADMIN))

  val publicDomainCopyright: common.draft.Copyright =
    common.draft.Copyright(Some("publicdomain"), Some(""), List.empty, List(), List(), None, None, None)
  private val byNcSaCopyright = common.draft.Copyright(
    Some(CC_BY_NC_SA.toString),
    Some("Gotham City"),
    List(common.Author("Forfatter", "DC Comics")),
    List(),
    List(),
    None,
    None,
    None
  )
  private val copyrighted = common.draft.Copyright(
    Some("copyrighted"),
    Some("New York"),
    List(common.Author("Forfatter", "Clark Kent")),
    List(),
    List(),
    None,
    None,
    None
  )
  val today: LocalDateTime = LocalDateTime.now()

  val (articleId, externalId) = (1, "751234")

  val sampleArticleV2: api.Article = api.Article(
    id = 1,
    oldNdlaUrl = None,
    revision = 1,
    status = api.Status(DRAFT.toString, Seq.empty),
    title = Some(api.ArticleTitle("title", "nb")),
    content = Some(api.ArticleContent("this is content", "nb")),
    copyright = Some(
      api.Copyright(
        Some(api.License("licence", None, None)),
        Some("origin"),
        Seq(api.Author("developer", "Per")),
        List(),
        List(),
        None,
        None,
        None
      )
    ),
    tags = Some(api.ArticleTag(Seq("tag"), "nb")),
    requiredLibraries = Seq(api.RequiredLibrary("JS", "JavaScript", "url")),
    visualElement = None,
    introduction = None,
    metaDescription = Some(api.ArticleMetaDescription("metaDesc", "nb")),
    None,
    created = LocalDateTime.of(2017, 1, 1, 12, 15, 32),
    updated = LocalDateTime.of(2017, 4, 1, 12, 15, 32),
    updatedBy = "me",
    published = LocalDateTime.of(2017, 4, 1, 12, 15, 32),
    articleType = "standard",
    supportedLanguages = Seq("nb"),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    availability = "everyone",
    Seq.empty,
    Seq.empty,
    responsible = None
  )

  val blankUpdatedArticle: UpdatedArticle = api.UpdatedArticle(
    1,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    Right(None),
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    Right(None)
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
    api.Status(DRAFT.toString, Seq.empty),
    Some(api.ArticleTitle("title", "nb")),
    Some(api.ArticleContent("content", "nb")),
    Some(
      api.Copyright(
        Some(
          api.License(
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
        None
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
    None
  )

  val apiArticleUserTest: api.Article = api.Article(
    articleId,
    Some(s"//red.ndla.no/node/$externalId"),
    2,
    api.Status(USER_TEST.toString, Seq.empty),
    Some(api.ArticleTitle("title", "nb")),
    Some(api.ArticleContent("content", "nb")),
    Some(
      api.Copyright(
        Some(
          api.License(
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
        None
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
    None
  )

  val sampleTopicArticle: common.draft.Draft = common.draft.Draft(
    Option(1),
    Option(1),
    common.Status(DRAFT, Set.empty),
    Seq(common.Title("test", "en")),
    Seq(common.ArticleContent("<section><div>test</div></section>", "en")),
    Some(publicDomainCopyright),
    Seq.empty,
    Seq.empty,
    Seq(common.VisualElement("image", "en")),
    Seq(common.ArticleIntroduction("This is an introduction", "en")),
    Seq.empty,
    Seq.empty,
    LocalDateTime.now().minusDays(4).withNano(0),
    LocalDateTime.now().minusDays(2).withNano(0),
    userWithWriteAccess.id,
    LocalDateTime.now().minusDays(2).withNano(0),
    common.draft.ArticleType.TopicArticle,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    common.Availability.everyone,
    Seq.empty,
    Seq.empty,
    None
  )

  val sampleArticleWithPublicDomain: common.draft.Draft = common.draft.Draft(
    Option(1),
    Option(1),
    common.Status(DRAFT, Set.empty),
    Seq(common.Title("test", "en")),
    Seq(common.ArticleContent("<section><div>test</div></section>", "en")),
    Some(publicDomainCopyright),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq(common.ArticleIntroduction("This is an introduction", "en")),
    Seq.empty,
    Seq.empty,
    LocalDateTime.now().minusDays(4).withNano(0),
    LocalDateTime.now().minusDays(2).withNano(0),
    userWithWriteAccess.id,
    LocalDateTime.now().minusDays(2).withNano(0),
    common.draft.ArticleType.Standard,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    common.Availability.everyone,
    Seq.empty,
    common.draft.RevisionMeta.default,
    None
  )

  val sampleDomainArticle: common.draft.Draft = common.draft.Draft(
    Option(articleId),
    Option(2),
    common.Status(DRAFT, Set.empty),
    Seq(common.Title("title", "nb")),
    Seq(common.ArticleContent("content", "nb")),
    Some(common.draft.Copyright(Some(CC_BY.toString), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, None)),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq(common.ArticleMetaDescription("meta description", "nb")),
    Seq.empty,
    today,
    today,
    "ndalId54321",
    today,
    common.draft.ArticleType.Standard,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    common.Availability.everyone,
    Seq.empty,
    Seq.empty,
    None
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
      api.Copyright(
        Some(api.License("publicdomain", None, None)),
        Some(""),
        Seq.empty,
        Seq.empty,
        Seq.empty,
        None,
        None,
        None
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
    None
  )

  val sampleArticleWithByNcSa: common.draft.Draft =
    sampleArticleWithPublicDomain.copy(copyright = Some(byNcSaCopyright))
  val sampleArticleWithCopyrighted: common.draft.Draft =
    sampleArticleWithPublicDomain.copy(copyright = Some(copyrighted))

  val sampleDomainArticleWithHtmlFault: common.draft.Draft = common.draft.Draft(
    Option(articleId),
    Option(2),
    common.Status(DRAFT, Set.empty),
    Seq(common.Title("test", "en")),
    Seq(
      common.ArticleContent(
        """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
      |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
      |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
      |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
    """.stripMargin,
        "en"
      )
    ),
    Some(common.draft.Copyright(Some("publicdomain"), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, None)),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq(common.ArticleMetaDescription("meta description", "nb")),
    Seq.empty,
    today,
    today,
    "ndalId54321",
    today,
    common.draft.ArticleType.Standard,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    common.Availability.everyone,
    Seq.empty,
    Seq.empty,
    None
  )

  val apiArticleWithHtmlFaultV2: api.Article = api.Article(
    1,
    None,
    1,
    api.Status(DRAFT.toString, Seq.empty),
    Some(api.ArticleTitle("test", "en")),
    Some(
      api.ArticleContent(
        """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
        |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
        |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
        |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin,
        "en"
      )
    ),
    Some(
      api.Copyright(
        Some(api.License("publicdomain", None, None)),
        Some(""),
        Seq.empty,
        Seq.empty,
        Seq.empty,
        None,
        None,
        None
      )
    ),
    Some(api.ArticleTag(Seq.empty, "en")),
    Seq.empty,
    None,
    None,
    Some(api.ArticleMetaDescription("so meta", "en")),
    None,
    LocalDateTime.now().minusDays(4),
    LocalDateTime.now().minusDays(2),
    "ndalId54321",
    LocalDateTime.now().minusDays(2),
    "standard",
    Seq("en"),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    Seq.empty,
    availability = "everyone",
    Seq.empty,
    Seq.empty,
    None
  )

  val (nodeId, nodeId2)         = ("1234", "4321")
  val sampleTitle: common.Title = common.Title("title", "en")

  val visualElement: common.VisualElement = common.VisualElement(
    s"""<embed data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />""",
    "nb"
  )

  val sampleApiAgreement: api.Agreement = api.Agreement(
    1,
    "title",
    "content",
    api.Copyright(Some(api.License("publicdomain", None, None)), Some(""), Seq(), List(), List(), None, None, None),
    LocalDateTime.now().minusDays(4),
    LocalDateTime.now().minusDays(2),
    "ndalId54321"
  )

  val sampleDomainAgreement: domain.Agreement = domain.Agreement(
    id = Some(1),
    title = "Title",
    content = "Content",
    copyright = byNcSaCopyright,
    created = LocalDateTime.now().minusDays(4),
    updated = LocalDateTime.now().minusDays(2),
    updatedBy = userWithWriteAccess.id
  )

  val sampleBySaDomainAgreement: domain.Agreement = domain.Agreement(
    id = Some(1),
    title = "Title",
    content = "Content",
    copyright =
      common.draft.Copyright(Some(CC_BY_SA.toString), Some("Origin"), List(), List(), List(), None, None, None),
    created = LocalDateTime.now().minusDays(4),
    updated = LocalDateTime.now().minusDays(2),
    updatedBy = userWithWriteAccess.id
  )

  val emptyDomainUserData: domain.UserData =
    domain.UserData(id = None, userId = "", savedSearches = None, latestEditedArticles = None, favoriteSubjects = None)

  val emptyApiUserData: api.UserData =
    api.UserData(userId = "", savedSearches = None, latestEditedArticles = None, favoriteSubjects = None)

  val newAgreement: NewAgreement = NewAgreement(
    "newTitle",
    "newString",
    api.NewAgreementCopyright(
      Some(api.License("by-sa", None, None)),
      Some(""),
      List(),
      List(),
      List(),
      None,
      None,
      None
    )
  )
  val statusWithAwaitingPublishing                 = Set(DRAFT, QUEUED_FOR_PUBLISHING)
  val statusWithPublished: common.Status           = common.Status(PUBLISHED, Set.empty)
  val statusWithDraft: common.Status               = common.Status(DRAFT, Set.empty)
  val statusWithProposal: common.Status            = common.Status(PROPOSAL, Set.empty)
  val statusWithUserTest: common.Status            = common.Status(USER_TEST, Set.empty)
  val statusWithAwaitingQA: common.Status          = common.Status(AWAITING_QUALITY_ASSURANCE, Set.empty)
  val statusWithQueuedForPublishing: common.Status = common.Status(QUEUED_FOR_PUBLISHING, Set.empty)

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
