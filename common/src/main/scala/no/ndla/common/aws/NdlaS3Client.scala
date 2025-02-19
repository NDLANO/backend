/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.aws

import no.ndla.common.model.domain.UploadedFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.{S3Client, S3ClientBuilder}

import scala.util.Try

trait NdlaS3Client {
  val s3Client: NdlaS3Client

  class NdlaS3Client(bucket: String, region: Option[String]) {

    private val builder: S3ClientBuilder = S3Client.builder()

    val client: S3Client = region match {
      case Some(value) => builder.region(Region.of(value)).build()
      case None        => builder.build()
    }

    val foundRegion: Region = client.serviceClientConfiguration().region()

    def headObject(key: String): Try[HeadObjectResponse] = Try {
      val headObjectRequest = HeadObjectRequest.builder().bucket(bucket).key(key).build()
      client.headObject(headObjectRequest)
    }

    def objectExists(key: String): Boolean = headObject(key).isSuccess

    def getObject(key: String): Try[NdlaS3Object] = Try {
      val gor = GetObjectRequest
        .builder()
        .bucket(bucket)
        .key(key)
        .build()
      val response = client.getObject(gor)
      NdlaS3Object(
        bucket = bucket,
        key = key,
        stream = response,
        contentType = response.response().contentType(),
        contentLength = response.response().contentLength()
      )
    }

    def deleteObject(key: String): Try[DeleteObjectResponse] = Try {
      val dor = DeleteObjectRequest.builder().key(key).bucket(bucket).build()
      client.deleteObject(dor)
    }

    def putObject(key: String, uploadedFile: UploadedFile): Try[PutObjectResponse] = putObject(key, uploadedFile, None)

    def putObject(key: String, uploadedFile: UploadedFile, cacheControl: Option[String]): Try[PutObjectResponse] = Try {
      val contentType = uploadedFile.contentType.getOrElse("application/octet-stream")
      val por = PutObjectRequest
        .builder()
        .contentLength(uploadedFile.fileSize)
        .contentType(contentType)
        .key(key)
        .bucket(bucket)

      val porWithCacheControl = cacheControl match {
        case Some(value) => por.cacheControl(value)
        case None        => por
      }

      val requestBody = RequestBody.fromFile(uploadedFile.file)

      client.putObject(
        porWithCacheControl.build(),
        requestBody
      )
    }

    def updateMetadata(key: String, metadata: java.util.Map[String, String]): Try[_] = Try {
      val cor =
        CopyObjectRequest
          .builder()
          .sourceBucket(bucket)
          .destinationBucket(bucket)
          .sourceKey(key)
          .destinationKey(key)
          .metadata(metadata)

      client.copyObject(cor.build())
    }

    def copyObject(fromKey: String, toKey: String): Try[CopyObjectResponse] = Try {
      val cor = CopyObjectRequest
        .builder()
        .sourceBucket(bucket)
        .destinationBucket(bucket)
        .sourceKey(fromKey)
        .destinationKey(toKey)
        .build()

      client.copyObject(cor)
    }
  }
}
