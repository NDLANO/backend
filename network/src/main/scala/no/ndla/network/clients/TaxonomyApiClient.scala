/*
 * Part of NDLA network
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import cats.Traverse
import cats.implicits.toTraverseOps
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.caching.Memoize
import no.ndla.common.errors.TaxonomyException
import no.ndla.common.model.domain.Title
import no.ndla.common.model.taxonomy.*
import no.ndla.language.Language
import no.ndla.network.NdlaClient
import no.ndla.network.TaxonomyData.{TAXONOMY_VERSION_HEADER, defaultVersion}
import no.ndla.network.model.RequestInfo
import no.ndla.network.tapir.auth.TokenUser
import org.jsoup.Jsoup
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.annotation.unused
import scala.collection.mutable.ListBuffer
import scala.concurrent.*
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success, Try}

class TaxonomyApiClient(taxonomyBaseUrl: String, defaultLanguage: String)(using
    ndlaClient: NdlaClient
) extends StrictLogging {
  private val TaxonomyApiEndpoint = s"$taxonomyBaseUrl/v1"
  private val timeoutSeconds      = 600.seconds

  def updateTaxonomyIfExists(
      shouldUsePublishedTax: Boolean,
      resourceId: Long,
      resourceType: String,
      titles: Seq[Title],
      user: TokenUser
  ): Try[Long] = {
    for {
      nodes <- queryNodes(shouldUsePublishedTax, resourceId, resourceType)
      _     <- updateTaxonomy(shouldUsePublishedTax, nodes, titles, user)
    } yield resourceId
  }

  private def updateTaxonomy(
      shouldUsePublishedTax: Boolean,
      nodes: Seq[Node],
      titles: Seq[Title],
      user: TokenUser
  ): Try[List[Node]] = {
    Language.findByLanguageOrBestEffort(titles, defaultLanguage) match {
      case Some(title) =>
        val updated = nodes.map(updateTitleAndTranslations(shouldUsePublishedTax, _, title, titles, user))
        updated
          .collect { case Failure(ex) => ex }
          .foreach(ex => logger.warn(s"Taxonomy update failed with: ${ex.getMessage}"))
        Traverse[List].sequence(updated.toList)
      case None => Failure(new RuntimeException("This is a bug, no name was found for published article."))
    }
  }

  private def updateTitleAndTranslations(
      shouldUsePublishedTax: Boolean,
      node: Node,
      defaultTitle: Title,
      titles: Seq[Title],
      user: TokenUser
  ) = {
    val strippedTitles = titles.map(title => title.copy(title = Jsoup.parseBodyFragment(title.title).body().text()))
    val nodeResult     =
      updateNode(shouldUsePublishedTax, node.withName(Jsoup.parseBodyFragment(defaultTitle.title).body().text()), user)
    val translationResult = updateTranslations(shouldUsePublishedTax, node.id, strippedTitles, user)

    val deleteResult = getTranslations(shouldUsePublishedTax, node.id).flatMap(translations => {
      val translationsToDelete = translations.filterNot(trans => {
        strippedTitles.exists(title => trans.language.contains(title.language))
      })

      translationsToDelete.traverse(deleteTranslation(shouldUsePublishedTax, node.id, _, user))
    })

    (nodeResult, translationResult, deleteResult) match {
      case (Success(s1), Success(_), Success(_)) => Success(s1)
      case (Failure(ex), _, _)                   => Failure(ex)
      case (_, Failure(ex), _)                   => Failure(ex)
      case (_, _, Failure(ex))                   => Failure(ex)
    }
  }

  private def updateTranslations(
      shouldUsePublishedTax: Boolean,
      id: String,
      titles: Seq[Title],
      user: TokenUser
  ) = {
    val tries = titles.map(t => updateNodeTranslation(shouldUsePublishedTax, id, t.language, t.title, user))
    Traverse[List].sequence(tries.toList)
  }

  private def updateNodeTranslation(
      shouldUsePublishedTax: Boolean,
      nodeId: String,
      lang: String,
      name: String,
      user: TokenUser
  ) =
    putRaw[TaxonomyTranslation](
      s"$TaxonomyApiEndpoint/nodes/$nodeId/translations/$lang",
      headers = getVersionHashHeader(shouldUsePublishedTax),
      TaxonomyTranslation(name, lang),
      user
    )

  private def updateNode(shouldUsePublishedTax: Boolean, node: Node, user: TokenUser) =
    putRaw[Node](
      s"$TaxonomyApiEndpoint/nodes/${node.id}",
      headers = getVersionHashHeader(shouldUsePublishedTax),
      node,
      user
    )

  private def getTranslations(shouldUsePublishedTax: Boolean, nodeId: String) =
    get[List[TaxonomyTranslation]](
      s"$TaxonomyApiEndpoint/nodes/$nodeId/translations",
      headers = getVersionHashHeader(shouldUsePublishedTax)
    )

  private def deleteTranslation(
      shouldUsePublishedTax: Boolean,
      nodeId: String,
      translation: TaxonomyTranslation,
      user: TokenUser
  ) = {
    delete(
      s"$TaxonomyApiEndpoint/nodes/$nodeId/translations/${translation.language}",
      headers = getVersionHashHeader(shouldUsePublishedTax),
      user
    )
  }

  def queryNodes(shouldUsePublishedTax: Boolean, resourceId: Long, resourceType: String): Try[List[Node]] =
    get[List[Node]](
      s"$TaxonomyApiEndpoint/nodes",
      headers = getVersionHashHeader(shouldUsePublishedTax),
      "contentURI" -> s"urn:$resourceType:$resourceId"
    )

  def getNode(shouldUsePublishedTax: Boolean, uri: String): Try[Node] =
    get[Node](s"$TaxonomyApiEndpoint/nodes/$uri", getVersionHashHeader(shouldUsePublishedTax))

  def getChildNodes(shouldUsePublishedTax: Boolean, uri: String): Try[List[Node]] =
    get[List[Node]](
      s"$TaxonomyApiEndpoint/nodes/$uri/nodes",
      getVersionHashHeader(shouldUsePublishedTax),
      "recursive" -> "true"
    )

  private def getNodes(shouldUsePublishedTax: Boolean): Try[ListBuffer[Node]] =
    get[ListBuffer[Node]](
      s"$TaxonomyApiEndpoint/nodes/",
      headers = getVersionHashHeader(shouldUsePublishedTax),
      "nodeType"        -> List(NodeType.NODE, NodeType.SUBJECT, NodeType.TOPIC).mkString(","),
      "includeContexts" -> "true",
      "isVisible"       -> getIsVisibleParam(shouldUsePublishedTax)
    )

  private def getResources(shouldUsePublishedTax: Boolean): Try[List[Node]] =
    getPaginated[Node](
      s"$TaxonomyApiEndpoint/nodes/search",
      headers = getVersionHashHeader(shouldUsePublishedTax),
      "pageSize"        -> "500",
      "nodeType"        -> NodeType.RESOURCE.toString,
      "includeContexts" -> "true",
      "isVisible"       -> getIsVisibleParam(shouldUsePublishedTax)
    )

  def getTaxonomyContext(
      contentUri: String,
      filterVisibles: Boolean,
      filterContexts: Boolean,
      shouldUsePublishedTax: Boolean
  ): Try[List[TaxonomyContext]] = {
    val contexts = get[List[TaxonomyContext]](
      s"$TaxonomyApiEndpoint/queries/$contentUri",
      headers = getVersionHashHeader(shouldUsePublishedTax),
      params = ("filterVisibles" -> filterVisibles.toString)
    )
    if (filterContexts) contexts.map(list => list.filter(c => c.rootId.contains("subject"))) else contexts
  }

  private def getIsVisibleParam(shouldUsePublishedTax: Boolean) = {
    if (shouldUsePublishedTax) "" else "false"
  }

  private def getVersionHashHeader(shouldUsePublishedTax: Boolean): Map[String, String] = {
    if (shouldUsePublishedTax) Map.empty else Map(TAXONOMY_VERSION_HEADER -> defaultVersion)
  }

  val getTaxonomyBundle: Memoize[Boolean, Try[TaxonomyBundle]] =
    new Memoize(1000 * 60, shouldUsePublishedTax => getTaxonomyBundleUncached(shouldUsePublishedTax))

  /** The memoized function of this [[getTaxonomyBundle]] should probably be used in most cases */
  def getTaxonomyBundleUncached(shouldUsePublishedTax: Boolean): Try[TaxonomyBundle] = {
    logger.info(s"Fetching ${if (shouldUsePublishedTax) "published" else "draft"} taxonomy in bulk...")
    val startFetch                            = System.currentTimeMillis()
    implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

    val requestInfo = RequestInfo.fromThreadContext()

    /** Calls function in separate thread and converts Try to Future */
    def tryToFuture[T](x: Boolean => Try[T]) = Future {
      requestInfo.setRequestInfo(): Unit
      x(shouldUsePublishedTax)
    }.flatMap(Future.fromTry)

    val nodes     = tryToFuture(shouldUsePublishedTax => getNodes(shouldUsePublishedTax))
    val resources = tryToFuture(shouldUsePublishedTax => getResources(shouldUsePublishedTax))

    val x = for {
      n <- nodes
      r <- resources
    } yield TaxonomyBundle(n.addAll(r).result())

    Try(Await.result(x, Duration(300, "seconds"))) match {
      case Success(bundle) =>
        logger.info(s"Fetched taxonomy in ${System.currentTimeMillis() - startFetch}ms...")
        Success(bundle)
      case Failure(ex) =>
        logger.error(s"Could not fetch taxonomy bundle (${ex.getMessage})", ex)
        Failure(TaxonomyException("Could not fetch taxonomy bundle..."))
    }
  }

  private def putRaw[B <: AnyRef](url: String, headers: Map[String, String], data: B, user: TokenUser)(implicit
      d: Encoder[B]
  ): Try[B] = {
    val uri = uri"$url"
    logger.info(s"Doing call to $uri")
    val request = quickRequest
      .put(uri)
      .body(CirceUtil.toJsonString(data))
      .readTimeout(timeoutSeconds)
      .headers(headers)
    ndlaClient.fetchRawWithForwardedAuth(request, Some(user)) match {
      case Success(_)  => Success(data)
      case Failure(ex) => Failure(ex)
    }
  }

  private def delete(
      url: String,
      headers: Map[String, String],
      user: TokenUser,
      params: (String, String)*
  ): Try[Unit] =
    ndlaClient.fetchRawWithForwardedAuth(
      quickRequest
        .delete(uri"$url".withParams(params*))
        .headers(headers)
        .readTimeout(timeoutSeconds),
      Some(user)
    ) match {
      case Failure(ex) => Failure(ex)
      case Success(_)  => Success(())
    }

  private def get[A: Decoder](url: String, headers: Map[String, String], params: (String, String)*): Try[A] = {
    ndlaClient.fetchWithForwardedAuth[A](
      quickRequest.get(uri"$url".withParams(params*)).headers(headers).readTimeout(timeoutSeconds),
      None
    )
  }

  private def getPaginated[T: Decoder](
      url: String,
      headers: Map[String, String],
      params: (String, String)*
  ): Try[List[T]] = {
    def fetchPage(p: (String, String)*): Try[PaginationPage[T]] =
      get[PaginationPage[T]](url, headers, p*)

    val pageSize   = params.toMap.getOrElse("pageSize", "100").toInt
    val pageParams = params :+ ("page" -> "1")
    fetchPage(pageParams*).flatMap(firstPage => {
      val numPages  = Math.ceil(firstPage.totalCount.toDouble / pageSize.toDouble).toInt
      val pageRange = 1 to numPages

      val numThreads                                                 = Math.max(20, numPages)
      implicit val executionContext: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numThreads))

      val pages        = pageRange.map(pageNum => Future(fetchPage((params :+ ("page" -> s"$pageNum"))*)))
      val mergedFuture = Future.sequence(pages)
      val awaited      = Await.result(mergedFuture, timeoutSeconds)

      awaited.toList.sequence.map(_.flatMap(_.results))
    })
  }
}

case class PaginationPage[T](totalCount: Long, results: List[T])
object PaginationPage {
  implicit def encoder[T](implicit @unused e: Encoder[T]): Encoder[PaginationPage[T]] = deriveEncoder
  implicit def decoder[T](implicit @unused d: Decoder[T]): Decoder[PaginationPage[T]] = deriveDecoder
}
