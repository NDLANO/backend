/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import io.circe.syntax.*
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Responsible
import no.ndla.common.model.domain.concept.ConceptMetaImage
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.TestUtility.{getFields, getMappingFields}
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.api
import no.ndla.searchapi.model.domain.LearningResourceType
import no.ndla.searchapi.model.search.SearchableConcept
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}

class DraftConceptIndexServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {

  e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse(""))
  override val draftConceptIndexService: DraftConceptIndexService = new DraftConceptIndexService {
    override val indexShards = 1
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    articleIndexService.deleteIndexAndAlias()
    articleIndexService.createIndexWithGeneratedName
  }

  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  test("That mapping contains every field after serialization") {
    val languageValues = SearchableLanguageValues(Seq(LanguageValue("nb", "hei"), LanguageValue("en", "hå")))
    val languageList   = SearchableLanguageList(Seq(LanguageValue("nb", Seq("")), LanguageValue("en", Seq(""))))
    val now            = NDLADate.now()

    val searchableToTestWith = SearchableConcept(
      id = 1,
      conceptType = "concept",
      title = languageValues,
      content = languageValues,
      metaImage = Seq(ConceptMetaImage("1", "alt", "nb")),
      defaultTitle = Some("hei"),
      tags = languageList,
      subjectIds = List("urn:subject:1"),
      lastUpdated = now,
      draftStatus = api.StatusDTO("IN_PROGRESS", Seq("PUBLISHED")),
      users = List("noen", "some-id"),
      updatedBy = Seq("noen"),
      license = Some("CC-BY-SA-4.0"),
      authors = List("Noen Kule"),
      articleIds = Seq(1, 2, 3),
      created = now,
      source = Some("heidu"),
      responsible = Some(Responsible("some-id", now)),
      gloss = Some("hei"),
      domainObject = TestData.sampleNbDomainConcept,
      favorited = 0,
      learningResourceType = LearningResourceType.Concept
    )
    val searchableFields = searchableToTestWith.asJson
    val fields           = getFields(searchableFields, None, Seq("domainObject"))
    val mapping          = draftConceptIndexService.getMapping

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
