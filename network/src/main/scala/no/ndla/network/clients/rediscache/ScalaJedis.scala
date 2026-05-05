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
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.exceptions.JedisConnectionException

import scala.util.{Failure, Success, Try}

class ScalaJedis(host: String, port: Int, environment: String) extends StrictLogging {
  private val jedis = new JedisPooled(host, port)

  private implicit class TryOps[T](t: Try[T]) {
    def handleJedisError(fallback: T): Try[T] = t.recoverWith {
      case jce: JedisConnectionException if environment == "local" =>
        logger.error("Could not connect to redis instance, but allowing since we are in local environment", jce)
        Success(fallback)
      case ex => Failure(ex)
    }
  }

  private def _expire(key: String, seconds: Long): Try[Long]         = Try(jedis.expire(key, seconds)).handleJedisError(0L)
  private def _hget(key: String, field: String): Try[Option[String]] = Try(Option(jedis.hget(key, field)))
    .handleJedisError(None)
  private def _hset(key: String, field: String, value: String): Try[Long] = Try(jedis.hset(key, field, value))
    .handleJedisError(0L)
  private def _ttl(key: String): Try[Long] = Try(jedis.ttl(key)).handleJedisError(0L)

  def expire(prefix: RedisStoredType, key: String, seconds: Long): Try[Long]              = _expire(prefix.getKey(key), seconds)
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
