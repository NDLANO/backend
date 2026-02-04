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

  private def cleanupOldUsers: ServerEndpoint[Any, Eff] = endpoint
    .summary("Cleanup old users from myNDLA")
    .post
    .in("cleanup-old-users")
    .out(emptyOutput)
    .errorOut(errorOutputsFor(400))
    .serverLogicPure(_ => userService.cleanupOldUsers())

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(getDomainUser)

}
