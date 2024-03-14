/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import cats.Traverse
import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.Title
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.Props
import no.ndla.language.Language
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.network.{NdlaClient, TaxonomyData}
import org.jsoup.Jsoup
import sttp.client3.quick.*

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient with Props =>
  val taxonomyApiClient: TaxonomyApiClient
  import props.{DefaultLanguage, TaxonomyUrl, TaxonomyVersionHeader}

  class TaxonomyApiClient extends StrictLogging {
    private val TaxonomyApiEndpoint = s"$TaxonomyUrl/v1"
    private val taxonomyTimeout     = 20.seconds

    def updateTaxonomyIfExists(articleId: Long, article: Draft, user: TokenUser): Try[Long] = {
      for {
        nodes <- queryNodes(articleId)
        _     <- updateTaxonomy(nodes, article.title, user)
      } yield articleId
    }

    /** Updates the taxonomy for an article
      *
      * @param nodes
      *   Resources or Topics of article
      * @param titles
      *   Titles that are to be updated as translations
      * @param user
      *   The logged in user
      * @return
      *   List of Resources or Topics that were updated if none failed.
      */
    private def updateTaxonomy(nodes: Seq[Node], titles: Seq[Title], user: TokenUser): Try[List[Node]] = {
      Language.findByLanguageOrBestEffort(titles, DefaultLanguage) match {
        case Some(title) =>
          val updated = nodes.map(updateTitleAndTranslations(_, title, titles, user))
          updated
            .collect { case Failure(ex) => ex }
            .foreach(ex => logger.warn(s"Taxonomy update failed with: ${ex.getMessage}"))
          Traverse[List].sequence(updated.toList)
        case None => Failure(new RuntimeException("This is a bug, no name was found for published article."))
      }
    }

    private def updateTitleAndTranslations(
        node: Node,
        defaultTitle: Title,
        titles: Seq[Title],
        user: TokenUser
    ) = {
      val strippedTitles = titles.map(title => title.copy(title = Jsoup.parseBodyFragment(title.title).body().text()))
      val nodeResult     = updateNode(node.withName(Jsoup.parseBodyFragment(defaultTitle.title).body().text()), user)
      val translationResult = updateTranslations(node.id, strippedTitles, user)

      val deleteResult = getTranslations(node.id).flatMap(translations => {
        val translationsToDelete = translations.filterNot(trans => {
          strippedTitles.exists(title => trans.language.contains(title.language))
        })

        translationsToDelete.traverse(deleteTranslation(node.id, _, user))
      })

      (nodeResult, translationResult, deleteResult) match {
        case (Success(s1), Success(_), Success(_)) => Success(s1)
        case (Failure(ex), _, _)                   => Failure(ex)
        case (_, Failure(ex), _)                   => Failure(ex)
        case (_, _, Failure(ex))                   => Failure(ex)
      }
    }

    private def updateTranslations(
        id: String,
        titles: Seq[Title],
        user: TokenUser
    ) = {
      val tries = titles.map(t => updateNodeTranslation(id, t.language, t.title, user))
      Traverse[List].sequence(tries.toList)
    }

    private[integration] def updateNodeTranslation(nodeId: String, lang: String, name: String, user: TokenUser) =
      putRaw[Translation](s"$TaxonomyApiEndpoint/nodes/$nodeId/translations/$lang", Translation(name), user)

    private[integration] def updateNode(node: Node, user: TokenUser) =
      putRaw[Node](s"$TaxonomyApiEndpoint/nodes/${node.id}", node, user)

    private[integration] def getTranslations(nodeId: String) =
      get[List[Translation]](s"$TaxonomyApiEndpoint/nodes/$nodeId/translations")

    private def deleteTranslation(nodeId: String, translation: Translation, user: TokenUser) = {
      translation.language
        .map(language => {
          delete(s"$TaxonomyApiEndpoint/nodes/$nodeId/translations/$language", user)
        })
        .getOrElse({
          logger.info(s"Cannot delete translation without language for $nodeId")
          Success(())
        })
    }

    def updateTaxonomyMetadataIfExists(articleId: Long, visible: Boolean, user: TokenUser): Try[Long] = {
      for {
        nodes                      <- queryNodes(articleId)
        existingNodeMetadataWithId <- nodes.traverse(res => getMetadata(res.id).map((res.id, _)))
        _ <- existingNodeMetadataWithId.traverse { case (resId, existingMeta) =>
          updateMetadata(resId, existingMeta.copy(visible = visible), user)
        }
      } yield articleId
    }

    private def getMetadata(nodeId: String): Try[TaxonomyMetadata] = {
      get[TaxonomyMetadata](s"$TaxonomyApiEndpoint/nodes/$nodeId/metadata")
    }

    private def updateMetadata(
        nodeId: String,
        body: TaxonomyMetadata,
        user: TokenUser
    ): Try[TaxonomyMetadata] = {
      putRaw[TaxonomyMetadata](s"$TaxonomyApiEndpoint/nodes/$nodeId/metadata", body, user)
    }

    private def get[A: Decoder](url: String, params: (String, String)*): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest
          .get(uri"$url".withParams(params: _*))
          .readTimeout(taxonomyTimeout)
          .header(TaxonomyVersionHeader, TaxonomyData.get),
        None
      )
    }

    def queryNodes(articleId: Long): Try[List[Node]] =
      get[List[Node]](s"$TaxonomyApiEndpoint/nodes", "contentURI" -> s"urn:article:$articleId")

    def getNode(uri: String): Try[Node] = get[Node](s"$TaxonomyApiEndpoint/nodes/$uri")

    def getChildNodes(uri: String): Try[List[Node]] =
      get[List[Node]](s"$TaxonomyApiEndpoint/nodes/$uri/nodes", "recursive" -> "true")

    def getChildResources(uri: String): Try[List[Node]] =
      get[List[Node]](s"$TaxonomyApiEndpoint/nodes/$uri/resources")

    private[integration] def delete(url: String, user: TokenUser, params: (String, String)*): Try[Unit] =
      ndlaClient.fetchRawWithForwardedAuth(
        quickRequest
          .delete(uri"$url".withParams(params: _*))
          .readTimeout(taxonomyTimeout),
        Some(user)
      ) match {
        case Failure(ex) => Failure(ex)
        case Success(_)  => Success(())
      }

    private[integration] def putRaw[B <: AnyRef](url: String, data: B, user: TokenUser)(implicit d: Encoder[B]): Try[B] = {
      val uri = uri"$url"
      logger.info(s"Doing call to $uri")
      val request = quickRequest
        .put(uri)
        .body(CirceUtil.toJsonString(data))
        .readTimeout(taxonomyTimeout)
        .header(TaxonomyVersionHeader, TaxonomyData.get)
        .header("Content-Type", "application/json", replaceExisting = true)
      ndlaClient.fetchRawWithForwardedAuth(request, Some(user)) match {
        case Success(_)  => Success(data)
        case Failure(ex) => Failure(ex)
      }
    }
  }
}

case class Node(id: String, name: String, contentUri: Option[String], paths: List[String]) {
  def withName(name: String): Node = this.copy(name = name)
}
object Node {
  implicit val encoder: Encoder[Node] = deriveEncoder
  implicit val decoder: Decoder[Node] = deriveDecoder
}

case class TaxonomyMetadata(grepCodes: Seq[String], visible: Boolean)
object TaxonomyMetadata {
  implicit val encoder: Encoder[TaxonomyMetadata] = deriveEncoder
  implicit val decoder: Decoder[TaxonomyMetadata] = deriveDecoder
}

case class Translation(name: String, language: Option[String] = None)
object Translation {
  implicit val encoder: Encoder[Translation] = deriveEncoder
  implicit val decoder: Decoder[Translation] = deriveDecoder
}
