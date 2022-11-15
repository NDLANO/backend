/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.clients

import no.ndla.network.model.{FeideAccessToken, FeideID}
import org.json4s.DefaultFormats
import redis.clients.jedis.JedisPooled
import org.json4s.native.Serialization._

import scala.util.Try

trait RedisClient {
  val redisClient: RedisClient
  class RedisClient(
      host: String,
      port: Int,
      // default to 8 hours cache time
      cacheTimeSeconds: Long = 60 * 60 * 8
  ) {
    val jedis          = new JedisPooled(host, port)
    val feideIdField   = "feideId"
    val feideUserField = "feideUser"

    def getFeideUserFromCache(accessToken: FeideAccessToken): Try[Option[FeideExtendedUserInfo]] = Try {
      implicit val formats: DefaultFormats.type = DefaultFormats
      if (jedis.hexists(accessToken, feideUserField)) {
        val feideUser = jedis.hget(accessToken, feideUserField)
        Some(read[FeideExtendedUserInfo](feideUser))
      } else {
        None
      }
    }

    def updateCacheAndReturnFeideUser(
        accessToken: FeideAccessToken,
        feideExtendedUser: FeideExtendedUserInfo
    ): Try[FeideExtendedUserInfo] = Try {
      implicit val formats: DefaultFormats.type = DefaultFormats
      val existingExpireTime                    = jedis.ttl(accessToken)
      val newExpireTime                         = if (existingExpireTime > 0) existingExpireTime else cacheTimeSeconds

      jedis.hset(accessToken, feideUserField, write(feideExtendedUser))
      jedis.expire(accessToken, newExpireTime)
      feideExtendedUser
    }

    def getFeideIdFromCache(accessToken: FeideAccessToken): Try[Option[FeideID]] = Try {
      if (jedis.hexists(accessToken, feideIdField)) {
        Some(jedis.hget(accessToken, feideIdField))
      } else {
        None
      }
    }

    def updateCacheAndReturnFeideId(accessToken: FeideAccessToken, feideId: FeideID): Try[FeideID] = Try {
      val existingExpireTime = jedis.ttl(accessToken)
      val newExpireTime      = if (existingExpireTime > 0) existingExpireTime else cacheTimeSeconds

      jedis.hset(accessToken, feideIdField, feideId)
      jedis.expire(accessToken, newExpireTime)
      feideId
    }

  }
}
