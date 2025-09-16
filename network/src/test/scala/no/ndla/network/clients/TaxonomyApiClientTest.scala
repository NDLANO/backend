/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import no.ndla.common.model.domain.Title
import no.ndla.common.model.taxonomy.{Node, NodeType, TaxonomyTranslation}
import no.ndla.network.clients.TaxonomyApiClient
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.network.{NdlaClient, UnitSuite}
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.*
import org.mockito.invocation.InvocationOnMock

import scala.concurrent.TimeoutException
import scala.util.{Failure, Success}

class TaxonomyApiClientTest extends UnitSuite {

  lazy val taxonomyApiClient: TaxonomyApiClient = spy(
    new TaxonomyApiClient("taxonomy", "nb")(using NdlaClient())
  )

  val defaultNode: Node = Node(
    id = "urn:resource:1",
    name = "Name",
    contentUri = Some(s"urn:article:1"),
    path = Some(s"/subject:1/resource:1"),
    url = Some("/r/name/12345678"),
    metadata = None,
    translations = List.empty,
    nodeType = NodeType.RESOURCE,
    contextids = List.empty,
    context = None,
    contexts = List.empty
  )

  override def beforeEach(): Unit = {
    // Since we use spy, we reset the mock before each test allowing verify to be accurate
    reset(taxonomyApiClient)
  }

  test("That updating one nodes translations works as expected") {
    val title = Seq(
      Title("Norsk", "nb"),
      Title("<strong>Engelsk</strong>", "en")
    )
    val id   = 1
    val node =
      defaultNode.copy(
        id = "urn:resource:1:12312",
        name = "Outdated name",
        contentUri = Some(s"urn:article:$id")
      )

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](2)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[TaxonomyTranslation](2)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[TaxonomyTranslation], any)(using any)
    doReturn(Success(List(node)), Success(List.empty))
      .when(taxonomyApiClient)
      .queryNodes(any[Boolean], any[Long], any[String])
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any, any)
    doReturn(Success(List.empty), Success(List.empty))
      .when(taxonomyApiClient)
      .getTranslations(any[Boolean], any[String])

    taxonomyApiClient.updateTaxonomyIfExists(false, id, "article", title, TokenUser.SystemUser) should be(
      Success(id)
    )

    verify(taxonomyApiClient, times(1)).updateNode(any[Boolean], eqTo(node.copy(name = "Norsk")), any)
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(node.id),
      eqTo("nb"),
      eqTo("Norsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(node.id),
      eqTo("en"),
      eqTo("Engelsk"),
      any
    ) // HTML is stripped
    verify(taxonomyApiClient, times(2)).updateNodeTranslation(any[Boolean], anyString, anyString, anyString, any)
  }

  test("That updating multiple nodes translations works as expected") {
    val title = Seq(
      Title("Norsk", "nb"),
      Title("Engelsk", "en")
    )
    val id   = 1L
    val node =
      defaultNode.copy(
        id = "urn:resource:1:12312",
        name = "Outdated name",
        contentUri = Some(s"urn:article:$id")
      )
    val node2 =
      defaultNode.copy(
        id = "urn:resource:1:99551",
        name = "Outdated other name",
        contentUri = Some(s"urn:article:$id")
      )

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](2)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[TaxonomyTranslation](2)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[TaxonomyTranslation], any)(using any)
    doReturn(Success(List(node, node2)), Success(List.empty))
      .when(taxonomyApiClient)
      .queryNodes(any[Boolean], any[Long], any[String])
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any, any[(String, String)])
    doReturn(Success(List.empty), Success(List.empty))
      .when(taxonomyApiClient)
      .getTranslations(any[Boolean], any[String])

    taxonomyApiClient.updateTaxonomyIfExists(false, id, "article", title, TokenUser.SystemUser) should be(Success(id))

    verify(taxonomyApiClient, times(1)).updateNode(any[Boolean], eqTo(node.copy(name = "Norsk")), any)
    verify(taxonomyApiClient, times(1)).updateNode(any[Boolean], eqTo(node2.copy(name = "Norsk")), any)
    verify(taxonomyApiClient, times(2)).updateNode(any[Boolean], any[Node], any)
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(node.id),
      eqTo("nb"),
      eqTo("Norsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(node.id),
      eqTo("en"),
      eqTo("Engelsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(node2.id),
      eqTo("nb"),
      eqTo("Norsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(node2.id),
      eqTo("en"),
      eqTo("Engelsk"),
      any
    )
    verify(taxonomyApiClient, times(4)).updateNodeTranslation(any[Boolean], anyString, anyString, anyString, any)
  }

  test("That both resources and topics for single article is updated") {
    val title = Seq(
      Title("Norsk", "nb"),
      Title("Engelsk", "en")
    )
    val id        = 1L
    val resource1 =
      defaultNode.copy(
        id = "urn:resource:1:12312",
        name = "Outdated name",
        contentUri = Some(s"urn:article:$id")
      )
    val resource2 =
      defaultNode.copy(
        id = "urn:resource:1:d8a19b97-10ee-481a-b44c-dd54cffbddda",
        name = "Outdated other name",
        contentUri = Some(s"urn:article:$id")
      )
    val topic1 =
      defaultNode.copy(
        id = "urn:topic:1:12312",
        name = "Outdated topic name",
        contentUri = Some(s"urn:article:$id")
      )
    val topic2 =
      defaultNode.copy(
        id = "urn:resource:1:99551",
        name = "Outdated other topic name",
        contentUri = Some(s"urn:article:$id")
      )

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](2)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](2)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[TaxonomyTranslation](2)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[TaxonomyTranslation], any)(using any)
    doReturn(Success(List(resource1, resource2, topic1, topic2)), Success(List.empty))
      .when(taxonomyApiClient)
      .queryNodes(false, id, "article")
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any, any[(String, String)])
    doReturn(Success(List.empty), Success(List.empty))
      .when(taxonomyApiClient)
      .getTranslations(any[Boolean], any[String])

    taxonomyApiClient.updateTaxonomyIfExists(false, id, "article", title, TokenUser.SystemUser) should be(Success(id))

    verify(taxonomyApiClient, times(1)).updateNode(any[Boolean], eqTo(topic1.copy(name = "Norsk")), any)
    verify(taxonomyApiClient, times(1)).updateNode(any[Boolean], eqTo(topic2.copy(name = "Norsk")), any)
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(topic1.id),
      eqTo("nb"),
      eqTo("Norsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(topic1.id),
      eqTo("en"),
      eqTo("Engelsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(topic2.id),
      eqTo("nb"),
      eqTo("Norsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(topic2.id),
      eqTo("en"),
      eqTo("Engelsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNode(any[Boolean], eqTo(resource1.copy(name = "Norsk")), any)
    verify(taxonomyApiClient, times(1)).updateNode(any[Boolean], eqTo(resource2.copy(name = "Norsk")), any)
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(resource1.id),
      eqTo("nb"),
      eqTo("Norsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(resource1.id),
      eqTo("en"),
      eqTo("Engelsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(resource2.id),
      eqTo("nb"),
      eqTo("Norsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(resource2.id),
      eqTo("en"),
      eqTo("Engelsk"),
      any
    )

    verify(taxonomyApiClient, times(4)).updateNode(any[Boolean], any[Node], any)
    verify(taxonomyApiClient, times(8)).updateNodeTranslation(any[Boolean], anyString, anyString, anyString, any)
  }

  test("That updateTaxonomyIfExists fails if updating translation fails") {
    val title = Seq(
      Title("Norsk", "nb"),
      Title("Engelsk", "en")
    )
    val id        = 1L
    val resource1 = defaultNode.copy(
      id = "urn:resource:1:12035",
      name = "Outdated res name",
      contentUri = Some(s"urn:article:$id")
    )
    val resource2 = defaultNode.copy(
      id = "urn:resource:1:d8a19b97-10ee-481a-b44c-dd54cffbddda",
      name = "Outdated other res name",
      contentUri = Some(s"urn:article:$id")
    )
    val topic1 =
      defaultNode.copy(
        id = "urn:topic:1:12312",
        name = "Outdated top name",
        contentUri = Some(s"urn:article:$id")
      )
    val topic2 =
      defaultNode.copy(
        id = "urn:topic:1:99551",
        name = "Outdated other top name",
        contentUri = Some(s"urn:article:$id")
      )

    doReturn(Success(List(resource1, resource2, topic1, topic2)), Success(List.empty))
      .when(taxonomyApiClient)
      .queryNodes(false, id, "article")

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[Node], any)(using any)
    doReturn(Failure(new TimeoutException), Failure(new TimeoutException))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[TaxonomyTranslation], any)(using any)
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any, any[(String, String)])
    doReturn(Success(List.empty), Success(List.empty))
      .when(taxonomyApiClient)
      .getTranslations(any[Boolean], any[String])

    taxonomyApiClient.updateTaxonomyIfExists(false, id, "article", title, TokenUser.SystemUser).isFailure should be(
      true
    )
  }

  test("That updateTaxonomyIfExists fails if updating fetching nodes fails") {
    val title = Seq(
      Title("Norsk", "nb"),
      Title("Engelsk", "en")
    )
    val id = 1L

    doReturn(Failure(new RuntimeException("woawiwa")), Success(List.empty))
      .when(taxonomyApiClient)
      .queryNodes(any[Boolean], any[Long], any[String])

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[TaxonomyTranslation](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[TaxonomyTranslation], any)(using any)

    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any, any[(String, String)])
    doReturn(Success(List.empty), Success(List.empty))
      .when(taxonomyApiClient)
      .getTranslations(any[Boolean], any[String])

    taxonomyApiClient.updateTaxonomyIfExists(false, id, "article", title, TokenUser.SystemUser).isFailure should be(
      true
    )
  }

  test("That nothing happens (successfully) if no taxonomy exists") {
    val title = Seq(
      Title("Norsk", "nb"),
      Title("Engelsk", "en")
    )
    val id = 1L

    doReturn(Success(List.empty), Success(List.empty))
      .when(taxonomyApiClient)
      .queryNodes(any[Boolean], any[Long], any[String])

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[TaxonomyTranslation](1)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[TaxonomyTranslation], any)(using any)
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any, any[(String, String)])
    doReturn(Success(List.empty), Success(List.empty))
      .when(taxonomyApiClient)
      .getTranslations(any[Boolean], any[String])

    taxonomyApiClient.updateTaxonomyIfExists(false, id, "article", title, TokenUser.SystemUser).isSuccess should be(
      true
    )

    verify(taxonomyApiClient, times(0)).updateNode(any[Boolean], any[Node], any)
    verify(taxonomyApiClient, times(0)).updateNodeTranslation(any[Boolean], anyString, anyString, anyString, any)
    verify(taxonomyApiClient, times(0)).putRaw[Node](any, any, any, any)(using any)
  }

  test("That translations are deleted if found in taxonomy, but not in article") {
    val title = Seq(
      Title("Norsk", "nb"),
      Title("Engelsk", "en")
    )

    val id       = 1L
    val resource =
      defaultNode.copy(
        id = "urn:resource:1:12312",
        name = "Outdated name",
        contentUri = Some(s"urn:article:$id")
      )

    doAnswer((i: InvocationOnMock) => Success(i.getArgument[Node](2)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[Node], any)(using any)
    doAnswer((i: InvocationOnMock) => Success(i.getArgument[TaxonomyTranslation](2)))
      .when(taxonomyApiClient)
      .putRaw(any[String], any, any[TaxonomyTranslation], any)(using any)
    doReturn(Success(List(resource)), Success(List.empty))
      .when(taxonomyApiClient)
      .queryNodes(any[Boolean], any[Long], any[String])
    doReturn(Success(()), Success(())).when(taxonomyApiClient).delete(any[String], any, any, any[(String, String)])
    doReturn(Success(List(TaxonomyTranslation("yolo", "nn"))), Success(TaxonomyTranslation("yolo", "nn")))
      .when(taxonomyApiClient)
      .getTranslations(any[Boolean], any[String])

    taxonomyApiClient.updateTaxonomyIfExists(false, id, "article", title, TokenUser.SystemUser) should be(Success(id))

    verify(taxonomyApiClient, times(1)).updateNode(any[Boolean], eqTo(resource.copy(name = "Norsk")), any)
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(resource.id),
      eqTo("nb"),
      eqTo("Norsk"),
      any
    )
    verify(taxonomyApiClient, times(1)).updateNodeTranslation(
      any[Boolean],
      eqTo(resource.id),
      eqTo("en"),
      eqTo("Engelsk"),
      any
    )
    verify(taxonomyApiClient, times(2)).updateNodeTranslation(any[Boolean], anyString, anyString, anyString, any)

    verify(taxonomyApiClient, times(1)).delete(
      eqTo(s"taxonomy/v1/nodes/urn:resource:1:12312/translations/nn"),
      any,
      any,
      any[(String, String)]
    )
  }
}
