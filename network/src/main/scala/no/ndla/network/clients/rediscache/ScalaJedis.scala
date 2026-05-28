/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients.rediscache

import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.clients.rediscache.RedisStoredType
import redis.clients.jedis.RedisClient as JedisClient

import scala.util.Try

case class RedisConnectionFailureException(cause: Throwable)
    extends RuntimeException("Could not connect to Redis", cause)

class ScalaJedis(host: String, port: Int) extends StrictLogging {
  private val jedis = JedisClient.create(host, port)

  private def _expire(key: String, seconds: Long): Try[Long]              = Try(jedis.expire(key, seconds))
  private def _expireAt(key: String, unixTime: Long): Try[Long]           = Try(jedis.expireAt(key, unixTime))
  private def _get(key: String): Try[Option[String]]                      = Try(Option(jedis.get(key)))
  private def _set(key: String, value: String): Try[String]               = Try(jedis.set(key, value))
  private def _hget(key: String, field: String): Try[Option[String]]      = Try(Option(jedis.hget(key, field)))
  private def _hset(key: String, field: String, value: String): Try[Long] = Try(jedis.hset(key, field, value))
  private def _ttl(key: String): Try[Long]                                = Try(jedis.ttl(key))

  def ping(): Try[Unit]                                                         = Try(jedis.ping: Unit)
  def expire(prefix: RedisStoredType, key: String, seconds: Long): Try[Long]    = _expire(prefix.getKey(key), seconds)
  def expireAt(prefix: RedisStoredType, key: String, unixTime: Long): Try[Long] =
    _expireAt(prefix.getKey(key), unixTime)
  def get(prefix: RedisStoredType, key: String): Try[Option[String]]                      = _get(prefix.getKey(key))
  def set(prefix: RedisStoredType, key: String, value: String): Try[String]               = _set(prefix.getKey(key), value)
  def hget(prefix: RedisStoredType, key: String, field: String): Try[Option[String]]      = _hget(prefix.getKey(key), field)
  def hset(prefix: RedisStoredType, key: String, field: String, value: String): Try[Long] =
    _hset(prefix.getKey(key), field, value)
  def ttl(prefix: RedisStoredType, key: String): Try[Long] = _ttl(prefix.getKey(key))

  def getNewTTL(redisType: RedisStoredType, key: String): Try[Long] = for {
    existingExpireTime <- ttl(redisType, key)
    keepExistingTTL     = existingExpireTime > 0 && !redisType.refreshTTL
    newExpireTime       = Option.when(keepExistingTTL)(existingExpireTime)
  } yield newExpireTime.getOrElse(redisType.cacheTime.toSeconds)
}
