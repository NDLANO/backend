/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.common.CirceUtil
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.imageapi.model.api.bulk.{BulkUploadItemDTO, BulkUploadItemStatus, BulkUploadStateDTO, BulkUploadStatus}
import no.ndla.network.clients.rediscache.{FeideRedisClient, RedisStoredType, ScalaJedis}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import scala.util.{Failure, Success}

class BulkUploadStoreTest extends UnitSuite with TestEnvironment {

  given redisClient: FeideRedisClient = mock[FeideRedisClient]
  val jedis: ScalaJedis               = mock[ScalaJedis]
  when(redisClient.jedis).thenReturn(jedis)

  override implicit lazy val bulkUploadStore: BulkUploadStore = new BulkUploadStore
  private val store: BulkUploadStore                          = bulkUploadStore

  override def beforeEach(): Unit = reset(jedis)

  private val sampleState = BulkUploadStateDTO(
    status = BulkUploadStatus.Running,
    total = 2,
    completed = 1,
    failed = 0,
    items = List(
      BulkUploadItemDTO(Some("first.jpg"), BulkUploadItemStatus.Done, None, None),
      BulkUploadItemDTO(Some("second.jpg"), BulkUploadItemStatus.Pending, None, None),
    ),
    error = None,
  )

  test("get returns Success(None) when redis has no entry for the upload-id") {
    val uploadId = UUID.randomUUID()
    when(jedis.hget(eqTo(BulkUploadType), eqTo(uploadId.toString), eqTo(BulkUploadType.stateField))).thenReturn(
      Success(None)
    )

    store.get(uploadId) should be(Success(None))
  }

  test("get returns Failure when the cached JSON cannot be parsed") {
    val uploadId = UUID.randomUUID()
    when(jedis.hget(eqTo(BulkUploadType), eqTo(uploadId.toString), eqTo(BulkUploadType.stateField))).thenReturn(
      Success(Some("not-json"))
    )

    store.get(uploadId) shouldBe a[Failure[?]]
  }

  test("get propagates a redis read failure as Failure") {
    val uploadId = UUID.randomUUID()
    val ex       = new RuntimeException("redis down")
    when(jedis.hget(eqTo(BulkUploadType), eqTo(uploadId.toString), eqTo(BulkUploadType.stateField))).thenReturn(
      Failure(ex)
    )

    store.get(uploadId) should be(Failure(ex))
  }

  test("set writes the encoded state and refreshes the TTL using BulkUploadType.cacheTime") {
    val uploadId    = UUID.randomUUID()
    val expectedTtl = BulkUploadType.cacheTime.toSeconds
    when(jedis.getNewTTL(eqTo(BulkUploadType), eqTo(uploadId.toString))).thenReturn(Success(expectedTtl))
    when(jedis.hset(eqTo(BulkUploadType), eqTo(uploadId.toString), eqTo(BulkUploadType.stateField), any[String]))
      .thenReturn(Success(1L))
    when(jedis.expire(eqTo(BulkUploadType), eqTo(uploadId.toString), eqTo(expectedTtl))).thenReturn(Success(1L))

    store.set(uploadId, sampleState) should be(Success(()))

    verify(jedis).hset(
      eqTo(BulkUploadType),
      eqTo(uploadId.toString),
      eqTo(BulkUploadType.stateField),
      eqTo(CirceUtil.toJsonString(sampleState)),
    )
    verify(jedis).expire(eqTo(BulkUploadType), eqTo(uploadId.toString), eqTo(expectedTtl))
  }

  test("set followed by get round-trips a state through the cache without losing fields") {
    val uploadId     = UUID.randomUUID()
    val capturedJson = new AtomicReference[String]()
    when(jedis.getNewTTL(eqTo(BulkUploadType), eqTo(uploadId.toString))).thenReturn(Success(1L))
    when(jedis.hset(eqTo(BulkUploadType), eqTo(uploadId.toString), eqTo(BulkUploadType.stateField), any[String]))
      .thenAnswer { i =>
        capturedJson.set(i.getArgument[String](3))
        Success(1L)
      }
    when(jedis.expire(any[RedisStoredType], any[String], any[Long])).thenReturn(Success(1L))
    when(jedis.hget(eqTo(BulkUploadType), eqTo(uploadId.toString), eqTo(BulkUploadType.stateField))).thenAnswer(_ =>
      Success(Option(capturedJson.get()))
    )

    store.set(uploadId, sampleState) should be(Success(()))
    store.get(uploadId) should be(Success(Some(sampleState)))
  }
}
