/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.clients

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.*
import no.ndla.network.model.{FeideAccessToken, FeideID}

import scala.util.{Failure, Success, Try}

sealed trait CacheResult[T]
case class CacheHit[T](value: T) extends CacheResult[T]
case class CacheMiss[T]()        extends CacheResult[T]

trait RedisClient {
  val redisClient: RedisClient
  class RedisClient(
      host: String,
      port: Int,
      // default to 8 hours cache time
      cacheTimeSeconds: Long = 60 * 60 * 8
  ) extends StrictLogging {
    val jedis                    = new ScalaJedis(host, port)
    private val feideIdField     = "feideId"
    private val feideUserField   = "feideUser"
    private val feideGroupField  = "feideGroup"
    private val feideGroupsField = "feideGroups"

    private def getKeyExpireTime(key: String): Try[Long] = permitTry {
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

    private def cacheEmptyField(accessToken: FeideAccessToken, field: String): Try[?] = {
      updateCache(accessToken, field, "")
    }

    def getFeideUserFromCache(accessToken: FeideAccessToken): Try[Option[FeideExtendedUserInfo]] = {
      jedis.hget(accessToken, feideUserField).map {
        case Some(feideUser) =>
          CirceUtil.tryParseAs[FeideExtendedUserInfo](feideUser) match {
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
      updateCache(accessToken, feideUserField, CirceUtil.toJsonString(feideExtendedUser)).map(_ => feideExtendedUser)
    }

    def getFeideIdFromCache(accessToken: FeideAccessToken): Try[Option[FeideID]] =
      jedis.hget(accessToken, feideIdField)

    def updateCacheAndReturnFeideId(accessToken: FeideAccessToken, feideId: FeideID): Try[FeideID] = {
      updateCache(accessToken, feideIdField, feideId).map(_ => feideId)
    }

    def getOrganizationFromCache(accessToken: FeideAccessToken): Try[CacheResult[Option[String]]] = {
      val cachedValue = jedis.hget(accessToken, feideGroupField).?
      cachedValue match {
        case Some(value) if value.nonEmpty => Success(CacheHit(Some(value)))
        case Some(_)                       => Success(CacheHit(None))
        case None                          => Success(CacheMiss())
      }
    }

    def updateCacheAndReturnOrganization(
        accessToken: FeideAccessToken,
        feideOrganization: Option[String]
    ): Try[Option[String]] = {
      (feideOrganization match {
        case Some(value) => updateCache(accessToken, feideGroupField, value)
        case None        => cacheEmptyField(accessToken, feideGroupField)
      }).map(_ => feideOrganization)
    }

    def getGroupsFromCache(accessToken: FeideAccessToken): Try[Option[Seq[FeideGroup]]] = {
      jedis.hget(accessToken, feideGroupsField).map {
        case Some(feideGroups) =>
          CirceUtil.tryParseAs[Seq[FeideGroup]](feideGroups) match {
            case Success(value) => Some(value)
            case Failure(ex) =>
              logger.warn(s"Failed to deserialize cached value from field $feideGroupsField. Updating cache.", ex)
              None
          }
        case None => None
      }
    }

    def updateCacheAndReturnGroups(
        accessToken: FeideAccessToken,
        feideGroups: Seq[FeideGroup]
    ): Try[Seq[FeideGroup]] = {
      updateCache(accessToken, feideGroupsField, CirceUtil.toJsonString(feideGroups)).map(_ => feideGroups)
    }

  }
}
