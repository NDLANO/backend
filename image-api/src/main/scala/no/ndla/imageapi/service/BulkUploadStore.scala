/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.imageapi.model.api.bulk.BulkUploadStateDTO
import no.ndla.network.clients.rediscache.{FeideRedisClient, RedisStoredType}

import java.util.UUID
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Success, Try}

object BulkUploadType extends RedisStoredType {
  override val cacheTime: Duration = 24.hours
  override val prefix: String      = "image-bulk-upload"
  override def refreshTTL: Boolean = true
  val stateField: String           = "state"
}

class BulkUploadStore(using redisClient: FeideRedisClient) extends StrictLogging {
  private val jedis = redisClient.jedis

  def get(uploadId: UUID): Try[Option[BulkUploadStateDTO]] = jedis
    .hget(BulkUploadType, uploadId.toString, BulkUploadType.stateField)
    .flatMap {
      case None       => Success(None)
      case Some(json) => CirceUtil.tryParseAs[BulkUploadStateDTO](json).map(Some(_))
    }

  def set(uploadId: UUID, state: BulkUploadStateDTO): Try[Unit] = {
    val json = CirceUtil.toJsonString(state)
    for {
      newTtl <- jedis.getNewTTL(BulkUploadType, uploadId.toString)
      _      <- jedis.hset(BulkUploadType, uploadId.toString, BulkUploadType.stateField, json)
      _      <- jedis.expire(BulkUploadType, uploadId.toString, newTtl)
    } yield ()
  }
}
