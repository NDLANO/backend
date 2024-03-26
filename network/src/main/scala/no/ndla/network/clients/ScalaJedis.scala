/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.clients

import redis.clients.jedis.JedisPooled
import scala.util.Try

class ScalaJedis(host: String, port: Int) {
  private val jedis = new JedisPooled(host, port)

  def expire(key: String, seconds: Long): Try[Long]              = Try(jedis.expire(key, seconds))
  def hget(key: String, field: String): Try[Option[String]]      = Try(Option(jedis.hget(key, field)))
  def hset(key: String, field: String, value: String): Try[Long] = Try(jedis.hset(key, field, value))
  def ttl(key: String): Try[Long]                                = Try(jedis.ttl(key))
}
