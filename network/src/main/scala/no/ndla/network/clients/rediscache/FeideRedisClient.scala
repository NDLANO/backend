/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients.rediscache

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.network.clients.rediscache.RedisStoredType
import no.ndla.network.clients.{FeideExtendedUserInfo, FeideGroup}
import no.ndla.network.model.{FeideAccessToken, FeideID, FeideIdToken}

import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success, Try}

object FeideToken extends RedisStoredType {
  override val cacheTime: Duration = 8.hours
  override val prefix: String      = "feide"

  val feideIdField      = "feideId"
  val feideUserField    = "feideUser"
  val feideGroupField   = "feideGroup"
  val feideGroupsField  = "feideGroups"
  val feideSessionField = "feideSession"
}

class FeideRedisClient(host: String, port: Int) extends StrictLogging {
  val jedis = new ScalaJedis(host, port)

  import FeideToken.*

  def ping(): Try[Unit] = jedis.ping()

  private def updateFeideCache(feideId: FeideID, field: String, data: String): Try[?] = {
    for {
      newExpireTime <- jedis.getFieldNewTtl(FeideToken, feideId, field)
      _             <- jedis.hset(FeideToken, feideId, field, data)
      _             <- jedis.hexpire(FeideToken, feideId, field, newExpireTime)
    } yield ()
  }

  def getFeideUserFromCache(accessToken: FeideAccessToken): Try[Option[FeideExtendedUserInfo]] = {
    jedis
      .hget(FeideToken, accessToken, feideUserField)
      .map {
        case Some(feideUser) => CirceUtil.tryParseAs[FeideExtendedUserInfo](feideUser) match {
            case Success(value) => Some(value)
            case Failure(ex)    =>
              logger.warn(s"Failed to deserialize cached value from field $feideUserField. Updating cache.", ex)
              None
          }
        case None => None
      }
  }

  def updateCacheAndReturnFeideUser(
      feideId: FeideID,
      feideExtendedUser: FeideExtendedUserInfo,
  ): Try[FeideExtendedUserInfo] = {
    updateFeideCache(feideId, feideUserField, CirceUtil.toJsonString(feideExtendedUser)).map(_ => feideExtendedUser)
  }

  def getGroupsFromCache(feideId: FeideID): Try[Option[Seq[FeideGroup]]] = {
    jedis
      .hget(FeideToken, feideId, feideGroupsField)
      .map {
        case Some(feideGroups) => CirceUtil.tryParseAs[Seq[FeideGroup]](feideGroups) match {
            case Success(value) => Some(value)
            case Failure(ex)    =>
              logger.warn(s"Failed to deserialize cached value from field $feideGroupsField. Updating cache.", ex)
              None
          }
        case None => None
      }
  }

  def updateCacheAndReturnGroups(feideId: FeideID, feideGroups: Seq[FeideGroup]): Try[Seq[FeideGroup]] = {
    updateFeideCache(feideId, feideGroupsField, CirceUtil.toJsonString(feideGroups)).map(_ => feideGroups)
  }

  def getFeideSession(idToken: FeideIdToken): Try[Option[FeideAccessToken]] =
    jedis.hget(FeideToken, idToken.sub, feideSessionField)

  def setFeideSession(idToken: FeideIdToken, accessToken: FeideAccessToken): Try[Unit] = for {
    key = idToken.sub
    _  <- jedis.hset(FeideToken, key, feideSessionField, accessToken)
    _  <- jedis.hexpireAt(FeideToken, key, feideSessionField, idToken.exp)
  } yield ()
}
