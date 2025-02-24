/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import no.ndla.common.Clock
import no.ndla.database.DBUtility
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.network.clients.FeideApiClient
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.network.model.FeideAccessToken

import scala.util.Try

trait ArenaReadService {
  this: FeideApiClient & UserService & Clock & ConfigService & FolderRepository & UserRepository & NodeBBClient &
    DBUtility =>
  val arenaReadService: ArenaReadService

  class ArenaReadService {

    def deleteAllUserData(feideAccessToken: Option[FeideAccessToken]): Try[Unit] =
      DBUtil.rollbackOnFailure(session => {
        for {
          feideToken   <- feideApiClient.getFeideAccessTokenOrFail(feideAccessToken)
          feideId      <- feideApiClient.getFeideID(feideAccessToken)
          nodebbUserId <- nodebb.getUserId(feideToken)
          _            <- folderRepository.deleteAllUserFolders(feideId)(session)
          _            <- folderRepository.deleteAllUserResources(feideId)(session)
          _            <- userRepository.deleteUser(feideId)(session)
          _            <- nodebb.deleteUser(nodebbUserId, feideToken)
        } yield ()
      })
  }
}
