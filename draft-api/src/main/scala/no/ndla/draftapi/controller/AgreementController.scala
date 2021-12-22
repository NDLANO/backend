/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.DraftApiProperties.InitialScrollContextKeywords
import no.ndla.draftapi.auth.User
import no.ndla.draftapi.integration.ReindexClient
import no.ndla.draftapi.model.api._
import no.ndla.draftapi.model.domain.{AgreementSearchSettings, Language, Sort}
import no.ndla.draftapi.service.search.{AgreementSearchService, SearchConverterService}
import no.ndla.draftapi.service.{ConverterService, ReadService, WriteService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.scalatra.{Created, NotFound, Ok}

import scala.util.{Failure, Success}

trait AgreementController {
  this: ReadService
    with WriteService
    with AgreementSearchService
    with ConverterService
    with SearchConverterService
    with User
    with ReindexClient =>
  val agreementController: AgreementController

  class AgreementController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "API for accessing agreements."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val converterService = new ConverterService
    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val query =
      Param[Option[String]]("query", "Return only agreements with content matching the specified query.")
    private val agreementIds = Param[Option[Seq[String]]](
      "ids",
      "Return only agreements that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val agreementId = Param[Long]("agreement_id", "Id of the agreement that is to be returned")

    private def scrollSearchOr(orFunction: => Any): Any = {
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)

      paramOrNone(this.scrollId.paramName) match {
        case Some(scroll) =>
          agreementSearchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
              Ok(searchConverterService.asApiAgreementSearchResult(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
          }
        case None => orFunction
      }
    }

    private def search(query: Option[String],
                       sort: Option[Sort.Value],
                       license: Option[String],
                       page: Int,
                       pageSize: Int,
                       idList: List[Long],
                       shouldScroll: Boolean) = {
      val settings = query match {
        case Some(q) =>
          AgreementSearchSettings(
            query = Some(q),
            withIdIn = idList,
            license = license,
            page = page,
            pageSize = if (idList.isEmpty) pageSize else idList.size,
            sort = sort.getOrElse(Sort.ByTitleAsc),
            shouldScroll = shouldScroll
          )
        case None =>
          AgreementSearchSettings(
            query = None,
            withIdIn = idList,
            license = license,
            page = page,
            pageSize = if (idList.isEmpty) pageSize else idList.size,
            sort = sort.getOrElse(Sort.ByTitleAsc),
            shouldScroll = shouldScroll
          )
      }

      agreementSearchService.matchingQuery(settings) match {
        case Success(searchResult) =>
          val responseHeader = searchResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
          Ok(searchConverterService.asApiAgreementSearchResult(searchResult), headers = responseHeader)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/",
      operation(
        apiOperation[AgreementSearchResult]("getAllAgreements")
          .summary("Show all agreements")
          .description("Shows all agreements. You can search too.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(agreementIds),
            asQueryParam(license),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(sort),
            asQueryParam(scrollId)
          )
          .authorizations("oauth2")
          .responseMessages(response500))
    ) {
      val userInfo = user.getUser
      doOrAccessDenied(userInfo.canWrite) {
        scrollSearchOr {
          val query = paramOrNone(this.query.paramName)
          val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
          val license = paramOrNone(this.license.paramName)
          val pageSize = intOrDefault(this.pageSize.paramName, DraftApiProperties.DefaultPageSize)
          val page = intOrDefault(this.pageNo.paramName, 1)
          val idList = paramAsListOfLong(this.agreementIds.paramName)
          val shouldScroll = paramOrNone(this.scrollId.paramName).exists(InitialScrollContextKeywords.contains)

          search(query, sort, license, page, pageSize, idList, shouldScroll)
        }
      }
    }

    get(
      "/:agreement_id",
      operation(
        apiOperation[Agreement]("getAgreementById")
          .summary("Show agreement with a specified Id")
          .description("Shows the agreement for the specified id.")
          .parameters(
            asHeaderParam[Option[String]](correlationId),
            pathParam[Long]("agreement_id").description("Id of the article that is to be returned")
          )
          .authorizations("oauth2")
          .responseMessages(response404, response500))
    ) {
      val userInfo = user.getUser
      doOrAccessDenied(userInfo.canWrite) {
        val agreementId = long(this.agreementId.paramName)

        readService.agreementWithId(agreementId) match {
          case Some(agreement) => agreement
          case None            => NotFound(body = Error(Error.NOT_FOUND, s"No agreement with id $agreementId found"))
        }
      }
    }

    post(
      "/",
      operation(
        apiOperation[Agreement]("newAgreement")
          .summary("Create a new agreement")
          .description("Creates a new agreement")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[NewAgreement]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500))
    ) {
      val userInfo = user.getUser
      doOrAccessDenied(userInfo.canWrite) {
        val newAgreement = extract[NewAgreement](request.body)

        newAgreement.flatMap(writeService.newAgreement(_, userInfo)) match {
          case Success(agreement) =>
            reindexClient.reindexAll()
            Created(body = agreement)
          case Failure(exception) => errorHandler(exception)
        }
      }
    }

    patch(
      "/:agreement_id",
      operation(
        apiOperation[Agreement]("updateAgreement")
          .summary("Update an existing agreement")
          .description("Update an existing agreement")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(agreementId),
            bodyParam[UpdatedAgreement]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500))
    ) {
      val userInfo = user.getUser

      doOrAccessDenied(userInfo.canWrite) {
        val agreementId = long(this.agreementId.paramName)
        val updatedAgreement = extract[UpdatedAgreement](request.body)
        updatedAgreement.flatMap(writeService.updateAgreement(agreementId, _, userInfo)) match {
          case Success(agreement) =>
            reindexClient.reindexAll()
            Ok(body = agreement)
          case Failure(exception) => errorHandler(exception)
        }
      }
    }

  }
}
