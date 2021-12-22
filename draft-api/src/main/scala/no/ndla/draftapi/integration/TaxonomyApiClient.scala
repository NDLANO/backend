/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import cats.Traverse
import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.{AuthUser, NdlaClient}
import no.ndla.draftapi.DraftApiProperties.{ApiGatewayHost, DefaultLanguage}
import no.ndla.draftapi.model.domain.{Article, ArticleTitle, Language}
import org.json4s.{DefaultFormats, Formats}
import scalaj.http.Http
import org.json4s.jackson.Serialization.write

import scala.util.{Failure, Success, Try}
import cats.implicits._

trait TaxonomyApiClient {
  this: NdlaClient =>
  val taxonomyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends LazyLogging {
    private val TaxonomyApiEndpoint = s"http://$ApiGatewayHost/taxonomy/v1"
    private val taxonomyTimeout = 20 * 1000 // 20 Seconds
    implicit val formats: DefaultFormats.type = DefaultFormats

    def updateTaxonomyIfExists(articleId: Long, article: Article): Try[Long] = {
      for {
        resources <- queryResource(articleId)
        _ <- updateTaxonomy[Resource](resources, article.title, updateResourceTitleAndTranslations)
        topics <- queryTopic(articleId)
        _ <- updateTaxonomy[Topic](topics, article.title, updateTopicTitleAndTranslations)
      } yield articleId
    }

    /**
      * Updates the taxonomy for an article
      *
      * @param resource Resources or Topics of article
      * @param titles Titles that are to be updated as translations
      * @param updateFunc Function that updates taxonomy and translations ([[updateResourceTitleAndTranslations]] or [[updateTopicTitleAndTranslations]])
      * @tparam T Taxonomy resource type ([[Resource]] or [[Topic]])
      * @return List of Resources or Topics that were updated if none failed.
      */
    private def updateTaxonomy[T](resource: Seq[T],
                                  titles: Seq[ArticleTitle],
                                  updateFunc: (T, ArticleTitle, Seq[ArticleTitle]) => Try[T]): Try[List[T]] = {
      Language.findByLanguageOrBestEffort(titles, DefaultLanguage) match {
        case Some(title) =>
          val updated = resource.map(updateFunc(_, title, titles))
          updated
            .collect { case Failure(ex) => ex }
            .foreach(ex => logger.warn(s"Taxonomy update failed with: ${ex.getMessage}"))
          Traverse[List].sequence(updated.toList)
        case None => Failure(new RuntimeException("This is a bug, no name was found for published article."))
      }
    }

    private def updateTitleAndTranslations[T <: Taxonomy[T]](
        res: T,
        defaultTitle: ArticleTitle,
        titles: Seq[ArticleTitle],
        updateFunc: T => Try[T],
        updateTranslationsFunc: Seq[ArticleTitle] => Try[List[Translation]],
        getTranslationsFunc: String => Try[List[Translation]],
        deleteTranslationFunc: Translation => Try[Unit]) = {
      val resourceResult = updateFunc(res.withName(defaultTitle.title))
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

    private def updateTranslations(id: String,
                                   titles: Seq[ArticleTitle],
                                   updateTranslationFunc: (String, String, String) => Try[Translation]) = {
      val tries = titles.map(t => updateTranslationFunc(id, t.language, t.title))
      Traverse[List].sequence(tries.toList)
    }

    private def updateResourceTitleAndTranslations(res: Resource,
                                                   defaultTitle: ArticleTitle,
                                                   titles: Seq[ArticleTitle]) = {
      val updateTranslationsFunc = updateTranslations(res.id, _: Seq[ArticleTitle], updateResourceTranslation)
      updateTitleAndTranslations(
        res,
        defaultTitle,
        titles,
        updateResource,
        updateTranslationsFunc,
        getResourceTranslations,
        (t: Translation) => deleteResourceTranslation(res.id, t)
      )
    }

    private def updateTopicTitleAndTranslations(top: Topic, defaultTitle: ArticleTitle, titles: Seq[ArticleTitle]) = {
      val updateTranslationsFunc = updateTranslations(top.id, _: Seq[ArticleTitle], updateTopicTranslation)
      updateTitleAndTranslations(
        top,
        defaultTitle,
        titles,
        updateTopic,
        updateTranslationsFunc,
        getTopicTranslations,
        (t: Translation) => deleteTopicTranslation(top.id, t)
      )
    }

    private[integration] def updateResourceTranslation(resourceId: String, lang: String, name: String) =
      putRaw(s"$TaxonomyApiEndpoint/resources/$resourceId/translations/$lang", Translation(name))

    private[integration] def updateTopicTranslation(topicId: String, lang: String, name: String) =
      putRaw(s"$TaxonomyApiEndpoint/topics/$topicId/translations/$lang", Translation(name))

    private[integration] def updateResource(resource: Resource)(implicit formats: Formats) =
      putRaw[Resource](s"$TaxonomyApiEndpoint/resources/${resource.id}", resource)

    private[integration] def updateTopic(topic: Topic)(implicit formats: Formats) =
      putRaw[Topic](s"$TaxonomyApiEndpoint/topics/${topic.id}", topic)

    private[integration] def getTopicTranslations(topicId: String) =
      get[List[Translation]](s"$TaxonomyApiEndpoint/topics/$topicId/translations")

    private def deleteTopicTranslation(topicId: String, translation: Translation) = {
      translation.language
        .map(language => {
          delete(s"$TaxonomyApiEndpoint/topics/$topicId/translations/$language")
        })
        .getOrElse({
          logger.info(s"Cannot delete translation without language for $topicId")
          Success(())
        })
    }

    private[integration] def getResourceTranslations(resourceId: String) =
      get[List[Translation]](s"$TaxonomyApiEndpoint/resources/$resourceId/translations")

    private[integration] def deleteResourceTranslation(resourceId: String, translation: Translation) = {
      translation.language
        .map(language => {
          delete(s"$TaxonomyApiEndpoint/resources/$resourceId/translations/$language")
        })
        .getOrElse({
          logger.info(s"Cannot delete translation without language for $resourceId")
          Success(())
        })
    }

    def updateTaxonomyMetadataIfExists(articleId: Long, visible: Boolean) = {
      for {
        resources <- queryResource(articleId)
        existingResourceMetadataWithId <- resources.traverse(res => getResourceMetadata(res.id).map((res.id, _)))
        _ <- existingResourceMetadataWithId.traverse {
          case (resId, existingMeta) => updateResourceMetadata(resId, existingMeta.copy(visible = visible))
        }

        topics <- queryTopic(articleId)
        existingTopicMetadataWithId <- topics.traverse(top => getTopicMetadata(top.id).map((top.id, _)))
        _ <- existingTopicMetadataWithId.traverse {
          case (topId, existingMeta) => updateTopicMetadata(topId, existingMeta.copy(visible = visible))
        }
      } yield articleId
    }

    private def getResourceMetadata(resourceId: String): Try[TaxonomyMetadata] = {
      get[TaxonomyMetadata](s"$TaxonomyApiEndpoint/resources/$resourceId/metadata")
    }

    private def getTopicMetadata(resourceId: String): Try[TaxonomyMetadata] = {
      get[TaxonomyMetadata](s"$TaxonomyApiEndpoint/topics/$resourceId/metadata")
    }

    private def updateResourceMetadata(resourceId: String, body: TaxonomyMetadata): Try[TaxonomyMetadata] = {
      putRaw[TaxonomyMetadata](s"$TaxonomyApiEndpoint/resources/$resourceId/metadata", body)
    }

    private def updateTopicMetadata(resourceId: String, body: TaxonomyMetadata): Try[TaxonomyMetadata] = {
      putRaw[TaxonomyMetadata](s"$TaxonomyApiEndpoint/topics/$resourceId/metadata", body)
    }

    private def get[A](url: String, params: (String, String)*)(implicit mf: Manifest[A]): Try[A] =
      ndlaClient.fetchWithForwardedAuth[A](Http(url).timeout(taxonomyTimeout, taxonomyTimeout).params(params))

    def queryResource(articleId: Long): Try[List[Resource]] =
      get[List[Resource]](s"$TaxonomyApiEndpoint/queries/resources", "contentURI" -> s"urn:article:$articleId")

    def queryTopic(articleId: Long): Try[List[Topic]] =
      get[List[Topic]](s"$TaxonomyApiEndpoint/queries/topics", "contentURI" -> s"urn:article:$articleId")

    private[integration] def delete(url: String, params: (String, String)*): Try[Unit] =
      ndlaClient.fetchRawWithForwardedAuth(
        Http(url).method("DELETE").timeout(taxonomyTimeout, taxonomyTimeout).params(params)) match {
        case Failure(ex) => Failure(ex)
        case Success(_)  => Success(())
      }

    private[integration] def putRaw[B <: AnyRef](url: String, data: B, params: (String, String)*)(
        implicit formats: org.json4s.Formats): Try[B] = {
      logger.info(s"Doing call to $url")
      ndlaClient.fetchRawWithForwardedAuth(
        Http(url)
          .put(write(data))
          .timeout(taxonomyTimeout, taxonomyTimeout)
          .header("content-type", "application/json")
          .params(params)
      ) match {
        case Success(_)  => Success(data)
        case Failure(ex) => Failure(ex)
      }
    }
  }
}

trait Taxonomy[E <: Taxonomy[E]] {
  val id: String
  def name: String
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
