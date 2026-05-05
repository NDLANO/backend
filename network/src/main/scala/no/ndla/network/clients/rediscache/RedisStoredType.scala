/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients.rediscache

import scala.concurrent.duration.Duration

trait RedisStoredType {
  val cacheTime: Duration
  val prefix: String
  def getKey(key: String): String = s"$prefix:$key"
  def refreshTTL: Boolean         = false
}
