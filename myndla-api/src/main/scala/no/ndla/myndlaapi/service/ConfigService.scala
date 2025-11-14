/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import no.ndla.common.{Clock, model}
import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.common.model.api.config.ConfigMetaRestrictedDTO
import no.ndla.common.model.domain.config.{BooleanValue, ConfigKey, ConfigMeta, ConfigMetaValue, StringListValue}
import no.ndla.myndlaapi.repository.ConfigRepository
import no.ndla.network.tapir.auth.Permission.LEARNINGPATH_API_ADMIN
import no.ndla.network.tapir.auth.TokenUser

import scala.annotation.unused
import scala.util.{Failure, Success, Try}

class ConfigService(using configRepository: ConfigRepository, clock: Clock) {

  def isMyNDLAWriteRestricted: Try[Boolean] = getConfigBoolean(ConfigKey.MyNDLAWriteRestricted)

  private def getConfigBoolean(configKey: ConfigKey): Try[Boolean] = {
    configRepository
      .getConfigWithKey(configKey)
      .map(config =>
        config
          .map(_.value)
          .collectFirst { case BooleanValue(value) =>
            value
          }
          .getOrElse(false)
      )
  }

  @unused // for now
  private def getConfigStringList(configKey: ConfigKey): Try[List[String]] = {
    configRepository
      .getConfigWithKey(configKey)
      .map(config =>
        config
          .map(_.value)
          .collectFirst { case StringListValue(value) =>
            value
          }
          .getOrElse(List.empty)
      )
  }

  def getConfig(configKey: ConfigKey): Try[ConfigMetaRestrictedDTO] = {
    configRepository
      .getConfigWithKey(configKey)
      .flatMap {
        case None      => Failure(NotFoundException(s"Configuration with key $configKey does not exist"))
        case Some(key) => Success(asApiConfigRestricted(key))
      }
  }

  private def asApiConfigRestricted(configValue: ConfigMeta): ConfigMetaRestrictedDTO = {
    model.api.config.ConfigMetaRestrictedDTO(key = configValue.key.entryName, value = configValue.valueToEither)
  }

  def updateConfig(
      configKey: ConfigKey,
      value: model.api.config.ConfigMetaValueDTO,
      userInfo: TokenUser,
  ): Try[model.api.config.ConfigMetaDTO] =
    if (!userInfo.hasPermission(LEARNINGPATH_API_ADMIN)) {
      Failure(AccessDeniedException("Only administrators can edit configuration."))
    } else {
      val config = ConfigMeta(configKey, ConfigMetaValue.from(value), clock.now(), userInfo.id)
      for {
        validated <- config.validate
        stored    <- configRepository.updateConfigParam(validated)
      } yield asApiConfig(stored)
    }

  private def asApiConfig(configValue: ConfigMeta): model.api.config.ConfigMetaDTO = {
    model
      .api
      .config
      .ConfigMetaDTO(configValue.key.entryName, configValue.valueToEither, configValue.updatedAt, configValue.updatedBy)
  }
}
