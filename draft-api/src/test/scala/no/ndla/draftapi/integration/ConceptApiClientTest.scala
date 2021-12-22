/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers._

import scala.util.Success

class ConceptApiClientTest extends UnitSuite with TestEnvironment {
  val baseUrl = "http://mockpath"
  val draftPath = s"concept-api/v1/drafts"
  val idPath = (id: Long) => s"$draftPath/$id"

  override val conceptApiClient: ConceptApiClient = spy(new ConceptApiClient(baseUrl))

  override protected def beforeEach(): Unit = {
    // Since we use spy, we reset the mock before each test allowing verify to be accurate
    reset(conceptApiClient)
  }

  test("Test that publishConceptsIfToPublishing publishes concepts with publishable status") {
    val idPath1 = s"$draftPath/100"

    doReturn(Success(DraftConcept(100, ConceptStatus("QUALITY_ASSURED"))))
      .when(conceptApiClient)
      .get[DraftConcept](eqTo(idPath1), any, any)(any)

    doReturn(Success(DraftConcept(100, ConceptStatus("PUBLISHED"))))
      .when(conceptApiClient)
      .put[DraftConcept](eqTo(s"$idPath1/status/PUBLISHED"), any, any)(any)

    conceptApiClient.publishConceptsIfToPublishing(ids = Seq(100))

    verify(conceptApiClient, times(1)).put[DraftConcept](eqTo(s"$idPath1/status/PUBLISHED"), any, any)(any)
    verify(conceptApiClient, times(1)).put[DraftConcept](any, any, any)(any)
  }

  test("Test that publishConceptsIfToPublishing does not publish concepts with unpublishable status") {
    val idPath1 = s"$draftPath/100"
    val idPath2 = s"$draftPath/200"
    val idPath3 = s"$draftPath/300"
    val idPath4 = s"$draftPath/400"
    val idPath5 = s"$draftPath/500"
    val idPath6 = s"$draftPath/600"

    doReturn(Success(DraftConcept(100, ConceptStatus("DRAFT"))))
      .when(conceptApiClient)
      .get[DraftConcept](eqTo(idPath1), any, any)(any)
    doReturn(Success(DraftConcept(200, ConceptStatus("QUEUED_FOR_LANGUAGE"))))
      .when(conceptApiClient)
      .get[DraftConcept](eqTo(idPath2), any, any)(any)
    doReturn(Success(DraftConcept(300, ConceptStatus("TRANSLATED"))))
      .when(conceptApiClient)
      .get[DraftConcept](eqTo(idPath3), any, any)(any)
    doReturn(Success(DraftConcept(400, ConceptStatus("UNPUBLISHED"))))
      .when(conceptApiClient)
      .get[DraftConcept](eqTo(idPath4), any, any)(any)
    doReturn(Success(DraftConcept(500, ConceptStatus("ARCHIVED"))))
      .when(conceptApiClient)
      .get[DraftConcept](eqTo(idPath5), any, any)(any)
    doReturn(Success(DraftConcept(600, ConceptStatus("PUBLISHED"))))
      .when(conceptApiClient)
      .get[DraftConcept](eqTo(idPath6), any, any)(any)

    verify(conceptApiClient, never).put[DraftConcept](any, any, any)(any)
  }

}
