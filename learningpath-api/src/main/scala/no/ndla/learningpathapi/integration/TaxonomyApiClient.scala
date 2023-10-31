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
import org.json4s.DefaultFormats
import sttp.client3.Response

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient with Props =>
  val taxonomyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends StrictLogging {
    import props.{TaxonomyUrl, DefaultLanguage}
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
    private val taxonomyTimeout               = 20.seconds
    private val TaxonomyApiEndpoint           = s"$TaxonomyUrl/v1"
    private val LearningPathResourceTypeId    = "urn:resourcetype:learningPath"

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
              queryNodes(contentUri, user) match {
                case Failure(ex) => Failure(ex)
                case Success(resources) if resources.isEmpty && createResourceIfMissing =>
                  createAndUpdateResource(learningPath, contentUri, mainTitle, user)
                case Success(resources) =>
                  updateExistingNode(resources, contentUri, learningPath.title, mainTitle, user)
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
      val newResource = NewOrUpdatedNode(
        nodeType = "RESOURCE",
        name = mainTitle.title,
        contentUri = contentUri
      )
      createNode(newResource, user) match {
        case Failure(ex) => Failure(ex)
        case Success(newLocation) =>
          newLocation.split('/').lastOption match {
            case None =>
              val msg = "Wasn't able to derive id from taxonomy create response, this is probably a bug."
              logger.error(msg)
              Failure(TaxonomyUpdateException(msg))
            case Some(resourceId) =>
              val newResource = Node(
                id = resourceId,
                name = mainTitle.title,
                contentUri = Some(contentUri),
                paths = List(newLocation)
              )
              addLearningPathResourceType(resourceId, user).flatMap(_ =>
                updateExistingNode(List(newResource), contentUri, learningPath.title, mainTitle, user)
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

    private def updateNode(
        taxonomyId: String,
        node: NewOrUpdatedNode,
        user: Option[TokenUser]
    ) = {
      putRaw[NewOrUpdatedNode](s"$TaxonomyApiEndpoint/nodes/$taxonomyId", node, user) match {
        case Failure(ex) =>
          logger.error(s"Failed updating taxonomy resource $taxonomyId with name.")
          Failure(ex)
        case Success(res) =>
          logger.info(s"Successfully updated $taxonomyId with name: '${node.name}'...")
          Success(res)
      }
    }

    private def createNode(resource: NewOrUpdatedNode, user: Option[TokenUser]) = {
      postRaw[NewOrUpdatedNode](s"$TaxonomyApiEndpoint/nodes", resource, user) match {
        case Success(resp) =>
          resp.header("location") match {
            case Some(locationHeader) if locationHeader.nonEmpty => Success(locationHeader)
            case _ => Failure(TaxonomyUpdateException("Could not get location after inserting resource"))
          }

        case Failure(ex: HttpRequestException) if ex.httpResponse.exists(_.isSuccess) =>
          ex.httpResponse.flatMap(_.header("location")) match {
            case Some(locationHeader) if locationHeader.nonEmpty => Success(locationHeader)
            case _                                               => Failure(ex)
          }
        case Failure(ex) => Failure(ex)
      }
    }

    private def updateExistingNode(
        existingResources: List[Node],
        contentUri: String,
        titles: Seq[Title],
        mainTitle: Title,
        user: Option[TokenUser]
    ) = {
      existingResources
        .traverse(r => {
          val resourceToPut = NewOrUpdatedNode(
            nodeType = "RESOURCE",
            name = mainTitle.title,
            contentUri = r.contentUri.getOrElse(contentUri)
          )

          updateNode(r.id, resourceToPut, user)
            .flatMap(_ => updateResourceTranslations(r.id, titles, user))
        })
    }

    private def titleIsEqualToTranslation(title: Title, translation: Translation) =
      translation.name == title.title &&
        translation.language.contains(title.language)

    private def updateResourceTranslations(
        resourceId: String,
        titles: Seq[Title],
        user: Option[TokenUser]
    ): Try[List[Translation]] = {
      // Since 'unknown' language is known as 'unk' in taxonomy we do a conversion
      val titlesWithConvertedLang = titles.map(t => t.copy(language = t.language.replace("unknown", "unk")))
      getTranslations(resourceId, user) match {
        case Failure(ex) =>
          logger.error(s"Failed to get translations for $resourceId when updating taxonomy...")
          Failure(ex)
        case Success(existingTranslations) =>
          val toDelete =
            existingTranslations.filterNot(_.language.exists(titlesWithConvertedLang.map(_.language).contains))
          val deleted = toDelete.map(deleteTranslation(resourceId, _, user))
          val updated = titlesWithConvertedLang.toList.traverse(title =>
            existingTranslations.find(titleIsEqualToTranslation(title, _)) match {
              case Some(existingTranslation) => Success(existingTranslation)
              case None                      => updateTranslation(resourceId, title.language, title.title, user)
            }
          )

          deleted.collectFirst { case Failure(ex) => Failure(ex) } match {
            case Some(failedDelete) => failedDelete
            case None               => updated
          }
      }
    }

    private[integration] def updateTranslation(
        nodeId: String,
        lang: String,
        name: String,
        user: Option[TokenUser]
    ) =
      putRaw(s"$TaxonomyApiEndpoint/nodes/$nodeId/translations/$lang", Translation(name), user)

    private[integration] def deleteTranslation(
        nodeId: String,
        translation: Translation,
        user: Option[TokenUser]
    ) = {
      translation.language
        .map(language => {
          delete(s"$TaxonomyApiEndpoint/nodes/$nodeId/translations/$language", user)
        })
        .getOrElse({
          logger.info(s"Cannot delete translation without language for $nodeId")
          Success(())
        })
    }

    private[integration] def getTranslations(nodeId: String, user: Option[TokenUser]) =
      get[List[Translation]](s"$TaxonomyApiEndpoint/nodes/$nodeId/translations", user)

    private def queryNodes(contentUri: String, user: Option[TokenUser]): Try[List[Node]] = {
      get[List[Node]](s"$TaxonomyApiEndpoint/nodes", user, "contentURI" -> contentUri) match {
        case Success(resources) => Success(resources)
        case Failure(ex)        => Failure(ex)
      }
    }

    def getNode(nodeId: String, user: Option[TokenUser]): Try[Node] = {
      val resourceId = s"urn:resource:1:$nodeId"
      get[Node](s"$TaxonomyApiEndpoint/nodes/$resourceId", user) match {
        case Failure(ex) =>
          Failure(ex)
        case Success(a) =>
          Success(a)
      }
    }

    def updateNode(node: Node, user: Option[TokenUser]): Try[Node] = {
      put[String, Node](s"$TaxonomyApiEndpoint/nodes/${node.id}", node, user) match {
        case Success(_) => Success(node)
        case Failure(ex: HttpRequestException) if ex.httpResponse.exists(_.isSuccess) =>
          Success(node)
        case Failure(ex) => Failure(ex)
      }
    }

    def queryNodes(articleId: Long): Try[List[Node]] =
      get[List[Node]](s"$TaxonomyApiEndpoint/nodes", None, "contentURI" -> s"urn:article:$articleId")

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

case class NewOrUpdatedNode(
    nodeType: String,
    name: String,
    contentUri: String
)

case class ResourceResourceType(
    resourceId: String,
    resourceTypeId: String
)

case class Node(id: String, name: String, contentUri: Option[String], paths: List[String]) {
  def withName(name: String): Node = this.copy(name = name)
}
