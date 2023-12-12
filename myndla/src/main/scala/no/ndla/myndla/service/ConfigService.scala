/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.service

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.myndla.model.api.config.ConfigMetaRestricted
import no.ndla.myndla.model.{api, domain}
import no.ndla.myndla.model.domain.config.{BooleanValue, ConfigKey, ConfigMetaValue, StringListValue}
import no.ndla.myndla.repository.ConfigRepository
import no.ndla.network.tapir.auth.Permission.LEARNINGPATH_API_ADMIN
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Success, Try}

trait ConfigService {
  this: ConfigRepository with Clock =>

  val configService: ConfigService

  class ConfigService {

    def isWriteRestricted: Boolean =
      configRepository
        .getConfigWithKey(ConfigKey.LearningpathWriteRestricted)
        .map(_.value)
        .collectFirst { case BooleanValue(value) => value }
        .getOrElse(false)

    def isMyNDLAWriteRestricted: Boolean =
      configRepository
        .getConfigWithKey(ConfigKey.MyNDLAWriteRestricted)
        .map(_.value)
        .collectFirst { case BooleanValue(value) => value }
        .getOrElse(false)

    def getMyNDLAEnabledOrgs: Try[List[String]] = Try {
      configRepository
        .getConfigWithKey(ConfigKey.ArenaEnabledOrgs)
        .map(_.value)
        .collectFirst { case StringListValue(value) => value }
        .getOrElse(List.empty)
    }

    def getConfig(configKey: ConfigKey): Try[ConfigMetaRestricted] = {
      configRepository.getConfigWithKey(configKey) match {
        case None      => Failure(NotFoundException(s"Configuration with key $configKey does not exist"))
        case Some(key) => Success(asApiConfigRestricted(key))
      }
    }

    def asApiConfigRestricted(configValue: domain.config.ConfigMeta): api.config.ConfigMetaRestricted = {
      api.config.ConfigMetaRestricted(
        key = configValue.key.entryName,
        value = configValue.valueToEither
      )
    }

    def updateConfig(
        configKey: ConfigKey,
        value: api.config.ConfigMetaValue,
        userInfo: TokenUser
    ): Try[api.config.ConfigMeta] = if (!userInfo.hasPermission(LEARNINGPATH_API_ADMIN)) {
      Failure(AccessDeniedException("Only administrators can edit configuration."))
    } else {
      val config = domain.config.ConfigMeta(configKey, ConfigMetaValue.from(value), clock.now(), userInfo.id)
      for {
        validated <- config.validate
        stored    <- configRepository.updateConfigParam(validated)
      } yield asApiConfig(stored)
    }

    def asApiConfig(configValue: domain.config.ConfigMeta): api.config.ConfigMeta = {
      api.config.ConfigMeta(
        configValue.key.entryName,
        configValue.valueToEither,
        configValue.updatedAt,
        configValue.updatedBy
      )
    }
  }
}
