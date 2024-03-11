/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla

import no.ndla.common.Clock
import no.ndla.myndla.repository.{ConfigRepository, FolderRepository, UserRepository}
import no.ndla.myndla.service.{
  ConfigService,
  FolderConverterService,
  FolderReadService,
  FolderWriteService,
  UserService
}
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import org.mockito.Mockito.{reset, spy}
import org.scalatestplus.mockito.MockitoSugar.mock

trait TestEnvironment
    extends UserService
    with Clock
    with FeideApiClient
    with RedisClient
    with ConfigService
    with ConfigRepository
    with UserRepository
    with FolderConverterService
    with FolderRepository
    with FolderReadService
    with FolderWriteService {

  val userService: UserService                       = mock[UserService]
  val clock: SystemClock                             = mock[SystemClock]
  val redisClient: RedisClient                       = mock[RedisClient]
  val feideApiClient: FeideApiClient                 = mock[FeideApiClient]
  val configService: ConfigService                   = mock[ConfigService]
  val configRepository: ConfigRepository             = mock[ConfigRepository]
  val folderRepository: FolderRepository             = mock[FolderRepository]
  val userRepository: UserRepository                 = mock[UserRepository]
  val folderReadService: FolderReadService           = mock[FolderReadService]
  val folderWriteService: FolderWriteService         = mock[FolderWriteService]
  val folderConverterService: FolderConverterService = spy(new FolderConverterService)

  def resetMocks(): Unit = {
    reset(userService)
    reset(clock)
    reset(redisClient)
    reset(feideApiClient)
    reset(configService)
    reset(configRepository)
    reset(folderRepository)
    reset(userRepository)
    reset(folderReadService)
    reset(folderWriteService)
    reset(folderConverterService)
  }

}
