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
import org.mockito.Mockito.spy
import org.mockito.MockitoSugar.{mock, reset}

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
    reset(
      userService,
      clock,
      redisClient,
      feideApiClient,
      configService,
      configRepository,
      folderRepository,
      userRepository,
      folderReadService,
      folderWriteService,
      folderConverterService
    )
  }

}
