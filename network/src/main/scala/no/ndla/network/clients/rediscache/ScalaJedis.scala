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

import scala.jdk.CollectionConverters.*
import scala.util.Try

case class RedisConnectionFailureException(cause: Throwable)
    extends RuntimeException("Could not connect to Redis", cause)

class ScalaJedis(host: String, port: Int) extends StrictLogging {
  private val jedis = JedisClient.create(host, port)

  def hexpire(prefix: RedisStoredType, key: String, field: String, seconds: Long): Try[Long] =
    doSafe(_.hexpire(prefix.getKey(key), seconds, field).toSingleScalaLong)
  def hexpireAt(prefix: RedisStoredType, key: String, field: String, unixTime: Long): Try[Long] =
    doSafe(_.hexpireAt(prefix.getKey(key), unixTime, field).toSingleScalaLong)
  def hget(prefix: RedisStoredType, key: String, field: String): Try[Option[String]] =
    doSafeOpt(_.hget(prefix.getKey(key), field))
  def hset(prefix: RedisStoredType, key: String, field: String, value: String): Try[Long] =
    doSafe(_.hset(prefix.getKey(key), field, value))
  def httl(prefix: RedisStoredType, key: String, field: String): Try[Long] =
    doSafe(_.httl(prefix.getKey(key), field).toSingleScalaLong)

  def ping(): Try[Unit] = doSafe(_.ping: Unit)

  def getFieldNewTtl(redisType: RedisStoredType, key: String, field: String): Try[Long] = for {
    existingExpireTime <- httl(redisType, key, field)
    keepExistingTTL     = existingExpireTime > 0 && !redisType.refreshTTL
    newExpireTime       = Option.when(keepExistingTTL)(existingExpireTime)
  } yield newExpireTime.getOrElse(redisType.cacheTime.toSeconds)

  private def doSafe[T](f: JedisClient => T): Try[T]            = Try(f(jedis))
  private def doSafeOpt[T](f: JedisClient => T): Try[Option[T]] = Try(Option(f(jedis)))

  extension (javaLongList: java.util.List[java.lang.Long]) {
    def toSingleScalaLong: Long = javaLongList.asScala.head
  }
}
