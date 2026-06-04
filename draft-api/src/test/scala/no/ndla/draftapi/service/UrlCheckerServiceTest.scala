/*
 * Part of NDLA draft-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service

import no.ndla.common.model.domain.{ArticleContent, Responsible, RevisionMeta, RevisionStatus, Status}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.NdlaClient
import no.ndla.network.model.NdlaRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import sttp.client4.{Response, WebSocketSyncBackend}
import sttp.model.{Method, RequestMetadata, StatusCode, Uri}

import java.util.UUID

class UrlCheckerServiceTest extends UnitSuite with TestEnvironment {

  // Override ndlaClient with a version that uses a mock sttp backend
  val httpClientMock: WebSocketSyncBackend          = mock[WebSocketSyncBackend]
  override implicit lazy val ndlaClient: NdlaClient = new NdlaClient {
    override val client: WebSocketSyncBackend = httpClientMock
  }

  val service: UrlCheckerService = new UrlCheckerService

  val testUrl        = "https://example.com/page"
  val redirectTarget = "https://example.com/new-page"

  private def mockResponse(
      statusCode: Int,
      locationHeader: Option[String] = None,
      url: String = testUrl,
  ): Response[String] = {
    val headers = locationHeader.map(loc => sttp.model.Header("Location", loc)).toSeq
    new Response(
      body = "",
      code = StatusCode(statusCode),
      statusText = StatusCode(statusCode).toString,
      headers = headers,
      history = List.empty,
      request = RequestMetadata(Method.HEAD, Uri.unsafeParse(url), List.empty),
    )
  }

  private def draftWithContent(html: String): Draft = TestData
    .sampleArticleWithPublicDomain
    .copy(content = Seq(ArticleContent(html, "nb")), notes = Seq.empty, revisionMeta = Seq.empty)

  override def beforeEach(): Unit = {
    reset(httpClientMock)
    when(clock.now()).thenReturn(TestData.today)
  }

  test("extractUrls returns http and https hrefs from anchor tags") {
    val html = """<p><a href="https://example.com">link</a> and <a href="http://other.com/path">other</a></p>"""
    service.extractUrls(html) should contain theSameElementsAs List("https://example.com", "http://other.com/path")
  }

  test("extractUrls extracts data-url from ndlaembed related-content tags") {
    val html = """<ndlaembed data-resource="related-content" data-url="https://related.com/page"></ndlaembed>"""
    service.extractUrls(html) should contain theSameElementsAs List("https://related.com/page")
  }

  test("extractUrls does not extract data-url from ndlaembed tags with other resource types") {
    val html = """<ndlaembed data-resource="image" data-url="https://should-be-ignored.com"></ndlaembed>"""
    service.extractUrls(html) shouldBe empty
  }

  test("extractUrls extracts urls from both a-tags and ndlaembed related-content tags") {
    val html = """<p><a href="https://anchor.com">link</a></p>""" +
      """<ndlaembed data-resource="related-content" data-url="https://related.com"></ndlaembed>"""
    service.extractUrls(html) should contain theSameElementsAs List("https://anchor.com", "https://related.com")
  }

  test("extractUrls excludes relative and non-http hrefs") {
    val html =
      """<p><a href="/relative/path">rel</a><a href="mailto:foo@bar.com">mail</a><a href="ftp://ftp.example.com">ftp</a></p>"""
    service.extractUrls(html) shouldBe empty
  }

  test("extractUrls returns empty list when there are no anchor tags") {
    service.extractUrls("<div><p>No links here</p></div>") shouldBe empty
  }

  test("extractUrls returns empty list for empty html") {
    service.extractUrls("") shouldBe empty
  }

  test("checkUrl returns UrlOk for a 200 response") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(200))
    service.checkUrl(testUrl) shouldBe UrlOk
  }

  test("checkUrl returns UrlRedirected with new url for 301 response with Location header") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(301, Some(redirectTarget)))
    service.checkUrl(testUrl) shouldBe UrlRedirected(redirectTarget)
  }

  test("checkUrl returns UrlRedirected for 307 response with Location header") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(307, Some(redirectTarget)))
    service.checkUrl(testUrl) shouldBe UrlRedirected(redirectTarget)
  }

  test("checkUrl returns UrlRedirected for 308 response with Location header") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(308, Some(redirectTarget)))
    service.checkUrl(testUrl) shouldBe UrlRedirected(redirectTarget)
  }

  test("checkUrl returns UrlOk for redirect response without Location header") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(301, None))
    service.checkUrl(testUrl) shouldBe UrlOk
  }

  test("checkUrl returns UrlBroken for 404 response") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(404))
    service.checkUrl(testUrl) shouldBe UrlBroken(404)
  }

  test("checkUrl returns UrlBroken for 500 response") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(500))
    service.checkUrl(testUrl) shouldBe UrlBroken(500)
  }

  test("checkUrl returns UrlBroken for 410 Gone response") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(410))
    service.checkUrl(testUrl) shouldBe UrlBroken(410)
  }

  test("checkUrl returns UrlCheckFailed when sttp throws an exception") {
    when(httpClientMock.send(any[NdlaRequest])).thenThrow(new RuntimeException("Connection refused"))
    service.checkUrl(testUrl) shouldBe a[UrlCheckFailed]
  }

  test("checkUrl returns UrlOk for 2xx responses other than 200") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(204))
    service.checkUrl(testUrl) shouldBe UrlOk
  }

  test("checkUrl sends a HEAD request (not GET or POST)") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(200))
    service.checkUrl(testUrl)
    val captor = org.mockito.ArgumentCaptor.forClass(classOf[NdlaRequest])
    verify(httpClientMock).send(captor.capture())
    captor.getValue.method shouldBe Method.HEAD
  }

  test("updateContentUrls replaces matching href with new url") {
    val html    = """<p><a href="https://old.com">text</a></p>"""
    val updates = Map("https://old.com" -> "https://new.com")
    val result  = service.updateContentUrls(html, updates)
    result should include("https://new.com")
    result should not include "https://old.com"
  }

  test("updateContentUrls replaces data-url on ndlaembed related-content") {
    val html    = """<ndlaembed data-resource="related-content" data-url="https://old.com"></ndlaembed>"""
    val updates = Map("https://old.com" -> "https://new.com")
    val result  = service.updateContentUrls(html, updates)
    result should include("https://new.com")
    result should not include "https://old.com"
  }

  test("updateContentUrls replaces both a-tag href and ndlaembed data-url in the same content") {
    val html = """<p><a href="https://old.com">link</a></p>""" +
      """<ndlaembed data-resource="related-content" data-url="https://old.com"></ndlaembed>"""
    val updates = Map("https://old.com" -> "https://new.com")
    val result  = service.updateContentUrls(html, updates)
    result should not include "https://old.com"
    result.split("https://new.com").length - 1 shouldBe 2 // appears twice
  }

  test("updateContentUrls leaves urls not in the map unchanged") {
    val html    = """<p><a href="https://keep.com">text</a><a href="https://old.com">text2</a></p>"""
    val updates = Map("https://old.com" -> "https://new.com")
    val result  = service.updateContentUrls(html, updates)
    result should include("https://keep.com")
    result should include("https://new.com")
    result should not include "https://old.com"
  }

  test("updateContentUrls returns content unchanged when updates map is empty") {
    val html   = """<p><a href="https://example.com">text</a></p>"""
    val result = service.updateContentUrls(html, Map.empty)
    result shouldBe html
  }

  test("updateContentUrls replaces all occurrences of the same url") {
    val html    = """<p><a href="https://old.com">a</a><a href="https://old.com">b</a></p>"""
    val updates = Map("https://old.com" -> "https://new.com")
    val result  = service.updateContentUrls(html, updates)
    result should not include "https://old.com"
    result should include("https://new.com")
  }

  test("checkAndUpdateUrls updates ndlaembed data-url and adds EditorNote for redirect") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(301, Some(redirectTarget)))
    val html   = s"""<ndlaembed data-resource="related-content" data-url="$testUrl"></ndlaembed>"""
    val draft  = draftWithContent(html)
    val result = service.checkAndUpdateUrls(draft, "test-user")

    result.content.map(_.content).mkString should include(redirectTarget)
    result.content.map(_.content).mkString should not include testUrl
    result.notes should have size 1
    result.notes.head.note should include(testUrl)
    result.notes.head.note should include(redirectTarget)
  }

  test("checkAndUpdateUrls adds RevisionMeta for broken ndlaembed data-url") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(404))
    val html   = s"""<ndlaembed data-resource="related-content" data-url="$testUrl"></ndlaembed>"""
    val draft  = draftWithContent(html)
    val result = service.checkAndUpdateUrls(draft, "test-user")

    result.revisionMeta should have size 1
    result.revisionMeta.head.note should include(testUrl)
    result.revisionMeta.head.note should include("404")
    result.revisionMeta.head.status shouldBe RevisionStatus.NeedsRevision
  }

  test("checkAndUpdateUrls deduplicates url appearing in both a-tag and ndlaembed") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(200))
    val html = s"""<p><a href="$testUrl">link</a></p>""" +
      s"""<ndlaembed data-resource="related-content" data-url="$testUrl"></ndlaembed>"""
    val draft  = draftWithContent(html)
    val result = service.checkAndUpdateUrls(draft, "test-user")
    // URL deduplication means backend is called only once
    verify(httpClientMock, times(1)).send(any[NdlaRequest])
    result shouldBe draft
  }

  test("checkAndUpdateUrls returns draft unchanged when content has no http links") {
    val draft  = draftWithContent("<p>No links here</p>")
    val result = service.checkAndUpdateUrls(draft, "test-user")
    result shouldBe draft
  }

  test("checkAndUpdateUrls returns draft unchanged for 200 responses") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(200))
    val draft  = draftWithContent(s"""<p><a href="$testUrl">link</a></p>""")
    val result = service.checkAndUpdateUrls(draft, "test-user")
    result shouldBe draft
  }

  test("checkAndUpdateUrls updates href and adds EditorNote for 301 redirect") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(301, Some(redirectTarget)))
    val draft  = draftWithContent(s"""<p><a href="$testUrl">link</a></p>""")
    val result = service.checkAndUpdateUrls(draft, "test-user")

    // Content should contain the redirect target URL
    result.content.map(_.content).mkString should include(redirectTarget)
    result.content.map(_.content).mkString should not include testUrl

    // An EditorNote should have been added
    result.notes should have size 1
    result.notes.head.note should include(testUrl)
    result.notes.head.note should include(redirectTarget)
    result.notes.head.user shouldBe "test-user"

    // No new RevisionMeta
    result.revisionMeta shouldBe draft.revisionMeta
  }

  test("checkAndUpdateUrls adds RevisionMeta for 404 broken link") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(404))
    val draft  = draftWithContent(s"""<p><a href="$testUrl">link</a></p>""")
    val result = service.checkAndUpdateUrls(draft, "test-user")

    // Content should be unchanged
    result.content shouldBe draft.content

    // No EditorNotes added
    result.notes shouldBe draft.notes

    // A RevisionMeta should have been added
    result.revisionMeta should have size 1
    result.revisionMeta.head.note should include(testUrl)
    result.revisionMeta.head.note should include("404")
    result.revisionMeta.head.status shouldBe RevisionStatus.NeedsRevision
  }

  test("checkAndUpdateUrls does not add duplicate RevisionMeta for already-noted broken url") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(404))
    val existingNote = s"URL '$testUrl' returnerte HTTP 404 og må oppdateres."
    val existingMeta = RevisionMeta(
      id = UUID.randomUUID(),
      revisionDate = TestData.today.plusYears(1).withNano(0),
      note = existingNote,
      status = RevisionStatus.NeedsRevision,
    )
    val draft  = draftWithContent(s"""<p><a href="$testUrl">link</a></p>""").copy(revisionMeta = Seq(existingMeta))
    val result = service.checkAndUpdateUrls(draft, "test-user")

    // Should still be just the one existing entry
    result.revisionMeta should have size 1
    result.revisionMeta.head.id shouldBe existingMeta.id
  }

  test("checkAndUpdateUrls deduplicates identical urls before checking") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(200))
    val html   = s"""<p><a href="$testUrl">a</a><a href="$testUrl">b</a></p>"""
    val draft  = draftWithContent(html)
    val result = service.checkAndUpdateUrls(draft, "test-user")
    // Should only call the backend once for the deduplicated URL
    verify(httpClientMock, times(1)).send(any[NdlaRequest])
    result shouldBe draft
  }

  test("checkAndUpdateUrls handles multiple content blocks across languages") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(301, Some(redirectTarget)))
    val draft = TestData
      .sampleArticleWithPublicDomain
      .copy(
        content = Seq(
          ArticleContent(s"""<p><a href="$testUrl">link nb</a></p>""", "nb"),
          ArticleContent(s"""<p><a href="$testUrl">link nn</a></p>""", "nn"),
        ),
        notes = Seq.empty,
        revisionMeta = Seq.empty,
      )
    val result = service.checkAndUpdateUrls(draft, "test-user")

    // Both content blocks should be updated
    result.content.foreach(_.content should include(redirectTarget))
    result.content.foreach(_.content should not include testUrl)

    // URL is only checked once (deduplicated across language variants)
    verify(httpClientMock, times(1)).send(any[NdlaRequest])

    // One EditorNote (one unique URL was redirected)
    result.notes should have size 1
  }

  test("checkAndUpdateUrls adds RevisionMeta for 500 server error") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(500))
    val draft  = draftWithContent(s"""<a href="$testUrl">link</a>""")
    val result = service.checkAndUpdateUrls(draft, "test-user")
    result.revisionMeta should have size 1
    result.revisionMeta.head.note should include("500")
  }

  test("checkAndUpdateUrls adds RevisionMeta for network errors (UrlCheckFailed)") {
    when(httpClientMock.send(any[NdlaRequest])).thenThrow(new RuntimeException("Connection timed out"))
    val draft  = draftWithContent(s"""<a href="$testUrl">link</a>""")
    val result = service.checkAndUpdateUrls(draft, "test-user")
    // Network failures (unreachable domain etc.) are treated like broken links
    result.revisionMeta should have size 1
    result.revisionMeta.head.note should include(testUrl)
    result.revisionMeta.head.note should include("Connection timed out")
    result.revisionMeta.head.status shouldBe RevisionStatus.NeedsRevision
    // Content and notes are unchanged
    result.content shouldBe draft.content
    result.notes shouldBe draft.notes
  }

  test("checkAndUpdateUrls does not add duplicate RevisionMeta for already-noted unreachable url") {
    val errorMsg     = "Connection timed out"
    val existingNote = s"URL '$testUrl' kunne ikke nås ($errorMsg) og må oppdateres."
    val existingMeta = RevisionMeta(
      id = UUID.randomUUID(),
      revisionDate = TestData.today.plusYears(1).withNano(0),
      note = existingNote,
      status = RevisionStatus.NeedsRevision,
    )
    when(httpClientMock.send(any[NdlaRequest])).thenThrow(new RuntimeException(errorMsg))
    val draft  = draftWithContent(s"""<a href="$testUrl">link</a>""").copy(revisionMeta = Seq(existingMeta))
    val result = service.checkAndUpdateUrls(draft, "test-user")
    result.revisionMeta should have size 1
    result.revisionMeta.head.id shouldBe existingMeta.id
  }

  private def publishedDraftWithContent(html: String): Draft = draftWithContent(html).copy(
    status = Status(DraftStatus.PUBLISHED, Set.empty),
    updatedBy = "editor-user",
    responsible = None,
  )

  test("checkAndUpdateUrls sets status to IN_PROGRESS and responsible when PUBLISHED draft is modified by redirect") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(301, Some(redirectTarget)))
    val draft  = publishedDraftWithContent(s"""<a href="$testUrl">link</a>""")
    val result = service.checkAndUpdateUrls(draft, "url-checker")

    result.status.current shouldBe DraftStatus.IN_PROGRESS
    result.status.other shouldBe Set.empty
    result.responsible shouldBe Some(Responsible("editor-user", TestData.today))
  }

  test("checkAndUpdateUrls does NOT change status when PUBLISHED draft only gets revisionMeta for a broken link") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(404))
    val draft  = publishedDraftWithContent(s"""<a href="$testUrl">link</a>""")
    val result = service.checkAndUpdateUrls(draft, "url-checker")

    result.status.current shouldBe DraftStatus.PUBLISHED
    result.responsible shouldBe None
    // RevisionMeta is still added
    result.revisionMeta should have size 1
  }

  test("checkAndUpdateUrls does NOT change status when PUBLISHED draft only gets revisionMeta for an unreachable url") {
    when(httpClientMock.send(any[NdlaRequest])).thenThrow(new RuntimeException("Connection refused"))
    val draft  = publishedDraftWithContent(s"""<a href="$testUrl">link</a>""")
    val result = service.checkAndUpdateUrls(draft, "url-checker")

    result.status.current shouldBe DraftStatus.PUBLISHED
    result.responsible shouldBe None
    // RevisionMeta is still added
    result.revisionMeta should have size 1
  }

  test("checkAndUpdateUrls does not change status when PUBLISHED draft has only healthy links") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(200))
    val draft  = publishedDraftWithContent(s"""<a href="$testUrl">link</a>""")
    val result = service.checkAndUpdateUrls(draft, "url-checker")

    result.status.current shouldBe DraftStatus.PUBLISHED
    result.responsible shouldBe None
  }

  test("checkAndUpdateUrls does not change status when draft in IN_PROGRESS is modified") {
    when(httpClientMock.send(any[NdlaRequest])).thenReturn(mockResponse(301, Some(redirectTarget)))
    val draft = draftWithContent(s"""<a href="$testUrl">link</a>""").copy(
      status = Status(DraftStatus.IN_PROGRESS, Set.empty),
      updatedBy = "editor-user",
    )
    val result = service.checkAndUpdateUrls(draft, "url-checker")

    result.status.current shouldBe DraftStatus.IN_PROGRESS
    // responsible was not set by url-checker
    result.responsible shouldBe None
  }
}
