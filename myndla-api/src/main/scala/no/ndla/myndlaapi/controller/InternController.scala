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
import no.ndla.network.tapir.TapirController
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.codec.enumeratum.*
import no.ndla.myndlaapi.model.api.InactiveUserResultDTO
import no.ndla.myndlaapi.service.UserService
import no.ndla.network.model.{FeideIdToken, FeideUserWrapper}

class InternController(using errorHandling: ControllerErrorHandling, userService: UserService)
    extends TapirController
    with StrictLogging {
  override val prefix: EndpointInput[Unit] = "intern"
  override val enableSwagger               = false

  private def getFeideUserWrapper: ServerEndpoint[Any, Eff] = endpoint
    .summary("Get the Feide user wrapper based on the ID token. Used for Feide auth.")
    .post
    .in("get-user")
    .in(jsonBody[FeideIdToken])
    .out(jsonBody[Option[FeideUserWrapper]])
    .errorOut(errorOutputsFor(400))
    .serverLogic(userService.getFeideUserWrapperFromIdToken)

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

  override val endpoints: List[ServerEndpoint[Any, Eff]] =
    List(getFeideUserWrapper, cleanupInactiveUsers, sendTestEmail)

}
