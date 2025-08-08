/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.integration

import cats.implicits.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.*
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.api.{Delete, Missing, RelatedContentLinkDTO, UpdateWith}
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.Availability
import no.ndla.common.model.domain.article.{
  ArticleMetaDescriptionDTO,
  ArticleTagDTO,
  PartialPublishArticleDTO,
  PartialPublishArticlesBulkDTO
}
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.draftapi.DraftUtil.getNextRevision
import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api.{ArticleApiValidationErrorDTO, ContentIdDTO}
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.Response
import sttp.client3.quick.*

import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Try}

trait ArticleApiClient {
  this: NdlaClient & ConverterService & Props =>
  val articleApiClient: ArticleApiClient

  class ArticleApiClient(ArticleBaseUrl: String = s"http://${props.ArticleApiHost}") {
    private val InternalEndpoint = s"$ArticleBaseUrl/intern"
    private val deleteTimeout    = 10.seconds
    private val timeout          = 15.seconds

    def partialPublishArticle(
        id: Long,
        article: PartialPublishArticleDTO,
        user: TokenUser
    ): Try[Long] = {
      patchWithData[ArticleApiId, PartialPublishArticleDTO](
        s"$InternalEndpoint/partial-publish/$id",
        article,
        Some(user)
      ).map(res => res.id)
    }

    def updateArticle(
        id: Long,
        draft: Draft,
        externalIds: List[String],
        useSoftValidation: Boolean,
        user: TokenUser
    ): Try[Draft] = {
      val extParam = Option.when(externalIds.nonEmpty)("external-id" -> externalIds.mkString(","))
      val params   = List(
        "use-import-validation" -> false.toString,
        "use-soft-validation"   -> useSoftValidation.toString
      ) ++ extParam.toSeq
      for {
        converted <- converterService.toArticleApiArticle(draft)
        _         <- postWithData[common.article.Article, common.article.Article](
          s"$InternalEndpoint/article/$id",
          converted,
          Some(user),
          params*
        )
      } yield draft
    }

    def unpublishArticle(article: Draft, user: TokenUser): Try[Draft] = {
      val id = article.id.get
      post[ContentIdDTO](s"$InternalEndpoint/article/$id/unpublish/", Some(user)).map(_ => article)
    }

    def deleteArticle(id: Long, user: TokenUser): Try[ContentIdDTO] = {
      delete[ContentIdDTO](s"$InternalEndpoint/article/$id/", Some(user))
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
          val validationError = ex.httpResponse.map(r => CirceUtil.unsafeParseAs[ArticleApiValidationErrorDTO](r.body))
          Failure(
            new ValidationException(
              "Failed to validate article in article-api",
              validationError.map(_.messages).getOrElse(Seq.empty)
            )
          )
        case x => x
      }
    }

    def bulkPartialPublishArticles(
        ids: Map[Long, PartialPublishArticleDTO],
        user: TokenUser
    ): Try[Unit] = {
      val articles = PartialPublishArticlesBulkDTO(idTo = ids)
      patchWithDataRaw[PartialPublishArticlesBulkDTO](
        s"$InternalEndpoint/partial-publish",
        articles,
        Some(user),
        // NOTE: Long timeout since this potentially updates a bunch of articles
        10.minutes
      ).unit
    }

    private def post[A: Decoder](endpointUrl: String, user: Option[TokenUser], params: (String, String)*): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](quickRequest.post(uri"$endpointUrl".withParams(params*)), user)
    }

    private def delete[A: Decoder](endpointUrl: String, user: Option[TokenUser], params: (String, String)*): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        quickRequest.delete(uri"$endpointUrl".withParams(params*)).readTimeout(deleteTimeout),
        user
      )
    }

    private def patchWithDataRaw[B: Encoder](
        endpointUrl: String,
        data: B,
        user: Option[TokenUser],
        timeout: Duration,
        params: (String, String)*
    ): Try[Response[String]] = {
      ndlaClient.fetchRawWithForwardedAuth(
        quickRequest
          .patch(uri"$endpointUrl".withParams(params*))
          .body(CirceUtil.toJsonString(data))
          .header("content-type", "application/json", replaceExisting = true)
          .readTimeout(timeout),
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
          .patch(uri"$endpointUrl".withParams(params*))
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
          .post(uri"$endpointUrl".withParams(params*))
          .body(CirceUtil.toJsonString(data))
          .header("content-type", "application/json", replaceExisting = true),
        user
      )
    }
  }

  implicit class PartialPublishArticleDTOImplicits(self: PartialPublishArticleDTO) {
    def withLicense(license: Option[String]): PartialPublishArticleDTO  = self.copy(license = license)
    def withGrepCodes(grepCodes: Seq[String]): PartialPublishArticleDTO = self.copy(grepCodes = grepCodes.some)
    def withTags(tags: Seq[common.Tag], language: String): PartialPublishArticleDTO =
      self.copy(tags =
        tags
          .find(t => t.language == language)
          .toSeq
          .map(t => ArticleTagDTO(t.tags, t.language))
          .some
      )
    def withTags(tags: Seq[common.Tag]): PartialPublishArticleDTO =
      self.copy(tags = tags.map(t => ArticleTagDTO(t.tags, t.language)).some)
    def withRelatedContent(relatedContent: Seq[common.RelatedContent]): PartialPublishArticleDTO = {
      val api = relatedContent.map { rc => rc.leftMap { rcl => RelatedContentLinkDTO(rcl.title, rcl.url) } }
      self.copy(relatedContent = api.some)
    }

    def withMetaDescription(meta: Seq[common.Description], language: String): PartialPublishArticleDTO =
      self.copy(metaDescription =
        meta
          .find(m => m.language == language)
          .map(m => ArticleMetaDescriptionDTO(m.content, m.language))
          .toSeq
          .some
      )
    def withMetaDescription(meta: Seq[common.Description]): PartialPublishArticleDTO = {
      val api = meta.map(m => ArticleMetaDescriptionDTO(m.content, m.language))
      self.copy(metaDescription = api.some)
    }
    def withAvailability(availability: Availability): PartialPublishArticleDTO =
      self.copy(availability = availability.some)
    def withEarliestRevisionDate(revisionMeta: Seq[common.RevisionMeta]): PartialPublishArticleDTO = {
      val earliestRevisionDate = getNextRevision(revisionMeta).map(_.revisionDate)
      val newRev               = earliestRevisionDate match {
        case Some(value) => UpdateWith(value)
        case None        => Delete
      }
      self.copy(revisionDate = newRev)
    }
    def withPublished(published: NDLADate): PartialPublishArticleDTO = self.copy(published = published.some)
  }

  object PartialPublishArticle {
    def empty(): PartialPublishArticleDTO = PartialPublishArticleDTO(None, None, None, None, None, None, Missing, None)
  }
}

case class ArticleApiId(id: Long)
object ArticleApiId {
  implicit val encoder: Encoder[ArticleApiId] = deriveEncoder
  implicit val decoder: Decoder[ArticleApiId] = deriveDecoder
}
