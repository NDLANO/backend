/*
 * Part of NDLA search-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.{Copyright, DraftStatus, RevisionMeta, RevisionStatus}
import no.ndla.common.model.domain.{ArticleContent, Author, EditorNote, Status}
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.TestUtility.{getFields, getMappingFields}
import no.ndla.search.model.SearchableLanguageFormats
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import org.json4s.{Extraction, Formats}
import org.scalatest.Outcome

import java.util.UUID
import scala.util.Failure

class DraftIndexServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))
  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    elasticSearchContainer match {
      case Failure(ex) =>
        println(s"Elasticsearch container not running, cancelling '${this.getClass.getName}'")
        println(s"Got exception: ${ex.getMessage}")
        ex.printStackTrace()
      case _ =>
    }

    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val draftIndexService: DraftIndexService = new DraftIndexService {
    override val indexShards = 1
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    draftIndexService.deleteIndexAndAlias()
    draftIndexService.createIndexWithGeneratedName
  }

  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService
  implicit val formats: Formats       = SearchableLanguageFormats.JSonFormatsWithMillis

  test("That mapping contains every field after serialization") {
    val now = NDLADate.now()
    val domainDraft = TestData.draft1.copy(
      content = Seq(
        ArticleContent(
          """<section><h1>hei</h1><ndlaembed data-resource="image" data-title="heidu" data-resource_id="1"></ndlaembed><ndlaembed data-resource="h5p" data-title="yo"></ndlaembed></section>""",
          "nb"
        )
      ),
      status = Status(DraftStatus.PLANNED, Set(DraftStatus.IMPORTED)),
      notes = Seq(EditorNote("hei", "test", Status(DraftStatus.PLANNED, Set(DraftStatus.IMPORTED)), now)),
      previousVersionsNotes =
        Seq(EditorNote("hei", "test", Status(DraftStatus.PLANNED, Set(DraftStatus.IMPORTED)), now)),
      revisionMeta = Seq(RevisionMeta(UUID.randomUUID(), now, "hei", RevisionStatus.NeedsRevision)),
      copyright = Some(
        Copyright(
          license = Some("hei"),
          origin = Some("ho"),
          creators = Seq(Author("writer", "Jonas")),
          processors = Seq(Author("writer", "Jonas")),
          rightsholders = Seq(Author("writer", "Jonas")),
          validFrom = Some(now),
          validTo = Some(now)
        )
      )
    )
    val searchableToTestWith = searchConverterService
      .asSearchableDraft(
        domainDraft,
        Some(TestData.taxonomyTestBundle),
        Some(TestData.emptyGrepBundle)
      )
      .get

    val searchableFields = Extraction.decompose(searchableToTestWith)
    val fields           = getFields(searchableFields, None, Seq("domainObject"))
    val mapping          = draftIndexService.getMapping

    val staticMappingFields  = getMappingFields(mapping.properties, None)
    val dynamicMappingFields = mapping.templates.map(_.name)
    for (field <- fields) {
      val hasStatic  = staticMappingFields.contains(field)
      val hasDynamic = dynamicMappingFields.contains(field)

      if (!(hasStatic || hasDynamic)) {
        fail(s"'$field' was not found in mapping, i think you would want to add it to the index mapping?")
      }
    }
  }
}
