/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import com.typesafe.scalalogging.StrictLogging
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

  def expire(key: String, seconds: Long): Try[Long]         = Try(jedis.expire(key, seconds)).handleJedisError(0L)
  def hget(key: String, field: String): Try[Option[String]] = Try(Option(jedis.hget(key, field))).handleJedisError(None)
  def hset(key: String, field: String, value: String): Try[Long] =
    Try(jedis.hset(key, field, value)).handleJedisError(0L)
  def ttl(key: String): Try[Long] = Try(jedis.ttl(key)).handleJedisError(0L)
}
