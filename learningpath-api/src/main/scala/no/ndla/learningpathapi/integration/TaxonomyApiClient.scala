/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.StrictLogging
import no.ndla.learningpathapi.model.domain.{LearningPath, TaxonomyUpdateException}
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import org.json4s.native.Serialization.write
import sttp.client3.quick._
import cats.implicits._
import no.ndla.common.model.domain.Title
import no.ndla.language.Language
import no.ndla.learningpathapi.Props
import no.ndla.network.TaxonomyData.{TAXONOMY_VERSION_HEADER, defaultVersion}
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.Response

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient with Props =>
  val taxononyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends StrictLogging {
    import props.{TaxonomyUrl, DefaultLanguage}
    implicit val formats                   = org.json4s.DefaultFormats
    private val taxonomyTimeout            = 20.seconds
    private val TaxonomyApiEndpoint        = s"$TaxonomyUrl/v1"
    private val LearningPathResourceTypeId = "urn:resourcetype:learningPath"

    def updateTaxonomyForLearningPath(
        learningPath: LearningPath,
        createResourceIfMissing: Boolean,
        user: Option[TokenUser]
    ): Try[LearningPath] = {
      val result = learningPath.id match {
        case None =>
          Failure(TaxonomyUpdateException("Can't update taxonomy resource when learningpath is missing id."))
        case Some(learningPathId) =>
          val contentUri = s"urn:learningpath:$learningPathId"

          Language.findByLanguageOrBestEffort(learningPath.title, DefaultLanguage) match {
            case None =>
              Failure(TaxonomyUpdateException("Can't update taxonomy resource when learningpath is missing titles."))
            case Some(mainTitle) =>
              queryResource(contentUri, user) match {
                case Failure(ex) => Failure(ex)
                case Success(resources) if resources.isEmpty && createResourceIfMissing =>
                  createAndUpdateResource(learningPath, contentUri, mainTitle, user)
                case Success(resources) =>
                  updateExistingResources(resources, contentUri, learningPath.title, mainTitle, user)
              }
          }
      }
      result.map(_ => learningPath)
    }

    private def createAndUpdateResource(
        learningPath: LearningPath,
        contentUri: String,
        mainTitle: Title,
        user: Option[TokenUser]
    ) = {
      val newResource = NewOrUpdateTaxonomyResource(
        name = mainTitle.title,
        contentUri = contentUri
      )
      createResource(newResource, user) match {
        case Failure(ex) => Failure(ex)
        case Success(newLocation) =>
          newLocation.split('/').lastOption match {
            case None =>
              val msg = "Wasn't able to derive id from taxonomy create response, this is probably a bug."
              logger.error(msg)
              Failure(TaxonomyUpdateException(msg))
            case Some(resourceId) =>
              val newResource = TaxonomyResource(
                id = resourceId,
                name = mainTitle.title,
                contentUri = Some(contentUri),
                path = None
              )
              addLearningPathResourceType(resourceId, user).flatMap(_ =>
                updateExistingResources(List(newResource), contentUri, learningPath.title, mainTitle, user)
              )
          }
      }
    }

    private def addLearningPathResourceType(resourceId: String, user: Option[TokenUser]): Try[String] = {
      val resourceType = ResourceResourceType(
        resourceId = resourceId,
        resourceTypeId = LearningPathResourceTypeId
      )
      postRaw[ResourceResourceType](s"$TaxonomyApiEndpoint/resource-resourcetypes", resourceType, user) match {
        case Failure(ex: HttpRequestException) if ex.httpResponse.exists(_.isSuccess) => Success(resourceId)
        case Failure(ex)                                                              => Failure(ex)
        case Success(_)                                                               => Success(resourceId)
      }
    }

    private def updateTaxonomyResource(
        taxonomyId: String,
        resource: NewOrUpdateTaxonomyResource,
        user: Option[TokenUser]
    ) = {
      putRaw[NewOrUpdateTaxonomyResource](s"$TaxonomyApiEndpoint/resources/${taxonomyId}", resource, user) match {
        case Failure(ex) =>
          logger.error(s"Failed updating taxonomy resource $taxonomyId with name.")
          Failure(ex)
        case Success(res) =>
          logger.info(s"Successfully updated $taxonomyId with name: '${resource.name}'...")
          Success(res)
      }
    }

    private def createResource(resource: NewOrUpdateTaxonomyResource, user: Option[TokenUser]) = {
      postRaw[NewOrUpdateTaxonomyResource](s"$TaxonomyApiEndpoint/resources", resource, user) match {
        case Success(resp) =>
          resp.header("location") match {
            case Some(locationHeader) if locationHeader.nonEmpty => Success(locationHeader)
            case _ => Failure(new TaxonomyUpdateException("Could not get location after inserting resource"))
          }

        case Failure(ex: HttpRequestException) if ex.httpResponse.exists(_.isSuccess) =>
          ex.httpResponse.flatMap(_.header("location")) match {
            case Some(locationHeader) if locationHeader.nonEmpty => Success(locationHeader)
            case _                                               => Failure(ex)
          }
        case Failure(ex) => Failure(ex)
      }
    }

    private def updateExistingResources(
        existingResources: List[TaxonomyResource],
        contentUri: String,
        titles: Seq[Title],
        mainTitle: Title,
        user: Option[TokenUser]
    ) = {
      existingResources
        .traverse(r => {
          val resourceToPut = NewOrUpdateTaxonomyResource(
            name = mainTitle.title,
            contentUri = r.contentUri.getOrElse(contentUri)
          )

          updateTaxonomyResource(r.id, resourceToPut, user)
            .flatMap(_ => updateResourceTranslations(r.id, titles, user))
        })
    }

    private def titleIsEqualToTranslation(title: Title, translation: Translation) =
      translation.name == title.title &&
        translation.language.exists(_ == title.language)

    private def updateResourceTranslations(
        resourceId: String,
        titles: Seq[Title],
        user: Option[TokenUser]
    ): Try[List[Translation]] = {
      // Since 'unknown' language is known as 'unk' in taxonomy we do a conversion
      val titlesWithConvertedLang = titles.map(t => t.copy(language = t.language.replace("unknown", "unk")))
      getResourceTranslations(resourceId, user) match {
        case Failure(ex) =>
          logger.error(s"Failed to get translations for $resourceId when updating taxonomy...")
          Failure(ex)
        case Success(existingTranslations) =>
          val toDelete =
            existingTranslations.filterNot(_.language.exists(titlesWithConvertedLang.map(_.language).contains))
          val deleted = toDelete.map(deleteResourceTranslation(resourceId, _, user))
          val updated = titlesWithConvertedLang.toList.traverse(title =>
            existingTranslations.find(titleIsEqualToTranslation(title, _)) match {
              case Some(existingTranslation) => Success(existingTranslation)
              case None                      => updateResourceTranslation(resourceId, title.language, title.title, user)
            }
          )

          deleted.collectFirst { case Failure(ex) => Failure(ex) } match {
            case Some(failedDelete) => failedDelete
            case None               => updated
          }
      }
    }

    private[integration] def updateResourceTranslation(
        resourceId: String,
        lang: String,
        name: String,
        user: Option[TokenUser]
    ) =
      putRaw(s"$TaxonomyApiEndpoint/resources/$resourceId/translations/$lang", Translation(name), user)

    private[integration] def deleteResourceTranslation(
        resourceId: String,
        translation: Translation,
        user: Option[TokenUser]
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

    private[integration] def getResourceTranslations(resourceId: String, user: Option[TokenUser]) =
      get[List[Translation]](s"$TaxonomyApiEndpoint/resources/$resourceId/translations", user)

    private def queryResource(contentUri: String, user: Option[TokenUser]): Try[List[TaxonomyResource]] = {
      get[List[TaxonomyResource]](s"$TaxonomyApiEndpoint/queries/resources", user, "contentURI" -> contentUri) match {
        case Success(resources) => Success(resources)
        case Failure(ex)        => Failure(ex)
      }
    }

    def getResource(nodeId: String, user: Option[TokenUser]): Try[TaxonomyResource] = {
      val resourceId = s"urn:resource:1:$nodeId"
      get[TaxonomyResource](s"$TaxonomyApiEndpoint/resources/$resourceId", user) match {
        case Failure(ex) =>
          Failure(ex)
        case Success(a) =>
          Success(a)
      }
    }

    def updateResource(resource: TaxonomyResource, user: Option[TokenUser]): Try[TaxonomyResource] = {
      put[String, TaxonomyResource](s"$TaxonomyApiEndpoint/resources/${resource.id}", resource, user) match {
        case Success(_) => Success(resource)
        case Failure(ex: HttpRequestException) if ex.httpResponse.exists(_.isSuccess) =>
          Success(resource)
        case Failure(ex) => Failure(ex)
      }
    }

    def queryResource(articleId: Long): Try[List[Resource]] =
      get[List[Resource]](s"$TaxonomyApiEndpoint/queries/resources", None, "contentURI" -> s"urn:article:$articleId")

    def queryTopic(articleId: Long): Try[List[Topic]] =
      get[List[Topic]](s"$TaxonomyApiEndpoint/queries/topics", None, "contentURI" -> s"urn:article:$articleId")

    private def get[A](url: String, user: Option[TokenUser], params: (String, String)*)(implicit
        mf: Manifest[A]
    ): Try[A] = {
      val request = quickRequest
        .get(uri"$url".withParams(params: _*))
        .readTimeout(taxonomyTimeout)
        .header(TAXONOMY_VERSION_HEADER, defaultVersion)
      ndlaClient.fetchWithForwardedAuth[A](request, user)
    }

    private def put[A, B <: AnyRef](url: String, data: B, user: Option[TokenUser], params: (String, String)*)(implicit
        mf: Manifest[A],
        format: org.json4s.Formats
    ): Try[A] = {
      val request = quickRequest
        .put(uri"$url".withParams(params: _*))
        .readTimeout(taxonomyTimeout)
        .body(write(data)(format))
        .header("content-type", "application/json", replaceExisting = true)
      ndlaClient.fetchWithForwardedAuth[A](request, user)
    }

    private[integration] def putRaw[B <: AnyRef](
        url: String,
        data: B,
        user: Option[TokenUser],
        params: (String, String)*
    )(implicit
        formats: org.json4s.Formats
    ): Try[B] = {
      logger.info(s"Doing call to $url")
      val request = quickRequest
        .put(uri"$url".withParams(params: _*))
        .body(write(data))
        .readTimeout(taxonomyTimeout)
        .header("content-type", "application/json", replaceExisting = true)
      ndlaClient.fetchRawWithForwardedAuth(request, user) match {
        case Success(_)  => Success(data)
        case Failure(ex) => Failure(ex)
      }
    }

    private def postRaw[B <: AnyRef](
        endpointUrl: String,
        data: B,
        user: Option[TokenUser],
        params: (String, String)*
    ): Try[Response[String]] = {
      ndlaClient.fetchRawWithForwardedAuth(
        quickRequest
          .post(uri"$endpointUrl".withParams(params.toMap))
          .body(write(data))
          .readTimeout(taxonomyTimeout)
          .header("content-type", "application/json", replaceExisting = true),
        user
      ) match {
        case Success(resp) => Success(resp)
        case Failure(ex)   => Failure(ex)
      }
    }

    private[integration] def delete(url: String, user: Option[TokenUser], params: (String, String)*): Try[Unit] =
      ndlaClient.fetchRawWithForwardedAuth(
        quickRequest
          .delete(uri"$url".withParams(params: _*))
          .readTimeout(taxonomyTimeout),
        user
      ) match {
        case Failure(ex) => Failure(ex)
        case Success(_)  => Success(())
      }

  }
}

case class Translation(name: String, language: Option[String] = None)

case class NewOrUpdateTaxonomyResource(
    name: String,
    contentUri: String
)

case class TaxonomyResource(
    id: String,
    name: String,
    contentUri: Option[String],
    path: Option[String]
)

case class ResourceResourceType(
    resourceId: String,
    resourceTypeId: String
)

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
