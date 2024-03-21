/*
 * Part of NDLA myndla-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import no.ndla.common.{Clock, model}
import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.common.model.api.config.ConfigMetaRestricted
import no.ndla.common.model.domain.config.{BooleanValue, ConfigKey, ConfigMeta, ConfigMetaValue, StringListValue}
import no.ndla.myndlaapi.repository.ConfigRepository
import no.ndla.network.tapir.auth.Permission.LEARNINGPATH_API_ADMIN
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Success, Try}

trait ConfigService {
  this: ConfigRepository with Clock =>

  val configService: ConfigService

  class ConfigService {

    def isWriteRestricted: Try[Boolean] = getConfigBoolean(ConfigKey.LearningpathWriteRestricted)

    def isMyNDLAWriteRestricted: Try[Boolean] = getConfigBoolean(ConfigKey.MyNDLAWriteRestricted)

    def getMyNDLAEnabledOrgs: Try[List[String]] = getConfigStringList(ConfigKey.ArenaEnabledOrgs)

    def getMyNDLAEnabledUsers: Try[List[String]] = getConfigStringList(ConfigKey.ArenaEnabledUsers)

    private def getConfigBoolean(configKey: ConfigKey): Try[Boolean] = {
      configRepository
        .getConfigWithKey(configKey)
        .map(config =>
          config
            .map(_.value)
            .collectFirst { case BooleanValue(value) => value }
            .getOrElse(false)
        )
    }

    private def getConfigStringList(configKey: ConfigKey): Try[List[String]] = {
      configRepository
        .getConfigWithKey(configKey)
        .map(config =>
          config
            .map(_.value)
            .collectFirst { case StringListValue(value) => value }
            .getOrElse(List.empty)
        )
    }

    def getConfig(configKey: ConfigKey): Try[ConfigMetaRestricted] = {
      configRepository.getConfigWithKey(configKey).flatMap {
        case None      => Failure(NotFoundException(s"Configuration with key $configKey does not exist"))
        case Some(key) => Success(asApiConfigRestricted(key))
      }
    }

    private def asApiConfigRestricted(configValue: ConfigMeta): ConfigMetaRestricted = {
      model.api.config.ConfigMetaRestricted(
        key = configValue.key.entryName,
        value = configValue.valueToEither
      )
    }

    def updateConfig(
        configKey: ConfigKey,
        value: model.api.config.ConfigMetaValue,
        userInfo: TokenUser
    ): Try[model.api.config.ConfigMeta] = if (!userInfo.hasPermission(LEARNINGPATH_API_ADMIN)) {
      Failure(AccessDeniedException("Only administrators can edit configuration."))
    } else {
      val config = ConfigMeta(configKey, ConfigMetaValue.from(value), clock.now(), userInfo.id)
      for {
        validated <- config.validate
        stored    <- configRepository.updateConfigParam(validated)
      } yield asApiConfig(stored)
    }

    private def asApiConfig(configValue: ConfigMeta): model.api.config.ConfigMeta = {
      model.api.config.ConfigMeta(
        configValue.key.entryName,
        configValue.valueToEither,
        configValue.updatedAt,
        configValue.updatedBy
      )
    }
  }
}
