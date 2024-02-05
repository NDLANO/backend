/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller
import cats.implicits._
import no.ndla.draftapi.Eff
import no.ndla.draftapi.model.api.ErrorHelpers
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import no.ndla.validation._
import sttp.tapir.EndpointInput
import sttp.tapir._
import no.ndla.network.tapir.NoNullJsonPrinter._
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.validation.model.HtmlRulesFile
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint

trait RuleController {
  this: ErrorHelpers =>
  val ruleController: RuleController

  class RuleController extends Service[Eff] {
    import ErrorHelpers._
    override val serviceName: String         = "rules"
    override val prefix: EndpointInput[Unit] = "draft-api" / "v1" / serviceName

    val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getHtmlRules,
      getEmbedTagRules,
      getMathMLRules
    )

    def getHtmlRules: ServerEndpoint[Any, Eff] = endpoint.get
      .in("html")
      .summary("Show all HTML validation rules")
      .description("Shows all the HTML validation rules.")
      .out(jsonBody[HtmlRulesFile])
      .errorOut(errorOutputsFor(401, 403))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ => _ =>
        ValidationRules.htmlRulesJson.asRight
      }

    def getEmbedTagRules: ServerEndpoint[Any, Eff] = endpoint.get
      .in("embed-tag")
      .summary("Show all embed tag validation rules")
      .description("Shows all the embed tag  validation rules.")
      .out(jsonBody[HtmlRulesFile])
      .errorOut(errorOutputsFor(401, 403))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ => _ =>
        ValidationRules.embedTagRulesJson.asRight
      }

    def getMathMLRules: ServerEndpoint[Any, Eff] = endpoint.get
      .in("mathml")
      .summary("Show all MathML validation rules")
      .description("Shows all the MathML validation rules.")
      .out(jsonBody[MathMLRulesFile])
      .errorOut(errorOutputsFor(401, 403))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { _ => _ =>
        ValidationRules.mathMLRulesJson.asRight
      }
  }
}
