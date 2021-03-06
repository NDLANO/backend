/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import cats.implicits._
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api.{ArticleApiValidationError, ContentId}
import no.ndla.draftapi.model.domain.RevisionMeta
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import no.ndla.validation.ValidationException
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.jackson.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.json4s.{DefaultFormats, Formats}
import scalaj.http.Http

import java.time.LocalDateTime
import scala.util.{Failure, Try}

trait ArticleApiClient {
  this: NdlaClient with ConverterService with Props =>
  val articleApiClient: ArticleApiClient

  class ArticleApiClient(ArticleBaseUrl: String = s"http://${props.ArticleApiHost}") {
    private val InternalEndpoint = s"$ArticleBaseUrl/intern"
    private val deleteTimeout    = 1000 * 10 // 10 seconds
    private val timeout          = 1000 * 15
    private implicit val format: Formats =
      DefaultFormats.withLong + new EnumNameSerializer(domain.Availability) ++ JavaTimeSerializers.all

    def partialPublishArticle(
        id: Long,
        article: PartialPublishArticle
    ): Try[Long] = {
      patchWithData[ArticleApiId, PartialPublishArticle](
        s"$InternalEndpoint/partial-publish/$id",
        article
      ).map(res => res.id)
    }

    def updateArticle(
        id: Long,
        article: domain.Article,
        externalIds: List[String],
        useImportValidation: Boolean,
        useSoftValidation: Boolean
    ): Try[domain.Article] = {

      val articleApiArticle = converterService.toArticleApiArticle(article)
      postWithData[api.ArticleApiArticle, api.ArticleApiArticle](
        s"$InternalEndpoint/article/$id",
        articleApiArticle,
        "external-id"           -> externalIds.mkString(","),
        "use-import-validation" -> useImportValidation.toString,
        "use-soft-validation"   -> useSoftValidation.toString
      ).map(_ => article)
    }

    def unpublishArticle(article: domain.Article): Try[domain.Article] = {
      val id = article.id.get
      post[ContentId](s"$InternalEndpoint/article/$id/unpublish/").map(_ => article)
    }

    def deleteArticle(id: Long): Try[ContentId] = {
      delete[ContentId](s"$InternalEndpoint/article/$id/")
    }

    def validateArticle(article: api.ArticleApiArticle, importValidate: Boolean): Try[api.ArticleApiArticle] = {
      postWithData[api.ArticleApiArticle, api.ArticleApiArticle](
        s"$InternalEndpoint/validate/article",
        article,
        ("import_validate", importValidate.toString)
      ) match {
        case Failure(ex: HttpRequestException) =>
          val validationError = ex.httpResponse.map(r => parse(r.body).extract[ArticleApiValidationError])
          Failure(
            new ValidationException(
              "Failed to validate article in article-api",
              validationError.map(_.messages).getOrElse(Seq.empty)
            )
          )
        case x => x
      }
    }

    private def post[A](endpointUrl: String, params: (String, String)*)(implicit
        mf: Manifest[A],
        format: org.json4s.Formats
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(endpointUrl).method("POST").params(params.toMap))
    }

    private def delete[A](endpointUrl: String, params: (String, String)*)(implicit
        mf: Manifest[A],
        format: org.json4s.Formats
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        Http(endpointUrl).method("DELETE").params(params.toMap).timeout(deleteTimeout, deleteTimeout)
      )
    }

    private def patchWithData[A, B <: AnyRef](endpointUrl: String, data: B, params: (String, String)*)(implicit
        mf: Manifest[A],
        format: org.json4s.Formats
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        Http(endpointUrl)
          .postData(write(data))
          .timeout(timeout, timeout)
          .method("PATCH")
          .params(params.toMap)
          .header("content-type", "application/json")
      )
    }

    private def postWithData[A, B <: AnyRef](endpointUrl: String, data: B, params: (String, String)*)(implicit
        mf: Manifest[A],
        format: org.json4s.Formats
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        Http(endpointUrl)
          .postData(write(data))
          .method("POST")
          .params(params.toMap)
          .header("content-type", "application/json")
      )
    }
  }

  case class PartialPublishArticle(
      availability: Option[api.Availability.Value],
      grepCodes: Option[Seq[String]],
      license: Option[String],
      metaDescription: Option[Seq[api.ArticleMetaDescription]],
      relatedContent: Option[Seq[api.RelatedContent]],
      tags: Option[Seq[api.ArticleTag]],
      revisionDate: Either[Null, Option[LocalDateTime]] // Left means `null` which deletes `revisionDate`
  ) {
    def withLicense(license: Option[String]): PartialPublishArticle  = copy(license = license)
    def withGrepCodes(grepCodes: Seq[String]): PartialPublishArticle = copy(grepCodes = grepCodes.some)
    def withTags(tags: Seq[domain.ArticleTag], language: String): PartialPublishArticle =
      copy(tags =
        tags
          .find(t => t.language == language)
          .toSeq
          .map(t => api.ArticleTag(t.tags, t.language))
          .some
      )
    def withTags(tags: Seq[domain.ArticleTag]): PartialPublishArticle =
      copy(tags = tags.map(t => api.ArticleTag(t.tags, t.language)).some)
    def withRelatedContent(relatedContent: Seq[domain.RelatedContent]): PartialPublishArticle =
      copy(relatedContent = relatedContent.map(converterService.toApiRelatedContent).some)
    def withMetaDescription(meta: Seq[domain.ArticleMetaDescription], language: String): PartialPublishArticle =
      copy(metaDescription =
        meta
          .find(m => m.language == language)
          .map(m => api.ArticleMetaDescription(m.content, m.language))
          .toSeq
          .some
      )
    def withMetaDescription(meta: Seq[domain.ArticleMetaDescription]): PartialPublishArticle =
      copy(metaDescription = meta.map(m => api.ArticleMetaDescription(m.content, m.language)).some)
    def withAvailability(availability: domain.Availability.Value): PartialPublishArticle =
      copy(availability = converterService.toApiAvailability(availability).some)

    def withEarliestRevisionDate(revisionMeta: Seq[RevisionMeta]): PartialPublishArticle = {
      val earliestRevisionDate = converterService.getNextRevision(revisionMeta).map(_.revisionDate)
      val newRev = earliestRevisionDate match {
        case Some(value) => Right(Some(value))
        case None        => Left(null)
      }
      copy(revisionDate = newRev)
    }
  }

  object PartialPublishArticle {
    def empty(): PartialPublishArticle = PartialPublishArticle(None, None, None, None, None, None, Right(None))
  }
}
case class ArticleApiId(id: Long)
