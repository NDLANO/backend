/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller
import no.ndla.draftapi.model.api.Error
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import no.ndla.validation._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.{ResponseMessage, Swagger}

trait RuleController {
  this: NdlaController =>
  val ruleController: RuleController

  class RuleController(implicit val swagger: Swagger) extends NdlaController {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription                 = "API for accessing validation rules."

    registerModel[Error]()

    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    get(
      "/html/",
      operation(
        apiOperation[Map[String, Any]]("getHtmlRules")
          .summary("Show all HTML validation rules")
          .description("Shows all the HTML validation rules.")
          .parameters(asHeaderParam(correlationId))
          .authorizations("oauth2")
          .responseMessages(response403, response500)
      )
    ) {
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        ValidationRules.htmlRulesJson
      }
    }: Unit

    get(
      "/embed-tag/",
      operation(
        apiOperation[Map[String, Any]]("getEmbedTagRules")
          .summary("Show all embed tag validation rules")
          .description("Shows all the embed tag  validation rules.")
          .parameters(asHeaderParam(correlationId))
          .authorizations("oauth2")
          .responseMessages(response403, response500)
      )
    ) {
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        ValidationRules.embedTagRulesJson
      }
    }: Unit

    get(
      "/mathml/",
      operation(
        apiOperation[Map[String, Any]]("getMathMLRules")
          .summary("Show all MathML validation rules")
          .description("Shows all the MathML validation rules.")
          .parameters(asHeaderParam(correlationId))
          .authorizations("oauth2")
          .responseMessages(response403, response500)
      )
    ) {
      requirePermissionOrAccessDenied(DRAFT_API_WRITE) {
        ValidationRules.mathMLRulesJson
      }
    }: Unit
  }

}
