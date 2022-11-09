/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.clients

import no.ndla.network.model.{FeideAccessToken, FeideID}
import redis.clients.jedis.JedisPooled

import java.time.LocalDateTime
import scala.jdk.CollectionConverters._
import scala.util.{Success, Try}

trait RedisClient {
  this: FeideApiClient =>
  val redisClient: RedisClient
  class RedisClient(
      host: String,
      port: Int,
      // default to 8 hours cache time
      cacheTimeSeconds: Long = 60 * 60 * 8
  ) {
    val jedis          = new JedisPooled(host, port)
    val feideIdField   = "feideId"
    val timestampField = "timestamp"

    private def fetchFeideIdAndUpdateCache(accessToken: FeideAccessToken): Try[FeideID] = {
      feideApiClient
        .getFeideID(Some(accessToken))
        .map(id => {
          val values = Map(
            feideIdField   -> id,
            timestampField -> LocalDateTime.now().plusSeconds(cacheTimeSeconds).toString
          )
          jedis.hset(accessToken, values.asJava)
          jedis.expire(accessToken, cacheTimeSeconds)
          id
        })
    }

    def memoize(maybeAccessToken: Option[FeideAccessToken]): Try[FeideID] = {
      feideApiClient
        .getFeideAccessTokenOrFail(maybeAccessToken)
        .flatMap(accessToken => {
          if (jedis.exists(accessToken)) {
            val values            = jedis.hgetAll(accessToken)
            val previousTimestamp = LocalDateTime.parse(values.get(timestampField))
            if (LocalDateTime.now().isAfter(previousTimestamp)) {
              fetchFeideIdAndUpdateCache(accessToken)
            } else {
              Success(values.get(feideIdField))
            }
          } else {
            fetchFeideIdAndUpdateCache(accessToken)
          }
        })
    }
  }
}
