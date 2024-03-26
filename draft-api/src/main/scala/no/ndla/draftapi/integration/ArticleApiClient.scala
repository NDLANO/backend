/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import cats.implicits.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.*
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.api.{Delete, Missing, UpdateOrDelete, UpdateWith}
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.Availability
import no.ndla.common.model.{NDLADate, RelatedContentLink, domain as common}
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api.{ArticleApiValidationError, ContentId}
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.quick.*

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Try}

trait ArticleApiClient {
  this: NdlaClient with ConverterService with Props =>
  val articleApiClient: ArticleApiClient

  class ArticleApiClient(ArticleBaseUrl: String = s"http://${props.ArticleApiHost}") {
    private val InternalEndpoint = s"$ArticleBaseUrl/intern"
    private val deleteTimeout    = 10.seconds
    private val timeout          = 15.seconds

    def partialPublishArticle(
        id: Long,
        article: PartialPublishArticle,
        user: TokenUser
    ): Try[Long] = {
      patchWithData[ArticleApiId, PartialPublishArticle](
        s"$InternalEndpoint/partial-publish/$id",
        article,
        Some(user)
      ).map(res => res.id)
    }

    def updateArticle(
        id: Long,
        draft: Draft,
        externalIds: List[String],
        useImportValidation: Boolean,
        useSoftValidation: Boolean,
        user: TokenUser
    ): Try[Draft] = {
      val extParam = Option.when(externalIds.nonEmpty)("external-id" -> externalIds.mkString(","))
      val params = List(
        "use-import-validation" -> useImportValidation.toString,
        "use-soft-validation"   -> useSoftValidation.toString
      ) ++ extParam.toSeq
      for {
        converted <- converterService.toArticleApiArticle(draft)
        _ <- postWithData[common.article.Article, common.article.Article](
          s"$InternalEndpoint/article/$id",
          converted,
          Some(user),
          params: _*
        )
      } yield draft
    }

    def unpublishArticle(article: Draft, user: TokenUser): Try[Draft] = {
      val id = article.id.get
      post[ContentId](s"$InternalEndpoint/article/$id/unpublish/", Some(user)).map(_ => article)
    }

    def deleteArticle(id: Long, user: TokenUser): Try[ContentId] = {
      delete[ContentId](s"$InternalEndpoint/article/$id/", Some(user))
    }

    def validateArticle(
        article: common.article.Article,
        importValidate: Boolean,
        user: Option[TokenUser]
    ): Try[common.article.Article] = {
      postWithData[common.article.Article, common.article.Article](
        s"$InternalEndpoint/validate/article",
        article,
        user,
        ("import_validate", importValidate.toString)
      ) match {
        case Failure(ex: HttpRequestException) =>
          val validationError = ex.httpResponse.map(r => CirceUtil.unsafeParseAs[ArticleApiValidationError](r.body))
          Failure(
            new ValidationException(
              "Failed to validate article in article-api",
              validationError.map(_.messages).getOrElse(Seq.empty)
            )
          )
        case x => x
      }
    }

    private def post[A: Decoder](endpointUrl: String, user: Option[TokenUser], params: (String, String)*): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](quickRequest.post(uri"$endpointUrl".withParams(params: _*)), user)
    }

    private def delete[A: Decoder](endpointUrl: String, user: Option[TokenUser], params: (String, String)*): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest.delete(uri"$endpointUrl".withParams(params: _*)).readTimeout(deleteTimeout),
        user
      )
    }

    private def patchWithData[A: Decoder, B <: AnyRef: Encoder](
        endpointUrl: String,
        data: B,
        user: Option[TokenUser],
        params: (String, String)*
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest
          .patch(uri"$endpointUrl".withParams(params: _*))
          .body(CirceUtil.toJsonString(data))
          .header("content-type", "application/json", replaceExisting = true)
          .readTimeout(timeout),
        user
      )
    }

    private def postWithData[A: Decoder, B <: AnyRef: Encoder](
        endpointUrl: String,
        data: B,
        user: Option[TokenUser],
        params: (String, String)*
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest
          .post(uri"$endpointUrl".withParams(params: _*))
          .body(CirceUtil.toJsonString(data))
          .header("content-type", "application/json", replaceExisting = true),
        user
      )
    }
  }

  case class PartialPublishArticle(
      availability: Option[Availability],
      grepCodes: Option[Seq[String]],
      license: Option[String],
      metaDescription: Option[Seq[api.ArticleMetaDescription]],
      relatedContent: Option[Seq[common.RelatedContent]],
      tags: Option[Seq[api.ArticleTag]],
      revisionDate: UpdateOrDelete[NDLADate]
  ) {
    def withLicense(license: Option[String]): PartialPublishArticle  = copy(license = license)
    def withGrepCodes(grepCodes: Seq[String]): PartialPublishArticle = copy(grepCodes = grepCodes.some)
    def withTags(tags: Seq[common.Tag], language: String): PartialPublishArticle =
      copy(tags =
        tags
          .find(t => t.language == language)
          .toSeq
          .map(t => api.ArticleTag(t.tags, t.language))
          .some
      )
    def withTags(tags: Seq[common.Tag]): PartialPublishArticle =
      copy(tags = tags.map(t => api.ArticleTag(t.tags, t.language)).some)
    def withRelatedContent(relatedContent: Seq[common.RelatedContent]): PartialPublishArticle =
      copy(relatedContent = relatedContent.some)
    def withMetaDescription(meta: Seq[common.Description], language: String): PartialPublishArticle =
      copy(metaDescription =
        meta
          .find(m => m.language == language)
          .map(m => api.ArticleMetaDescription(m.content, m.language))
          .toSeq
          .some
      )
    def withMetaDescription(meta: Seq[common.Description]): PartialPublishArticle =
      copy(metaDescription = meta.map(m => api.ArticleMetaDescription(m.content, m.language)).some)
    def withAvailability(availability: Availability): PartialPublishArticle =
      copy(availability = availability.some)

    def withEarliestRevisionDate(revisionMeta: Seq[common.draft.RevisionMeta]): PartialPublishArticle = {
      val earliestRevisionDate = converterService.getNextRevision(revisionMeta).map(_.revisionDate)
      val newRev = earliestRevisionDate match {
        case Some(value) => UpdateWith(value)
        case None        => Delete
      }
      copy(revisionDate = newRev)
    }
  }

  object PartialPublishArticle {
    def empty(): PartialPublishArticle = PartialPublishArticle(None, None, None, None, None, None, Missing)

    implicit def eitherEnc: Encoder[Either[RelatedContentLink, Long]] = eitherEncoder[RelatedContentLink, Long]
    implicit def eitherDec: Decoder[Either[RelatedContentLink, Long]] = eitherDecoder[RelatedContentLink, Long]

    implicit val encoder: Encoder[PartialPublishArticle] = UpdateOrDelete.filterMarkers(deriveEncoder)
    implicit val decoder: Decoder[PartialPublishArticle] = deriveDecoder[PartialPublishArticle]
  }
}
case class ArticleApiId(id: Long)

object ArticleApiId {
  implicit val encoder: Encoder[ArticleApiId] = deriveEncoder
  implicit val decoder: Decoder[ArticleApiId] = deriveDecoder
}
