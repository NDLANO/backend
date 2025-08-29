/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.{ErrorHelpers, TapirController}
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.codec.enumeratum.*
import no.ndla.common.model.api.config.{ConfigMetaDTO, ConfigMetaRestrictedDTO, ConfigMetaValueDTO}
import no.ndla.common.model.domain.config.ConfigKey
import no.ndla.myndlaapi.service.ConfigService
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.auth.Permission.LEARNINGPATH_API_ADMIN

class ConfigController(using
    errorHandling: ControllerErrorHandling,
    errorHelpers: ErrorHelpers,
    configService: ConfigService,
    myNDLAApiClient: MyNDLAApiClient
) extends TapirController {
  override val serviceName: String = "config"

  override protected val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

  val pathConfigKey: EndpointInput.PathCapture[ConfigKey] =
    path[ConfigKey]("config-key")
      .description(s"The of configuration value. Can only be one of '${ConfigKey.all.mkString("', '")}'")

  def getConfig: ServerEndpoint[Any, Eff] = endpoint.get
    .summary("Get db configuration by key")
    .description("Get db configuration by key")
    .in(pathConfigKey)
    .out(jsonBody[ConfigMetaRestrictedDTO])
    .errorOut(errorOutputsFor(401, 403, 404))
    .serverLogicPure { configKey =>
      configService.getConfig(configKey)
    }

  def updateConfig: ServerEndpoint[Any, Eff] = endpoint.post
    .summary("Update configuration used by api.")
    .description("Update configuration used by api.")
    .in(pathConfigKey)
    .in(jsonBody[ConfigMetaValueDTO])
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[ConfigMetaDTO])
    .requirePermission(LEARNINGPATH_API_ADMIN)
    .serverLogicPure { user =>
      { case (configKey, configValue) =>
        configService.updateConfig(configKey, configValue, user)
      }
    }

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
    getConfig,
    updateConfig
  )
}
