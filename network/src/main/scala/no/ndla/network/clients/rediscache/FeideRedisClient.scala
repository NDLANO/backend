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

  private def updateFeideCache(accessToken: FeideAccessToken, field: String, data: String): Try[?] = {
    for {
      newExpireTime <- jedis.getNewTTL(FeideToken, accessToken)
      _             <- jedis.hset(FeideToken, accessToken, field, data)
      _             <- jedis.expire(FeideToken, accessToken, newExpireTime)
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
      accessToken: FeideAccessToken,
      feideExtendedUser: FeideExtendedUserInfo,
  ): Try[FeideExtendedUserInfo] = {
    updateFeideCache(accessToken, feideUserField, CirceUtil.toJsonString(feideExtendedUser)).map(_ => feideExtendedUser)
  }

  def getFeideIdFromCache(accessToken: FeideAccessToken): Try[Option[FeideID]] =
    jedis.hget(FeideToken, accessToken, feideIdField)

  def updateCacheAndReturnFeideId(accessToken: FeideAccessToken, feideId: FeideID): Try[FeideID] = {
    updateFeideCache(accessToken, feideIdField, feideId).map(_ => feideId)
  }

  def getOrganizationFromCache(accessToken: FeideAccessToken): Try[Option[String]] =
    jedis.hget(FeideToken, accessToken, feideGroupField)

  def updateCacheAndReturnOrganization(accessToken: FeideAccessToken, feideOrganization: String): Try[String] = {
    updateFeideCache(accessToken, feideGroupField, feideOrganization).map(_ => feideOrganization)
  }

  def getGroupsFromCache(accessToken: FeideAccessToken): Try[Option[Seq[FeideGroup]]] = {
    jedis
      .hget(FeideToken, accessToken, feideGroupsField)
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

  def updateCacheAndReturnGroups(accessToken: FeideAccessToken, feideGroups: Seq[FeideGroup]): Try[Seq[FeideGroup]] = {
    updateFeideCache(accessToken, feideGroupsField, CirceUtil.toJsonString(feideGroups)).map(_ => feideGroups)
  }

  def getFeideSession(idToken: FeideIdToken): Try[Option[FeideAccessToken]] = jedis.get(FeideToken, idToken.sub)

  def setFeideSession(idToken: FeideIdToken, accessToken: FeideAccessToken): Try[Unit] = for {
    key = idToken.sub
    _  <- jedis.set(FeideToken, key, accessToken)
    _  <- jedis.expireAt(FeideToken, key, idToken.exp)
  } yield ()
}
