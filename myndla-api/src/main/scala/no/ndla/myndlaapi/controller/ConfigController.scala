/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import no.ndla.myndlaapi.Eff
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.{Service, TapirErrorHelpers}
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.codec.enumeratum._
import io.circe.generic.auto._
import no.ndla.common.model.api.config.{ConfigMeta, ConfigMetaRestricted, ConfigMetaValue}
import no.ndla.common.model.domain.config.ConfigKey
import no.ndla.myndlaapi.service.ConfigService
import no.ndla.network.tapir.auth.Permission.LEARNINGPATH_API_ADMIN

trait ConfigController {
  this: ErrorHelpers with TapirErrorHelpers with ConfigService =>

  val configController: ConfigController

  class ConfigController extends Service[Eff] {
    override val serviceName: String = "config"

    override protected val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

    import ErrorHelpers._

    val pathConfigKey: EndpointInput.PathCapture[ConfigKey] =
      path[ConfigKey]("config-key")
        .description(s"The of configuration value. Can only be one of '${ConfigKey.all.mkString("', '")}'")

    def getConfig: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get db configuration by key")
      .description("Get db configuration by key")
      .in(pathConfigKey)
      .out(jsonBody[ConfigMetaRestricted])
      .errorOut(errorOutputsFor(401, 403, 404))
      .serverLogicPure { configKey =>
        configService.getConfig(configKey).handleErrorsOrOk
      }

    def updateConfig: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Update configuration used by api.")
      .description("Update configuration used by api.")
      .in(pathConfigKey)
      .in(jsonBody[ConfigMetaValue])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[ConfigMeta])
      .requirePermission(LEARNINGPATH_API_ADMIN)
      .serverLogicPure { user =>
        { case (configKey, configValue) =>
          configService.updateConfig(configKey, configValue, user).handleErrorsOrOk
        }
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getConfig,
      updateConfig
    )
  }

}
