/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model
import no.ndla.common.model.api.{DraftCopyrightDTO, Missing}
import no.ndla.common.model.domain.{ContributorType, Priority, Title}
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.draft.DraftStatus.*
import no.ndla.common.model.domain.language.OptLanguageFields
import no.ndla.common.model.{NDLADate, api as commonApi, domain as common}
import no.ndla.draftapi.integration.LearningPath
import no.ndla.draftapi.model.api.*
import no.ndla.draftapi.model.{api, domain}
import no.ndla.mapping.License
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
  val userWithAdminAccess: TokenUser   =
    TokenUser("unit test", Set(DRAFT_API_WRITE, DRAFT_API_PUBLISH, DRAFT_API_ADMIN), None)

  val publicDomainCopyright: common.draft.DraftCopyright =
    common.draft.DraftCopyright(
      Some(License.PublicDomain.toString),
      Some(""),
      List.empty,
      List(),
      List(),
      None,
      None,
      false
    )
  private val byNcSaCopyright = common.draft.DraftCopyright(
    Some(CC_BY_NC_SA.toString),
    Some("Gotham City"),
    List(common.Author(ContributorType.Writer, "DC Comics")),
    List(),
    List(),
    None,
    None,
    false
  )
  private val copyrighted = common.draft.DraftCopyright(
    Some(License.Copyrighted.toString),
    Some("New York"),
    List(common.Author(ContributorType.Writer, "Clark Kent")),
    List(),
    List(),
    None,
    None,
    false
  )
  val today: NDLADate = NDLADate.now()

  val (articleId, externalId) = (1L, "751234")

  val sampleArticleV2: api.ArticleDTO = api.ArticleDTO(
    id = 1,
    oldNdlaUrl = None,
    revision = 1,
    status = api.StatusDTO(PLANNED.toString, Seq.empty),
    title = Some(api.ArticleTitleDTO("title", "title", "nb")),
    content = Some(api.ArticleContentDTO("this is content", "nb")),
    copyright = Some(
      DraftCopyrightDTO(
        Some(commonApi.LicenseDTO("licence", None, None)),
        Some("origin"),
        Seq(commonApi.AuthorDTO(ContributorType.Artist, "Per")),
        List(),
        List(),
        None,
        None,
        false
      )
    ),
    tags = Some(api.ArticleTagDTO(Seq("tag"), "nb")),
    requiredLibraries = Seq(api.RequiredLibraryDTO("JS", "JavaScript", "url")),
    visualElement = None,
    introduction = None,
    metaDescription = Some(api.ArticleMetaDescriptionDTO("metaDesc", "nb")),
    metaImage = None,
    created = NDLADate.of(2017, 1, 1, 12, 15, 32),
    updated = NDLADate.of(2017, 4, 1, 12, 15, 32),
    updatedBy = "me",
    published = NDLADate.of(2017, 4, 1, 12, 15, 32),
    articleType = "standard",
    supportedLanguages = Seq("nb"),
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
    priority = Priority.Unspecified,
    started = false,
    qualityEvaluation = None,
    disclaimer = None,
    traits = List.empty
  )

  val blankUpdatedArticle: UpdatedArticleDTO = api.UpdatedArticleDTO(
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
    priority = None,
    qualityEvaluation = None,
    disclaimer = None
  )

  val sampleApiUpdateArticle: UpdatedArticleDTO = blankUpdatedArticle.copy(
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

  val apiArticleV2: api.ArticleDTO = api.ArticleDTO(
    id = articleId,
    oldNdlaUrl = Some(s"//red.ndla.no/node/$externalId"),
    revision = 2,
    status = api.StatusDTO(PLANNED.toString, Seq.empty),
    title = Some(api.ArticleTitleDTO("title", "title", "nb")),
    content = Some(api.ArticleContentDTO("content", "nb")),
    copyright = Some(
      model.api.DraftCopyrightDTO(
        license = Some(
          commonApi.LicenseDTO(
            CC_BY.toString,
            Some("Creative Commons Attribution 4.0 International"),
            Some("https://creativecommons.org/licenses/by/4.0/")
          )
        ),
        origin = Some(""),
        creators = Seq.empty,
        processors = List(),
        rightsholders = List(),
        validFrom = None,
        validTo = None,
        processed = false
      )
    ),
    tags = None,
    requiredLibraries = Seq.empty,
    visualElement = None,
    introduction = None,
    metaDescription = Some(api.ArticleMetaDescriptionDTO("meta description", "nb")),
    metaImage = None,
    created = today,
    updated = today,
    updatedBy = "ndalId54321",
    published = today,
    articleType = "standard",
    supportedLanguages = Seq("nb"),
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
    priority = Priority.Unspecified,
    started = false,
    qualityEvaluation = None,
    disclaimer = None,
    traits = List.empty
  )

  val apiArticleUserTest: api.ArticleDTO = api.ArticleDTO(
    id = articleId,
    oldNdlaUrl = Some(s"//red.ndla.no/node/$externalId"),
    revision = 2,
    status = api.StatusDTO(EXTERNAL_REVIEW.toString, Seq.empty),
    title = Some(api.ArticleTitleDTO("title", "title", "nb")),
    content = Some(api.ArticleContentDTO("content", "nb")),
    copyright = Some(
      model.api.DraftCopyrightDTO(
        license = Some(
          commonApi.LicenseDTO(
            CC_BY.toString,
            Some("Creative Commons Attribution 4.0 International"),
            Some("https://creativecommons.org/licenses/by/4.0/")
          )
        ),
        origin = Some(""),
        creators = Seq.empty,
        processors = List(),
        rightsholders = List(),
        validFrom = None,
        validTo = None,
        processed = false
      )
    ),
    tags = None,
    requiredLibraries = Seq.empty,
    visualElement = None,
    introduction = None,
    metaDescription = Some(api.ArticleMetaDescriptionDTO("meta description", "nb")),
    metaImage = None,
    created = today,
    updated = today,
    updatedBy = "ndalId54321",
    published = today,
    articleType = "standard",
    supportedLanguages = Seq("nb"),
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
    priority = Priority.Unspecified,
    started = false,
    qualityEvaluation = None,
    disclaimer = None,
    traits = List.empty
  )

  val sampleTopicArticle: Draft = Draft(
    id = Option(1),
    revision = Option(1),
    status = common.Status(PLANNED, Set.empty),
    title = Seq(common.Title("test", "en")),
    content = Seq(common.ArticleContent("<section><div>test</div></section>", "en")),
    copyright = Some(publicDomainCopyright),
    tags = Seq.empty,
    requiredLibraries = Seq.empty,
    visualElement = Seq(common.VisualElement("image", "en")),
    introduction = Seq(common.Introduction("This is an introduction", "en")),
    metaDescription = Seq.empty,
    metaImage = Seq.empty,
    created = NDLADate.now().minusDays(4).withNano(0),
    updated = NDLADate.now().minusDays(2).withNano(0),
    updatedBy = userWithWriteAccess.id,
    published = NDLADate.now().minusDays(2).withNano(0),
    articleType = common.ArticleType.TopicArticle,
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
    started = false,
    qualityEvaluation = None,
    disclaimer = OptLanguageFields.empty,
    traits = List.empty
  )

  val sampleArticleWithPublicDomain: Draft = Draft(
    id = Option(1),
    revision = Option(1),
    status = common.Status(PLANNED, Set.empty),
    title = Seq(common.Title("test", "en")),
    content = Seq(common.ArticleContent("<section><div>test</div></section>", "en")),
    copyright = Some(publicDomainCopyright),
    tags = Seq.empty,
    requiredLibraries = Seq.empty,
    visualElement = Seq.empty,
    introduction = Seq(common.Introduction("This is an introduction", "en")),
    metaDescription = Seq.empty,
    metaImage = Seq.empty,
    created = NDLADate.now().minusDays(4).withNano(0),
    updated = NDLADate.now().minusDays(2).withNano(0),
    updatedBy = userWithWriteAccess.id,
    published = NDLADate.now().minusDays(2).withNano(0),
    articleType = common.ArticleType.Standard,
    notes = Seq.empty,
    previousVersionsNotes = Seq.empty,
    editorLabels = Seq.empty,
    grepCodes = Seq.empty,
    conceptIds = Seq.empty,
    availability = common.Availability.everyone,
    relatedContent = Seq.empty,
    revisionMeta = common.RevisionMeta.default,
    responsible = None,
    slug = None,
    comments = Seq.empty,
    priority = Priority.Unspecified,
    started = false,
    qualityEvaluation = None,
    disclaimer = OptLanguageFields.empty,
    traits = List.empty
  )

  val sampleDomainArticle: Draft = Draft(
    id = Option(articleId),
    revision = Option(2),
    status = common.Status(PLANNED, Set.empty),
    title = Seq(common.Title("title", "nb")),
    content = Seq(common.ArticleContent("content", "nb")),
    copyright = Some(
      common.draft.DraftCopyright(Some(CC_BY.toString), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, false)
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
    started = false,
    qualityEvaluation = None,
    disclaimer = OptLanguageFields.empty,
    traits = List.empty
  )

  val newArticle: NewArticleDTO = api.NewArticleDTO(
    "en",
    "test",
    Some(today),
    Some("<article><div>test</div></article>"),
    None,
    None,
    None,
    None,
    None,
    Some(
      model.api.DraftCopyrightDTO(
        Some(commonApi.LicenseDTO(License.PublicDomain.toString, None, None)),
        Some(""),
        Seq.empty,
        Seq.empty,
        Seq.empty,
        None,
        None,
        false
      )
    ),
    None,
    "standard",
    None,
    None,
    None,
    None,
    availability = None,
    None,
    None,
    None,
    None,
    None,
    None,
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
      common.draft.DraftCopyright(
        Some(License.PublicDomain.toString),
        Some(""),
        Seq.empty,
        Seq.empty,
        Seq.empty,
        None,
        None,
        false
      )
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
    started = false,
    qualityEvaluation = None,
    disclaimer = OptLanguageFields.empty,
    traits = List.empty
  )

  val apiArticleWithHtmlFaultV2: api.ArticleDTO = api.ArticleDTO(
    id = 1,
    oldNdlaUrl = None,
    revision = 1,
    status = api.StatusDTO(PLANNED.toString, Seq.empty),
    title = Some(api.ArticleTitleDTO("test", "title", "en")),
    content = Some(
      api.ArticleContentDTO(
        """<ul><li><h1>Det er ikke lov å gjøre dette.</h1> Tekst utenfor.</li><li>Dette er helt ok</li></ul>
          |<ul><li><h2>Det er ikke lov å gjøre dette.</h2></li><li>Dette er helt ok</li></ul>
          |<ol><li><h3>Det er ikke lov å gjøre dette.</h3></li><li>Dette er helt ok</li></ol>
          |<ol><li><h4>Det er ikke lov å gjøre dette.</h4></li><li>Dette er helt ok</li></ol>
      """.stripMargin,
        "en"
      )
    ),
    copyright = Some(
      model.api.DraftCopyrightDTO(
        Some(commonApi.LicenseDTO(License.PublicDomain.toString, None, None)),
        Some(""),
        Seq.empty,
        Seq.empty,
        Seq.empty,
        None,
        None,
        false
      )
    ),
    tags = Some(api.ArticleTagDTO(Seq.empty, "en")),
    requiredLibraries = Seq.empty,
    visualElement = None,
    introduction = None,
    metaDescription = Some(api.ArticleMetaDescriptionDTO("so meta", "en")),
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
    priority = Priority.Unspecified,
    started = false,
    qualityEvaluation = None,
    disclaimer = None,
    traits = List.empty
  )

  val (nodeId, nodeId2)         = ("1234", "4321")
  val sampleTitle: common.Title = common.Title("title", "en")

  val visualElement: common.VisualElement = common.VisualElement(
    s"""<$EmbedTagName data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size=""></$EmbedTagName>""",
    "nb"
  )

  val emptyDomainUserData: domain.UserData =
    domain.UserData(
      id = None,
      userId = "",
      savedSearches = None,
      latestEditedArticles = None,
      latestEditedConcepts = None,
      latestEditedLearningpaths = None,
      favoriteSubjects = None
    )

  val emptyApiUserData: api.UserDataDTO =
    api.UserDataDTO(
      userId = "",
      savedSearches = None,
      latestEditedArticles = None,
      favoriteSubjects = None,
      latestEditedConcepts = None,
      latestEditedLearningpaths = None
    )

  val statusWithPublished: common.Status      = common.Status(PUBLISHED, Set.empty)
  val statusWithPlanned: common.Status        = common.Status(PLANNED, Set.empty)
  val statusWithInProcess: common.Status      = common.Status(IN_PROGRESS, Set.empty)
  val statusWithExternalReview: common.Status = common.Status(EXTERNAL_REVIEW, Set.empty)
  val statusWithInternalReview: common.Status = common.Status(INTERNAL_REVIEW, Set.empty)
  val statusWithEndControl: common.Status     = common.Status(END_CONTROL, Set.empty)

  val sampleLearningPath: LearningPath = LearningPath(1, Title("Title", "nb"))

  val sampleApiGrepCodesSearchResult: GrepCodesSearchResultDTO = api.GrepCodesSearchResultDTO(10, 1, 1, Seq("a", "b"))
  val sampleApiTagsSearchResult: TagsSearchResultDTO           = api.TagsSearchResultDTO(10, 1, 1, "nb", Seq("a", "b"))

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
}
