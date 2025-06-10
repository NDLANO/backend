/*
 * Part of NDLA search-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import io.circe.syntax.*
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.{DraftCopyright, DraftStatus, RevisionMeta, RevisionStatus}
import no.ndla.common.model.domain.*
import no.ndla.scalatestsuite.ElasticsearchIntegrationSuite
import no.ndla.search.TestUtility.{getFields, getMappingFields}
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.util.UUID
import scala.util.Success

class DraftIndexServiceTest extends ElasticsearchIntegrationSuite with UnitSuite with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))

  override val draftIndexService: DraftIndexService = new DraftIndexService {
    override val indexShards = 1
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    draftIndexService.deleteIndexAndAlias()
    draftIndexService.createIndexWithGeneratedName
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(myndlaApiClient.getStatsFor(any, any)).thenReturn(Success(List.empty))
  }

  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

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
        DraftCopyright(
          license = Some("hei"),
          origin = Some("ho"),
          creators = Seq(Author(ContributorType.Writer, "Jonas")),
          processors = Seq(Author(ContributorType.Writer, "Jonas")),
          rightsholders = Seq(Author(ContributorType.Writer, "Jonas")),
          validFrom = Some(now),
          validTo = Some(now),
          false
        )
      ),
      responsible = Some(
        Responsible(
          "yolo",
          now
        )
      )
    )
    val searchableToTestWith = searchConverterService
      .asSearchableDraft(
        domainDraft,
        IndexingBundle(
          Some(TestData.emptyGrepBundle),
          Some(TestData.taxonomyTestBundle),
          None
        )
      )
      .get

    val searchableFields = searchableToTestWith.asJson
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
