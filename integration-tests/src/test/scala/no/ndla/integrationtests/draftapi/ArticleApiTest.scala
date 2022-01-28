package no.ndla.integrationtests.draftapi

import no.ndla.articleapi
import no.ndla.draftapi
import no.ndla.draftapi.model.api.ContentId
import no.ndla.draftapi.model.domain
import no.ndla.draftapi.model.domain.Availability
import no.ndla.scalatestsuite.UnitTestSuite
import org.eclipse.jetty.server.Server

import java.util.Date

class ArticleApiTest extends UnitTestSuite {

  val articleApiServer: Server = articleapi.JettyLauncher.startServer()
  val draftApiServer: Server = draftapi.JettyLauncher.startServer(30111)

  val idResponse = ContentId(1)

  val testCopyright = draftapi.model.domain.Copyright(
    Some("CC-BY-SA-4.0"),
    Some("Origin"),
    Seq.empty,
    Seq.empty,
    Seq.empty,
    None,
    None,
    None
  )

  val testArticle = domain.Article(
    id = Some(1),
    revision = Some(1),
    status = domain.Status(domain.ArticleStatus.PUBLISHED, Set.empty),
    title = Seq(domain.ArticleTitle("Title", "nb")),
    content = Seq(domain.ArticleContent("Content", "nb")),
    copyright = Some(testCopyright),
    tags = Seq(domain.ArticleTag(List("Tag1", "Tag2", "Tag3"), "nb")),
    requiredLibraries = Seq(),
    visualElement = Seq(),
    introduction = Seq(),
    metaDescription = Seq(domain.ArticleMetaDescription("Meta Description", "nb")),
    metaImage = Seq(),
    created = new Date(0),
    updated = new Date(0),
    updatedBy = "updatedBy",
    published = new Date(0),
    articleType = domain.ArticleType.Standard,
    notes = Seq.empty,
    previousVersionsNotes = Seq.empty,
    editorLabels = Seq.empty,
    grepCodes = Seq.empty,
    conceptIds = Seq.empty,
    availability = Availability.everyone,
    relatedContent = Seq.empty
  )

  test("that updating articles should work") {
    draftapi.ComponentRegistry.articleApiClient.updateArticle(1, testArticle, List("1234"), false, false)
  }
}
