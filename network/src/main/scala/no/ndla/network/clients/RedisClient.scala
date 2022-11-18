/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.clients

import no.ndla.common.implicits.TryQuestionMark
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
    val jedis           = new JedisPooled(host, port)
    val feideIdField    = "feideId"
    val feideUserField  = "feideUser"
    val feideGroupField = "feideGroup"

    private def getKeyExpireTime(key: String): Try[Long] = Try {
      val existingExpireTime = jedis.ttl(key)
      val newExpireTime      = if (existingExpireTime > 0) existingExpireTime else cacheTimeSeconds
      newExpireTime
    }

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
      val newExpireTime                         = getKeyExpireTime(accessToken).?
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
      val newExpireTime = getKeyExpireTime(accessToken).?
      jedis.hset(accessToken, feideIdField, feideId)
      jedis.expire(accessToken, newExpireTime)
      feideId
    }

    def getCountyFromCache(accessToken: FeideAccessToken): Try[Option[String]] = Try {
      if (jedis.hexists(accessToken, feideGroupField)) {
        Some(jedis.hget(accessToken, feideGroupField))
      } else {
        None
      }
    }

    def updateCacheAndReturnCounty(
        accessToken: FeideAccessToken,
        feideCounty: String
    ): Try[String] = Try {
      val newExpireTime = getKeyExpireTime(accessToken).?
      jedis.hset(accessToken, feideGroupField, feideCounty)
      jedis.expire(accessToken, newExpireTime)
      feideCounty
    }

  }
}
