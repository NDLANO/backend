/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.clients

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.network.model.{FeideAccessToken, FeideID}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._

import scala.util.{Failure, Success, Try}

trait RedisClient {
  val redisClient: RedisClient
  class RedisClient(
      host: String,
      port: Int,
      // default to 8 hours cache time
      cacheTimeSeconds: Long = 60 * 60 * 8
  ) extends StrictLogging {
    val jedis           = new ScalaJedis(host, port)
    val feideIdField    = "feideId"
    val feideUserField  = "feideUser"
    val feideGroupField = "feideGroup"

    private def getKeyExpireTime(key: String): Try[Long] = {
      val existingExpireTime = jedis.ttl(key).?
      val newExpireTime      = if (existingExpireTime > 0) existingExpireTime else cacheTimeSeconds
      Success(newExpireTime)
    }

    private def updateCache(accessToken: FeideAccessToken, field: String, data: String): Try[_] = {
      for {
        newExpireTime <- getKeyExpireTime(accessToken)
        _             <- jedis.hset(accessToken, field, data)
        _             <- jedis.expire(accessToken, newExpireTime)
      } yield ()
    }

    def getFeideUserFromCache(accessToken: FeideAccessToken): Try[Option[FeideExtendedUserInfo]] = {
      implicit val formats: DefaultFormats.type = DefaultFormats
      jedis.hget(accessToken, feideUserField).map {
        case Some(feideUser) =>
          Try(read[FeideExtendedUserInfo](feideUser)) match {
            case Success(value) => Some(value)
            case Failure(ex) =>
              logger.warn(s"Failed to deserialize cached value from field $feideUserField. Updating cache.", ex)
              None
          }
        case None => None
      }
    }

    def updateCacheAndReturnFeideUser(
        accessToken: FeideAccessToken,
        feideExtendedUser: FeideExtendedUserInfo
    ): Try[FeideExtendedUserInfo] = {
      implicit val formats: DefaultFormats.type = DefaultFormats
      updateCache(accessToken, feideUserField, write(feideExtendedUser)).map(_ => feideExtendedUser)
    }

    def getFeideIdFromCache(accessToken: FeideAccessToken): Try[Option[FeideID]] =
      jedis.hget(accessToken, feideIdField)

    def updateCacheAndReturnFeideId(accessToken: FeideAccessToken, feideId: FeideID): Try[FeideID] = {
      updateCache(accessToken, feideIdField, feideId).map(_ => feideId)
    }

    def getOrganizationFromCache(accessToken: FeideAccessToken): Try[Option[String]] =
      jedis.hget(accessToken, feideGroupField)

    def updateCacheAndReturnOrganization(accessToken: FeideAccessToken, feideOrganization: String): Try[String] = {
      updateCache(accessToken, feideGroupField, feideOrganization).map(_ => feideOrganization)
    }

  }
}
