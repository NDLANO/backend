/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import cats.Traverse
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.Title
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.Props
import no.ndla.language.Language
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.network.{NdlaClient, TaxonomyData}
import org.json4s.jackson.Serialization.write
import org.json4s.{DefaultFormats, Formats}
import sttp.client3.quick._

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient with Props =>
  val taxonomyApiClient: TaxonomyApiClient
  import props.{DefaultLanguage, TaxonomyUrl, TaxonomyVersionHeader}

  class TaxonomyApiClient extends StrictLogging {
    private val TaxonomyApiEndpoint           = s"$TaxonomyUrl/v1"
    private val taxonomyTimeout               = 20.seconds
    implicit val formats: DefaultFormats.type = DefaultFormats

    def updateTaxonomyIfExists(articleId: Long, article: Draft, user: TokenUser): Try[Long] = {
      for {
        resources <- queryResource(articleId)
        _         <- updateTaxonomy[Resource](resources, article.title, updateResourceTitleAndTranslations, user)
        topics    <- queryTopic(articleId)
        _         <- updateTaxonomy[Topic](topics, article.title, updateTopicTitleAndTranslations, user)
      } yield articleId
    }

    /** Updates the taxonomy for an article
      *
      * @param resource
      *   Resources or Topics of article
      * @param titles
      *   Titles that are to be updated as translations
      * @param updateFunc
      *   Function that updates taxonomy and translations ([[updateResourceTitleAndTranslations]] or
      *   [[updateTopicTitleAndTranslations]])
      * @tparam T
      *   Taxonomy resource type ([[Resource]] or [[Topic]])
      * @return
      *   List of Resources or Topics that were updated if none failed.
      */
    private def updateTaxonomy[T](
        resource: Seq[T],
        titles: Seq[Title],
        updateFunc: (T, Title, Seq[Title], TokenUser) => Try[T],
        user: TokenUser
    ): Try[List[T]] = {
      Language.findByLanguageOrBestEffort(titles, DefaultLanguage) match {
        case Some(title) =>
          val updated = resource.map(updateFunc(_, title, titles, user))
          updated
            .collect { case Failure(ex) => ex }
            .foreach(ex => logger.warn(s"Taxonomy update failed with: ${ex.getMessage}"))
          Traverse[List].sequence(updated.toList)
        case None => Failure(new RuntimeException("This is a bug, no name was found for published article."))
      }
    }

    private def updateTitleAndTranslations[T <: Taxonomy[T]](
        res: T,
        defaultTitle: Title,
        titles: Seq[Title],
        updateFunc: (T, TokenUser) => Try[T],
        updateTranslationsFunc: Seq[Title] => Try[List[Translation]],
        getTranslationsFunc: String => Try[List[Translation]],
        deleteTranslationFunc: Translation => Try[Unit],
        user: TokenUser
    ) = {
      val resourceResult    = updateFunc(res.withName(defaultTitle.title), user)
      val translationResult = updateTranslationsFunc(titles)

      val deleteResult = getTranslationsFunc(res.id).flatMap(translations => {
        val translationsToDelete = translations.filterNot(trans => {
          titles.exists(title => trans.language.contains(title.language))
        })

        translationsToDelete.traverse(deleteTranslationFunc)
      })

      (resourceResult, translationResult, deleteResult) match {
        case (Success(s1), Success(_), Success(_)) => Success(s1)
        case (Failure(ex), _, _)                   => Failure(ex)
        case (_, Failure(ex), _)                   => Failure(ex)
        case (_, _, Failure(ex))                   => Failure(ex)
      }
    }

    private def updateTranslations(
        id: String,
        titles: Seq[Title],
        user: TokenUser,
        updateTranslationFunc: (String, String, String, TokenUser) => Try[Translation]
    ) = {
      val tries = titles.map(t => updateTranslationFunc(id, t.language, t.title, user))
      Traverse[List].sequence(tries.toList)
    }

    private def updateResourceTitleAndTranslations(
        res: Resource,
        defaultTitle: Title,
        titles: Seq[Title],
        user: TokenUser
    ) = {
      val updateTranslationsFunc = updateTranslations(res.id, _: Seq[Title], user, updateResourceTranslation)
      updateTitleAndTranslations(
        res,
        defaultTitle,
        titles,
        updateResource,
        updateTranslationsFunc,
        getResourceTranslations,
        (t: Translation) => deleteResourceTranslation(res.id, t, user),
        user
      )
    }

    private def updateTopicTitleAndTranslations(
        top: Topic,
        defaultTitle: Title,
        titles: Seq[Title],
        user: TokenUser
    ) = {
      val updateTranslationsFunc = updateTranslations(top.id, _: Seq[Title], user, updateTopicTranslation)
      updateTitleAndTranslations(
        top,
        defaultTitle,
        titles,
        updateTopic,
        updateTranslationsFunc,
        getTopicTranslations,
        (t: Translation) => deleteTopicTranslation(top.id, t, user),
        user
      )
    }

    private[integration] def updateResourceTranslation(
        resourceId: String,
        lang: String,
        name: String,
        user: TokenUser
    ) =
      putRaw(s"$TaxonomyApiEndpoint/resources/$resourceId/translations/$lang", Translation(name), user)

    private[integration] def updateTopicTranslation(topicId: String, lang: String, name: String, user: TokenUser) =
      putRaw(s"$TaxonomyApiEndpoint/topics/$topicId/translations/$lang", Translation(name), user)

    private[integration] def updateResource(resource: Resource, user: TokenUser)(implicit formats: Formats) =
      putRaw[Resource](s"$TaxonomyApiEndpoint/resources/${resource.id}", resource, user)

    private[integration] def updateTopic(topic: Topic, user: TokenUser)(implicit formats: Formats) =
      putRaw[Topic](s"$TaxonomyApiEndpoint/topics/${topic.id}", topic, user)

    private[integration] def getTopicTranslations(topicId: String) =
      get[List[Translation]](s"$TaxonomyApiEndpoint/topics/$topicId/translations")

    private def deleteTopicTranslation(topicId: String, translation: Translation, user: TokenUser) = {
      translation.language
        .map(language => {
          delete(s"$TaxonomyApiEndpoint/topics/$topicId/translations/$language", user)
        })
        .getOrElse({
          logger.info(s"Cannot delete translation without language for $topicId")
          Success(())
        })
    }

    private[integration] def getResourceTranslations(resourceId: String) =
      get[List[Translation]](s"$TaxonomyApiEndpoint/resources/$resourceId/translations")

    private[integration] def deleteResourceTranslation(
        resourceId: String,
        translation: Translation,
        user: TokenUser
    ) = {
      translation.language
        .map(language => {
          delete(s"$TaxonomyApiEndpoint/resources/$resourceId/translations/$language", user)
        })
        .getOrElse({
          logger.info(s"Cannot delete translation without language for $resourceId")
          Success(())
        })
    }

    def updateTaxonomyMetadataIfExists(articleId: Long, visible: Boolean, user: TokenUser): Try[Long] = {
      for {
        resources                      <- queryResource(articleId)
        existingResourceMetadataWithId <- resources.traverse(res => getResourceMetadata(res.id).map((res.id, _)))
        _ <- existingResourceMetadataWithId.traverse { case (resId, existingMeta) =>
          updateResourceMetadata(resId, existingMeta.copy(visible = visible), user)
        }

        topics                      <- queryTopic(articleId)
        existingTopicMetadataWithId <- topics.traverse(top => getTopicMetadata(top.id).map((top.id, _)))
        _ <- existingTopicMetadataWithId.traverse { case (topId, existingMeta) =>
          updateTopicMetadata(topId, existingMeta.copy(visible = visible), user)
        }
      } yield articleId
    }

    private def getResourceMetadata(resourceId: String): Try[TaxonomyMetadata] = {
      get[TaxonomyMetadata](s"$TaxonomyApiEndpoint/resources/$resourceId/metadata")
    }

    private def getTopicMetadata(resourceId: String): Try[TaxonomyMetadata] = {
      get[TaxonomyMetadata](s"$TaxonomyApiEndpoint/topics/$resourceId/metadata")
    }

    private def updateResourceMetadata(
        resourceId: String,
        body: TaxonomyMetadata,
        user: TokenUser
    ): Try[TaxonomyMetadata] = {
      putRaw[TaxonomyMetadata](s"$TaxonomyApiEndpoint/resources/$resourceId/metadata", body, user)
    }

    private def updateTopicMetadata(
        resourceId: String,
        body: TaxonomyMetadata,
        user: TokenUser
    ): Try[TaxonomyMetadata] = {
      putRaw[TaxonomyMetadata](s"$TaxonomyApiEndpoint/topics/$resourceId/metadata", body, user)
    }

    private def get[A](url: String, params: (String, String)*)(implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest
          .get(uri"$url".withParams(params: _*))
          .readTimeout(taxonomyTimeout)
          .header(TaxonomyVersionHeader, TaxonomyData.get),
        None
      )
    }

    def queryResource(articleId: Long): Try[List[Resource]] =
      get[List[Resource]](s"$TaxonomyApiEndpoint/resources", "contentURI" -> s"urn:article:$articleId")

    def queryTopic(articleId: Long): Try[List[Topic]] =
      get[List[Topic]](s"$TaxonomyApiEndpoint/topics", "contentURI" -> s"urn:article:$articleId")

    def getNode(uri: String): Try[Topic] = get[Topic](s"$TaxonomyApiEndpoint/nodes/${uri}")

    def getChildNodes(uri: String): Try[List[Topic]] =
      get[List[Topic]](s"$TaxonomyApiEndpoint/nodes/${uri}/nodes", "recursive" -> "true")

    def getChildResources(uri: String): Try[List[Resource]] =
      get[List[Resource]](s"$TaxonomyApiEndpoint/nodes/${uri}/resources")

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

    private[integration] def putRaw[B <: AnyRef](url: String, data: B, user: TokenUser, params: (String, String)*)(
        implicit formats: org.json4s.Formats
    ): Try[B] = {
      val uri = uri"$url".withParams(params: _*)
      logger.info(s"Doing call to $uri")
      val request = quickRequest
        .put(uri)
        .body(write(data))
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

trait Taxonomy[E <: Taxonomy[E]] {
  val id: String
  def name: String
  def contentUri: Option[String]
  def withName(name: String): E
}
case class Resource(id: String, name: String, contentUri: Option[String], paths: List[String])
    extends Taxonomy[Resource] {
  def withName(name: String): Resource = this.copy(name = name)
}
case class Topic(id: String, name: String, contentUri: Option[String], paths: List[String]) extends Taxonomy[Topic] {
  def withName(name: String): Topic = this.copy(name = name)
}

case class TaxonomyMetadata(grepCodes: Seq[String], visible: Boolean)
case class Translation(name: String, language: Option[String] = None)
