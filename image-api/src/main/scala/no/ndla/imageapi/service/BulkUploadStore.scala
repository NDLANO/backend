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
import no.ndla.network.clients.rediscache.{RedisStoredType, ScalaJedis}

import java.util.UUID
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Success, Try}

object BulkUploadType extends RedisStoredType {
  override val cacheTime: Duration = 24.hours
  override val prefix: String      = "image-bulk-upload"
  override def refreshTTL: Boolean = true
  val stateField: String           = "state"
}

class BulkUploadStore(jedis: ScalaJedis) extends StrictLogging {
  def get(uploadId: UUID): Try[Option[BulkUploadStateDTO]] = jedis
    .hget(BulkUploadType, uploadId.toString, BulkUploadType.stateField)
    .flatMap {
      case None       => Success(None)
      case Some(json) => CirceUtil.tryParseAs[BulkUploadStateDTO](json).map(Some(_))
    }

  def set(uploadId: UUID, state: BulkUploadStateDTO): Try[Unit] = {
    val json = CirceUtil.toJsonString(state)
    for {
      newTtl <- jedis.getFieldNewTtl(BulkUploadType, uploadId.toString, BulkUploadType.stateField)
      _      <- jedis.hset(BulkUploadType, uploadId.toString, BulkUploadType.stateField, json)
      _      <- jedis.hexpire(BulkUploadType, uploadId.toString, BulkUploadType.stateField, newTtl)
    } yield ()
  }
}

object BulkUploadStore {
  def apply(host: String, port: Int): BulkUploadStore = new BulkUploadStore(ScalaJedis(host, port))
}
