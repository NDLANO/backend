/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.controller

import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.conceptapi.auth.User
import no.ndla.conceptapi.model.api.{
  Concept,
  NewConcept,
  NotFoundException,
  SubjectTags,
  TagsSearchResult,
  UpdatedConcept
}
import no.ndla.conceptapi.model.domain.Language
import no.ndla.conceptapi.service.{ReadService, WriteService}
import no.ndla.conceptapi.service.search.DraftConceptSearchService
import org.scalatra.{Created, Ok}

import scala.util.{Failure, Success}

/*
This is just to share endpoints between controllers while frontend migration is ongoing.
TODO: Move the endpoints to [[DraftConceptController]] and delete this file when frontend starts using [[DraftConceptController]] instead of [[PublishedConceptController]] for creating and updating
 */
trait DraftNdlaController {
  this: ReadService with WriteService with User with DraftConceptSearchService =>
  abstract class DraftNdlaControllerClass() extends NdlaController {
    get(
      "/tag-search/",
      operation(
        apiOperation[TagsSearchResult]("getTags-paginated")
          .summary("Retrieves a list of all previously used tags in concepts")
          .description("Retrieves a list of all previously used tags in concepts")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(pageSize),
            asQueryParam(pageNo),
            asQueryParam(language)
          )
          .responseMessages(response500)
          .authorizations("oauth2"))
    ) {

      val query = paramOrDefault(this.query.paramName, "")
      val pageSize = intOrDefault(this.pageSize.paramName, ConceptApiProperties.DefaultPageSize) match {
        case tooSmall if tooSmall < 1 => ConceptApiProperties.DefaultPageSize
        case x                        => x
      }
      val pageNo = intOrDefault(this.pageNo.paramName, 1) match {
        case tooSmall if tooSmall < 1 => 1
        case x                        => x
      }
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)

      readService.getAllTags(query, pageSize, pageNo, language)
    }

    post(
      "/",
      operation(
        apiOperation[Concept]("newConceptById")
          .summary("Create new concept")
          .description("Create new concept")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[NewConcept]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500))
    ) {
      val userInfo = user.getUser
      doOrAccessDenied(userInfo.canWrite) {
        val body = extract[NewConcept](request.body)
        body.flatMap(concept => writeService.newConcept(concept, userInfo)) match {
          case Success(c)  => Created(c)
          case Failure(ex) => errorHandler(ex)
        }
      }
    }

    patch(
      "/:concept_id",
      operation(
        apiOperation[Concept]("updateConceptById")
          .summary("Update a concept")
          .description("Update a concept")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[UpdatedConcept],
            asPathParam(conceptId)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response404, response500))
    ) {
      val userInfo = user.getUser
      doOrAccessDenied(userInfo.canWrite) {
        val body = extract[UpdatedConcept](request.body)
        val conceptId = long(this.conceptId.paramName)
        body.flatMap(writeService.updateConcept(conceptId, _, userInfo)) match {
          case Success(c)  => Ok(c)
          case Failure(ex) => errorHandler(ex)
        }
      }
    }
  }
}
