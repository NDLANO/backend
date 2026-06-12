/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.integration

import cats.implicits.*
import no.ndla.common.model.domain.Title
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.*
import org.mockito.invocation.InvocationOnMock

import scala.concurrent.TimeoutException
import scala.util.{Failure, Success}

class TaxonomyApiClientTest extends UnitSuite with TestEnvironment {

  override lazy val taxonomyApiClient: TaxonomyApiClient = spy(new TaxonomyApiClient)

  override def beforeEach(): Unit = {
    // Since we use spy, we reset the mock before each test allowing verify to be accurate
    reset(taxonomyApiClient)
  }

  test("That updating one nodes translations works as expected") {
    val article = TestData
      .sampleDomainArticle
      .copy(title = Seq(Title("Norsk", "nb"), Title("<strong>Engelsk</strong>", "en")))
    val id   = article.id.get
    val node = Node(
      "urn:resource:1:12312",
      "Outdated name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Translation](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Translation], any)(using any)
    doReturn(Success(List(node)), Success(List.empty)).when(taxonomyApiClient).queryNodes(id)
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any[(String, String)])

    taxonomyApiClient.updateTaxonomyIfExists(id, article, TokenUser.SystemUser) should be(Success(id))

    verify(taxonomyApiClient, times(1)).updateNode(
      eqTo(
        node.copy(
          name = "Norsk",
          translations = List(Translation("Norsk", "nb".some), Translation("Engelsk", "en".some)),
        )
      ),
      any,
    )
  }

  test("That updating multiple nodes translations works as expected") {
    val article = TestData.sampleDomainArticle.copy(title = Seq(Title("Norsk", "nb"), Title("Engelsk", "en")))
    val id      = article.id.get
    val node    = Node(
      "urn:resource:1:12312",
      "Outdated name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )
    val node2 = Node(
      "urn:resource:1:99551",
      "Outdated other name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated other name", "nb".some)),
    )

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Translation](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Translation], any)(using any)
    doReturn(Success(List(node, node2)), Success(List.empty)).when(taxonomyApiClient).queryNodes(id)
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any[(String, String)])

    taxonomyApiClient.updateTaxonomyIfExists(id, article, TokenUser.SystemUser) should be(Success(id))

    verify(taxonomyApiClient, times(1)).updateNode(
      eqTo(
        node.copy(
          name = "Norsk",
          translations = List(Translation("Norsk", "nb".some), Translation("Engelsk", "en".some)),
        )
      ),
      any,
    )
    verify(taxonomyApiClient, times(1)).updateNode(
      eqTo(
        node2.copy(
          name = "Norsk",
          translations = List(Translation("Norsk", "nb".some), Translation("Engelsk", "en".some)),
        )
      ),
      any,
    )
    verify(taxonomyApiClient, times(2)).updateNode(any[Node], any)
  }

  test("That both resources and topics for single article is updated") {
    val article   = TestData.sampleDomainArticle.copy(title = Seq(Title("Norsk", "nb"), Title("Engelsk", "en")))
    val id        = article.id.get
    val resource1 = Node(
      "urn:resource:1:12035",
      "Outdated res name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )
    val resource2 = Node(
      "urn:resource:1:d8a19b97-10ee-481a-b44c-dd54cffbddda",
      "Outdated other res name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )
    val topic1 = Node(
      "urn:topic:1:12312",
      "Outdated top name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )
    val topic2 = Node(
      "urn:topic:1:99551",
      "Outdated other top name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Translation](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Translation], any)(using any)
    doReturn(Success(List(resource1, resource2, topic1, topic2)), Success(List.empty))
      .when(taxonomyApiClient)
      .queryNodes(id)
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any[(String, String)])

    taxonomyApiClient.updateTaxonomyIfExists(id, article, TokenUser.SystemUser) should be(Success(id))

    verify(taxonomyApiClient, times(1)).updateNode(
      eqTo(
        topic1.copy(
          name = "Norsk",
          translations = List(Translation("Norsk", "nb".some), Translation("Engelsk", "en".some)),
        )
      ),
      any,
    )
    verify(taxonomyApiClient, times(1)).updateNode(
      eqTo(
        topic2.copy(
          name = "Norsk",
          translations = List(Translation("Norsk", "nb".some), Translation("Engelsk", "en".some)),
        )
      ),
      any,
    )
    verify(taxonomyApiClient, times(1)).updateNode(
      eqTo(
        resource1.copy(
          name = "Norsk",
          translations = List(Translation("Norsk", "nb".some), Translation("Engelsk", "en".some)),
        )
      ),
      any,
    )
    verify(taxonomyApiClient, times(1)).updateNode(
      eqTo(
        resource2.copy(
          name = "Norsk",
          translations = List(Translation("Norsk", "nb".some), Translation("Engelsk", "en".some)),
        )
      ),
      any,
    )

    verify(taxonomyApiClient, times(4)).updateNode(any[Node], any)
  }

  test("That updateTaxonomyIfExists fails if updating translation fails") {
    val article   = TestData.sampleDomainArticle.copy(title = Seq(Title("Norsk", "nb"), Title("Engelsk", "en")))
    val id        = article.id.get
    val resource1 = Node(
      "urn:resource:1:12035",
      "Outdated res name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )
    val resource2 = Node(
      "urn:resource:1:d8a19b97-10ee-481a-b44c-dd54cffbddda",
      "Outdated other res name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )
    val topic1 = Node(
      "urn:topic:1:12312",
      "Outdated top name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )
    val topic2 = Node(
      "urn:topic:1:99551",
      "Outdated other top name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )

    doReturn(Success(List(resource1, resource2, topic1, topic2)), Success(List.empty))
      .when(taxonomyApiClient)
      .queryNodes(id)

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Node], any)(using any)
    doReturn(Failure(new TimeoutException), Failure(new TimeoutException))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Translation], any)(using any)
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any[(String, String)])

    taxonomyApiClient.updateTaxonomyIfExists(id, article, TokenUser.SystemUser).isFailure should be(true)
  }

  test("That updateTaxonomyIfExists fails if updating fetching nodes fails") {
    val article = TestData.sampleDomainArticle.copy(title = Seq(Title("Norsk", "nb"), Title("Engelsk", "en")))
    val id      = article.id.get

    doReturn(Failure(new RuntimeException("woawiwa")), Success(List.empty)).when(taxonomyApiClient).queryNodes(id)

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Translation](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Translation], any)(using any)

    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any[(String, String)])

    taxonomyApiClient.updateTaxonomyIfExists(id, article, TokenUser.SystemUser).isFailure should be(true)
  }

  test("That nothing happens (successfully) if no taxonomy exists") {
    val article = TestData.sampleDomainArticle.copy(title = Seq(Title("Norsk", "nb"), Title("Engelsk", "en")))
    val id      = article.id.get

    doReturn(Success(List.empty), Success(List.empty)).when(taxonomyApiClient).queryNodes(id)

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Translation](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Translation], any)(using any)
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any[(String, String)])

    taxonomyApiClient.updateTaxonomyIfExists(id, article, TokenUser.SystemUser).isSuccess should be(true)

    verify(taxonomyApiClient, times(0)).updateNode(any[Node], any)
    verify(taxonomyApiClient, times(0)).putRaw[Node](any, any, any)(using any)
  }

  test("That translations are deleted if found in taxonomy, but not in article") {
    val article  = TestData.sampleDomainArticle.copy(title = Seq(Title("Norsk", "nb"), Title("Engelsk", "en")))
    val id       = article.id.get
    val resource = Node(
      "urn:resource:1:12312",
      "Outdated name",
      Some(s"urn:article:$id"),
      List(Translation("Outdated name", "nb".some)),
    )

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Translation](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any[Translation], any)(using any)
    doReturn(Success(List(resource)), Success(List.empty)).when(taxonomyApiClient).queryNodes(id)
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any[(String, String)])

    taxonomyApiClient.updateTaxonomyIfExists(id, article, TokenUser.SystemUser) should be(Success(id))

    verify(taxonomyApiClient, times(1)).updateNode(
      eqTo(
        resource.copy(
          name = "Norsk",
          translations = List(Translation("Norsk", "nb".some), Translation("Engelsk", "en".some)),
        )
      ),
      any,
    )
  }
}
