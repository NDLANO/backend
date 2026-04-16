/*
 * Part of NDLA myndla-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.myndla.MyNDLAUser
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.{ErrorHelpers, TapirController}
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.codec.enumeratum.*
import no.ndla.myndlaapi.integration.InternalMyNDLAApiClient
import no.ndla.myndlaapi.model.api.InactiveUserResultDTO
import no.ndla.myndlaapi.service.UserService

class InternController(using
    internalMyNDLAApiClient: InternalMyNDLAApiClient,
    errorHandling: ControllerErrorHandling,
    errorHelpers: ErrorHelpers,
    userService: UserService,
) extends TapirController
    with StrictLogging {
  override val prefix: EndpointInput[Unit] = "intern"
  override val enableSwagger               = false

  private def getDomainUser: ServerEndpoint[Any, Eff] = endpoint
    .summary("Get domain user from feide user. Useful for other api's that requires login")
    .get
    .in("get-user")
    .out(jsonBody[MyNDLAUser])
    .errorOut(errorOutputsFor(400))
    .withFeideUser
    .serverLogicPure(feide => _ => feide.userOrAccessDenied)

  private def cleanupInactiveUsers: ServerEndpoint[Any, Eff] = endpoint
    .summary("Notifies, and removes inactive users")
    .post
    .in("cleanup-inactive-users")
    .out(jsonBody[InactiveUserResultDTO])
    .errorOut(errorOutputsFor(400))
    .serverLogicPure(_ => userService.cleanupInactiveUsers())

  private def sendTestEmail: ServerEndpoint[Any, Eff] = endpoint
    .summary("Sends inactivty test email")
    .post
    .in("send-test-email")
    .in(query[String]("email").description("Email to send test email to"))
    .out(jsonBody[Boolean])
    .errorOut(errorOutputsFor(400))
    .serverLogicPure(userService.sendInactivityEmailIgnoreEnvironment)

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(getDomainUser, cleanupInactiveUsers, sendTestEmail)

}
